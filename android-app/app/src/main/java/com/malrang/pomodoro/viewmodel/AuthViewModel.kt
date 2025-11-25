package com.malrang.pomodoro.viewmodel

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malrang.pomodoro.localRepo.PomodoroRepository
import com.malrang.pomodoro.networkRepo.SupabaseRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuthViewModel(
    private val supabase: SupabaseClient,
    private val repository: PomodoroRepository,
    private val supabaseRepository: SupabaseRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    // [추가] 동기화 진행 상태 (UI 로딩 표시용)
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // 자동 동기화 설정 상태 (UI 바인딩용)
    val isAutoSyncEnabled: StateFlow<Boolean> = repository.autoSyncEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    init {
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

    // [기능] 앱 시작 시 자동 동기화 체크
    fun checkAndSyncOnStart() {
        viewModelScope.launch {
            if (repository.isAutoSyncEnabled()) {
                val currentUser = (authState.value as? AuthState.Authenticated)?.user
                if (currentUser != null) {
                    Log.d("AuthViewModel", "앱 시작: 자동 동기화 수행")
                    // 앱 시작 시에는 사용자가 인지하지 못하게 조용히(isSyncing 없이) 수행하거나,
                    // 필요하다면 여기서도 _isSyncing = true 처리를 할 수 있습니다.
                    syncLocalDataToSupabase(currentUser.id)
                }
            }
        }
    }

    // [기능] 자동 동기화 스위치 토글
    fun toggleAutoSync(isEnabled: Boolean) {
        viewModelScope.launch {
            repository.saveAutoSyncEnabled(isEnabled)
            // OFF -> ON 전환 시 즉시 동기화
            if (isEnabled) {
                val currentUser = (authState.value as? AuthState.Authenticated)?.user
                if (currentUser != null) {
                    _isSyncing.value = true
                    try {
                        syncLocalDataToSupabase(currentUser.id)
                    } finally {
                        _isSyncing.value = false
                    }
                }
            }
        }
    }

    // [기능] 수동 동기화 요청 (UI 버튼)
    fun requestManualSync() {
        viewModelScope.launch {
            val currentUser = (authState.value as? AuthState.Authenticated)?.user
            if (currentUser != null) {
                _isSyncing.value = true // 로딩 시작
                try {
                    syncLocalDataToSupabase(currentUser.id)
                } finally {
                    _isSyncing.value = false // 로딩 종료 (성공/실패 무관)
                }
            } else {
                _authState.value = AuthState.Error("로그인이 필요합니다.")
            }
        }
    }

    // [핵심 로직] 서버 <-> 로컬 동기화 (Merge & Upload)
    private suspend fun syncLocalDataToSupabase(userId: String) {
        try {
            // 1. 서버 데이터 다운로드
            val remoteStats = supabaseRepository.getDailyStats(userId)
            val remotePresets = supabaseRepository.getWorkPresets(userId)

            // 2. 로컬 DB 병합 (서버 데이터를 로컬에 덮어쓰거나 추가)
            if (remoteStats.isNotEmpty()) {
                val currentStats = repository.loadDailyStats().toMutableMap()
                remoteStats.forEach { stat ->
                    // 간단한 전략: 날짜가 같으면 서버 데이터로 덮어쓰기 (또는 더 정교한 병합 로직 가능)
                    currentStats[stat.date] = stat
                }
                repository.saveDailyStats(currentStats)
            }

            if (remotePresets.isNotEmpty()) {
                // 프리셋 병합 및 동기화
                // 리포지토리의 upsertWorkPresets(또는 insertNewWorkPresets)가
                // ID 충돌 시 업데이트/무시 로직을 처리한다고 가정합니다.
                repository.upsertWorkPresets(remotePresets)
            }

            // 3. 병합된 최신 로컬 데이터를 다시 서버로 업로드 (Upsert)
            val finalStats = repository.loadDailyStats()
            val finalPresets = repository.loadWorkPresets()

            finalStats.values.forEach { stat ->
                supabaseRepository.upsertDailyStat(userId, stat)
            }

            if (finalPresets.isNotEmpty()) {
                supabaseRepository.upsertWorkPresets(userId, finalPresets)
            }

            Log.d("AuthViewModel", "동기화 성공 완료")

        } catch (e: Exception) {
            Log.e("AuthViewModel", "동기화 실패: ${e.message}")
            // 에러 발생 시 필요하다면 _authState.value = AuthState.Error(...) 처리 가능
        }
    }

    // --- 기존 로그인/로그아웃 로직 ---

    fun signInWithGoogle() {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                supabase.auth.signInWith(Google) {
                    scopes.add("email")
                    scopes.add("profile")
                }
                // 결과는 sessionStatus Flow에서 처리됨
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "알 수 없는 오류가 발생했습니다.")
            }
        }
    }

    fun signOut(activityContext: Context) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                supabase.auth.signOut()
                val credentialManager = CredentialManager.create(activityContext)
                try {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Exception) {
                    Log.w("AuthViewModel", "clearCredentialState failed: ${e.message}")
                }
                _authState.value = AuthState.NotAuthenticated
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "로그아웃 중 오류가 발생했습니다.")
            }
        }
    }

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

                activityContext?.let {
                    try {
                        val credentialManager = CredentialManager.create(it)
                        credentialManager.clearCredentialState(ClearCredentialStateRequest())
                    } catch (e: Exception) {
                        Log.w("AuthViewModel", "clearCredentialState 실패: ${e.message}")
                    }
                }

                val jwt = session?.accessToken ?: session?.let {
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
                    if (code !in 200..299) {
                        throw RuntimeException("탈퇴 요청 실패: HTTP $code")
                    }
                }

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
        object WaitingForRedirect : AuthState
        data class Authenticated(val user: UserInfo?) : AuthState
        object NotAuthenticated : AuthState
        data class Error(val message: String) : AuthState
    }
}