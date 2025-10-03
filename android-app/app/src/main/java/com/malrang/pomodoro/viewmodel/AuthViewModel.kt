package com.malrang.pomodoro.viewmodel

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuthViewModel(
    private val supabase: SupabaseClient
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    init {
        // ✅ [핵심 수정] ViewModel 생성 시 Supabase의 인증 상태 흐름을 구독합니다.
        // 이 코드가 로그인, 로그아웃, 토큰 갱신 등 모든 상태 변화를 자동으로 감지하고
        // uiState를 올바르게 업데이트합니다.
        supabase.auth.sessionStatus
            .onEach { status ->
                _authState.value = when (status) {
                    is SessionStatus.Authenticated -> AuthState.Authenticated(status.session.user)
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
                _authState.value = AuthState.Loading
                supabase.auth.signInWith(Google) {
                    scopes.add("email")
                    scopes.add("profile")
                }
                // ✅ [핵심 수정] 로그인 시도 후, 인증 상태가 여전히 NotAuthenticated라면
                // 사용자가 로그인을 취소한 것으로 간주하고 상태를 되돌립니다.
                if (supabase.auth.sessionStatus.value is SessionStatus.NotAuthenticated) {
                    _authState.value = AuthState.NotAuthenticated
                }
            } catch (e: Exception) {
                // 실제 에러가 발생하면 Error 상태로 변경합니다.
                _authState.value = AuthState.Error(e.message ?: "알 수 없는 오류가 발생했습니다.")
            }
        }
    }

    /**
     * 로그아웃 처리
     * - supabase 세션 signOut
     * - Credential providers에 로그아웃 사실을 알리기 위해 clearCredentialState 호출(안그러면 구글은 계속 로그인 상태라 생각 가능)
     * @param activityContext Activity context (CredentialManager 사용)
     */
    fun signOut(activityContext: Context) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                // 1) Supabase 로그아웃
                supabase.auth.signOut()

                // 2) CredentialManager에 상태 초기화 요청 (동기/비동기 구현체 중 선택 가능)
                val credentialManager = CredentialManager.create(activityContext)
                try {
                    // Clear the credential state so providers (Google etc.) know user logged out
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Exception) {
                    // 일부 디바이스/구현에서 예외가 발생할 수 있으므로 로그만 남기고 계속 진행
                    Log.w("AuthViewModel", "clearCredentialState failed: ${e.message}")
                }

                _authState.value = AuthState.NotAuthenticated
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "로그아웃 중 오류가 발생했습니다.")
            }
        }
    }

    /**
     * 계정 삭제(회원 탈퇴) - 기존 Edge Function 호출 방식 사용
     * - Edge Function URL을 전달 (서비스 역할 키는 서버에서만 사용)
     * - activityContext는 clearCredentialState 호출을 위해 선택적으로 전달
     */
    fun deleteUser(edgeFunctionUrl: String, activityContext: Context? = null) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading

            try {
                val session = supabase.auth.currentSessionOrNull()
                val user = session?.user
                if (user == null) {
                    _authState.value = AuthState.Error("로그인된 사용자가 없습니다.")
                    return@launch
                }
                val userId = user.id

                // clear credential state (선택)
                activityContext?.let {
                    try {
                        val credentialManager = CredentialManager.create(it)
                        credentialManager.clearCredentialState(ClearCredentialStateRequest())
                    } catch (e: Exception) {
                        Log.w("AuthViewModel", "clearCredentialState 실패: ${e.message}")
                    }
                }

                // Edge Function 호출 (Bearer token으로 본인 인증)
                val jwt = session?.accessToken ?: session?.let {
                    // fallback: try to get token via reflection if 라이브러리 버전 차이시
                    try {
                        val field = it::class.java.getDeclaredField("accessToken")
                        field.isAccessible = true
                        field.get(it) as? String
                    } catch (_: Exception) { null }
                }

                if (jwt.isNullOrBlank()) {
                    _authState.value = AuthState.Error("현재 세션 토큰을 가져오지 못했습니다.")
                    return@launch
                }

                // 네트워크 호출은 IO 스레드에서
                withContext(Dispatchers.IO) {
                    val url = java.net.URL(edgeFunctionUrl)
                    val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                        requestMethod = "POST"
                        doOutput = true
                        setRequestProperty("Content-Type", "application/json")
                        setRequestProperty("Authorization", "Bearer $jwt")
                        connectTimeout = 10_000
                        readTimeout = 30_000
                    }
                    val payload = """{ "userId": "$userId" }"""
                    conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                    val code = conn.responseCode
                    val responseBody = try {
                        conn.inputStream.bufferedReader().readText()
                    } catch (e: Exception) {
                        conn.errorStream?.bufferedReader()?.readText() ?: ""
                    }
                    if (code !in 200..299) {
                        throw RuntimeException("탈퇴 요청 실패: HTTP $code / $responseBody")
                    }
                }

                // 성공 시 로컬 세션 삭제
                supabase.auth.signOut()
                _authState.value = AuthState.NotAuthenticated

            } catch (e: Exception) {
                _authState.value = AuthState.Error("회원 탈퇴 중 오류: ${e.message ?: e.toString()}")
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