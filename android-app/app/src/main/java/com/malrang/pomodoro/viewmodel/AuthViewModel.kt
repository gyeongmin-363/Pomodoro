package com.malrang.pomodoro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malrang.pomodoro.viewmodel.AuthViewModel.AuthState.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class AuthViewModel(
    private val supabase: SupabaseClient
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthState>(AuthState.Idle)
    val uiState: StateFlow<AuthState> = _uiState

    init {
        // ✅ [핵심 수정] ViewModel 생성 시 Supabase의 인증 상태 흐름을 구독합니다.
        // 이 코드가 로그인, 로그아웃, 토큰 갱신 등 모든 상태 변화를 자동으로 감지하고
        // uiState를 올바르게 업데이트합니다.
        supabase.auth.sessionStatus
            .onEach { status ->
                _uiState.value = when (status) {
                    is SessionStatus.Authenticated -> Authenticated(status.session.user)
                    is SessionStatus.NotAuthenticated -> AuthState.NotAuthenticated
                    is SessionStatus.Initializing -> AuthState.Loading
                    is SessionStatus.RefreshFailure -> AuthState.Error("세션 갱신에 실패했습니다. 다시 로그인해주세요.")
                }
            }
            .launchIn(viewModelScope)
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
                // ✅ [핵심 수정] 로그인 시도 후, 인증 상태가 여전히 NotAuthenticated라면
                // 사용자가 로그인을 취소한 것으로 간주하고 상태를 되돌립니다.
                if (supabase.auth.sessionStatus.value is SessionStatus.NotAuthenticated) {
                    _uiState.value = NotAuthenticated
                }
            } catch (e: Exception) {
                // 실제 에러가 발생하면 Error 상태로 변경합니다.
                _uiState.value = Error(e.message ?: "알 수 없는 오류가 발생했습니다.")
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