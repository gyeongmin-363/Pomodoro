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
    val isLoading: Boolean = true, // ✅ 기본값을 true로 변경하여 초기 로딩 상태를 표시합니다.
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
            // init에서 이미 isLoading이 true이므로 여기서 업데이트할 필요가 없습니다.
            // _userState.update { it.copy(isLoading = true) }
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

    /**
     * 닉네임을 제출받아 중복 검사 후 프로필을 생성합니다.
     * @param nickname 제출할 닉네임
     */
    fun submitNickname(nickname: String) {
        viewModelScope.launch {
            _userState.update { it.copy(isLoading = true, isNicknameAvailable = null) }
            val userId = SupabaseProvider.client.auth.currentUserOrNull()?.id

            if (userId == null) {
                _userState.update { it.copy(error = "사용자 인증 정보 없음", isLoading = false) }
                return@launch
            }

            try {
                // 1. 닉네임 중복 검사
                val isAvailable = supabaseRepository.isNicknameAvailable(nickname)

                if (isAvailable) {
                    // 2. 사용 가능하면 프로필 생성
                    supabaseRepository.createUserProfile(userId, nickname)
                    _userState.update {
                        it.copy(
                            user = User(id = userId, nickname = nickname, coins = 0),
                            isNicknameSet = true,
                            isLoading = false
                        )
                    }
                } else {
                    // 3. 중복된 닉네임이면 에러 상태 업데이트
                    _userState.update { it.copy(isNicknameAvailable = false, isLoading = false) }
                }
            } catch (e: Exception) {
                _userState.update { it.copy(error = "처리 중 오류 발생: ${e.message}", isLoading = false) }
            }
        }
    }
}