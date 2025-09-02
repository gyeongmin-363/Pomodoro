package com.malrang.pomodoro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val supabase: SupabaseClient
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthState>(AuthState.Idle)
    val uiState: StateFlow<AuthState> = _uiState

    init {
        // ViewModel이 처음 생성될 때 현재 세션이 있는지 확인합니다.
        checkSession()
    }

    fun signInWithGoogle() {
        viewModelScope.launch {
            try {
                // 로그인 시도 시 상태를 '로딩 중'으로 변경합니다.
                _uiState.value = AuthState.Loading
                supabase.auth.signInWith(Google) {
                    scopes.add("email")
                    scopes.add("profile")
                }
                // 성공 여부는 MainActivity의 신호를 통해 확인되므로 여기서는 추가 상태 변경이 없습니다.
            } catch (e: Exception) {
                _uiState.value = AuthState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * 현재 Supabase에 저장된 세션을 확인하고 UI 상태를 업데이트합니다.
     * MainActivity에서 딥링크를 받은 후 직접 호출될 것입니다.
     */
    fun checkSession() {
        viewModelScope.launch {
            val session = supabase.auth.sessionManager.loadSession()
            if (session != null) {
                _uiState.value = AuthState.Authenticated(session.user)
            } else {
                _uiState.value = AuthState.NotAuthenticated
            }
        }
    }

    sealed interface AuthState {
        object Idle : AuthState
        object Loading : AuthState
        object WaitingForRedirect : AuthState // 이 상태는 더 이상 직접 사용되지 않을 수 있습니다.
        data class Authenticated(val user: UserInfo?) : AuthState
        object NotAuthenticated : AuthState
        data class Error(val message: String) : AuthState
    }
}