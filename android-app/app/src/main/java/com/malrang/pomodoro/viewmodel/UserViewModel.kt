package com.malrang.pomodoro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malrang.pomodoro.networkRepo.SupabaseProvider
import com.malrang.pomodoro.networkRepo.SupabaseRepository
import com.malrang.pomodoro.networkRepo.User
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// 사용자 상태를 나타내는 데이터 클래스
data class UserState(
    val user: User? = null,
    val isNicknameSet: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isNicknameAvailable: Boolean? = null // 중복 확인 결과 (null: 확인 전, true: 사용 가능, false: 중복)
)

class UserViewModel(
    private val supabase: SupabaseClient
) : ViewModel() {
    private val supabaseRepository: SupabaseRepository =
        SupabaseRepository(supabase.postgrest, supabase.storage)

    private val _userState = MutableStateFlow(UserState())
    val userState = _userState.asStateFlow()

    init {
        fetchUserProfile()
    }

    fun fetchUserProfile() {
        viewModelScope.launch {
            _userState.update { it.copy(isLoading = true) }
            val userId = SupabaseProvider.client.auth.currentUserOrNull()?.id
            if (userId != null) {
                try {
                    val profile = supabaseRepository.getUserProfile(userId)
                    _userState.update {
                        it.copy(
                            user = profile,
                            isNicknameSet = profile?.nickname?.isNotBlank() == true,
                            isLoading = false
                        )
                    }
                } catch (e: Exception) {
                    _userState.update { it.copy(error = e.message, isLoading = false) }
                }
            } else {
                _userState.update { it.copy(error = "사용자 인증 정보 없음", isLoading = false) }
            }
        }
    }

    fun updateNickname(nickname: String) {
        viewModelScope.launch {
            _userState.update { it.copy(isLoading = true) }
            val userId = SupabaseProvider.client.auth.currentUserOrNull()?.id
            if (userId != null) {
                try {
                    supabaseRepository.updateNickname(userId, nickname)
                    _userState.update {
                        it.copy(
                            user = it.user?.copy(nickname = nickname),
                            isNicknameSet = true,
                            isLoading = false
                        )
                    }
                } catch (e: Exception) {
                    _userState.update { it.copy(error = e.message, isLoading = false) }
                }
            } else {
                _userState.update { it.copy(error = "사용자 인증 정보 없음", isLoading = false) }
            }
        }
    }

    fun checkNicknameAvailability(nickname: String) {
        // 닉네임이 비어있으면 확인 상태를 초기화
        if (nickname.isBlank()) {
            _userState.update { it.copy(isNicknameAvailable = null) }
            return
        }
        // 확인 시작
        _userState.update { it.copy(isNicknameAvailable = null, isLoading = true) }

        viewModelScope.launch {
            try {
                val isAvailable = supabaseRepository.isNicknameAvailable(nickname)
                _userState.update { it.copy(isNicknameAvailable = isAvailable, isLoading = false) }
            } catch (e: Exception) {
                // 에러 처리. 예를 들어, 네트워크 문제
                _userState.update { it.copy(isNicknameAvailable = false, error = "닉네임 확인 중 오류 발생", isLoading = false) }
            }
        }
    }
}