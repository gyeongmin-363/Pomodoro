package com.malrang.pomodoro.viewmodel

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malrang.pomodoro.dataclass.ui.Settings
import com.malrang.pomodoro.localRepo.PomodoroRepository
import com.malrang.pomodoro.networkRepo.BackupData
import com.malrang.pomodoro.networkRepo.SupabaseRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AuthViewModel(
    private val supabase: SupabaseClient,
    private val repository: PomodoroRepository,
    private val supabaseRepository: SupabaseRepository
) : ViewModel() {

    // --- 인증 상태 ---
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    // --- 백업/복원 상태 (UI 표시용) ---
    private val _backupState = MutableStateFlow<BackupState>(BackupState.Idle)
    val backupState: StateFlow<BackupState> = _backupState.asStateFlow()

    // JSON 설정 (유연한 파싱을 위해)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    init {
        // Supabase 인증 세션 상태 모니터링
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

    // =========================================================================
    // [백업] 데이터 업로드 (Backup)
    // =========================================================================
    fun backupData() {
        viewModelScope.launch {
            val user = (authState.value as? AuthState.Authenticated)?.user
            if (user == null) {
                _backupState.value = BackupState.Error("로그인이 필요합니다.")
                return@launch
            }

            try {
                _backupState.value = BackupState.Loading

                // 1. 로컬 데이터 모두 가져오기
                // (Repository가 DAO의 getAllDailyStats, getAllWorkPresets 등을 호출한다고 가정)
                // 만약 Repository에 해당 메서드가 없다면, loadDailyStats().values.toList() 등으로 대체 가능
                val stats = repository.getAllDailyStats()
                val presets = repository.getAllWorkPresets()

                // 현재 활성화된 설정(Settings) 가져오기 (없으면 기본값)
                val currentWorkId = repository.loadCurrentWorkId()
                val currentSettings = presets.find { it.id == currentWorkId }?.settings ?: Settings()

                // 2. BackupData 객체 생성 (보따리 싸기)
                val backupData = BackupData(
                    settings = currentSettings,
                    workPresets = presets,
                    dailyStats = stats
                )

                // 3. JSON 변환 및 업로드
                val jsonString = json.encodeToString(backupData)
                supabaseRepository.uploadBackup(user.id, jsonString)

                _backupState.value = BackupState.Success("데이터 백업이 완료되었습니다.")
                Log.d("AuthViewModel", "백업 성공: ${user.id}")

            } catch (e: Exception) {
                e.printStackTrace()
                _backupState.value = BackupState.Error("백업 실패: ${e.message}")
            }
        }
    }

    // =========================================================================
    // [복원] 데이터 다운로드 및 덮어쓰기 (Restore)
    // =========================================================================
    fun restoreData() {
        viewModelScope.launch {
            val user = (authState.value as? AuthState.Authenticated)?.user
            if (user == null) {
                _backupState.value = BackupState.Error("로그인이 필요합니다.")
                return@launch
            }

            try {
                _backupState.value = BackupState.Loading

                // 1. 서버에서 백업 파일 다운로드
                val jsonString = supabaseRepository.downloadBackup(user.id)

                // 2. JSON 파싱
                val backupData = json.decodeFromString<BackupData>(jsonString)

                // 3. 로컬 DB 복원 (유효성 검사 포함)
                repository.restoreAllData(backupData.dailyStats, backupData.workPresets)

                _backupState.value = BackupState.Success("데이터 복원이 완료되었습니다.")
                Log.d("AuthViewModel", "복원 성공: ${user.id}")

            } catch (e: IllegalArgumentException) {
                // [수정] 유효성 검사 실패 시 에러 메시지 표시
                e.printStackTrace()
                _backupState.value = BackupState.Error(e.message ?: "데이터 복원 중 오류가 발생했습니다.")
            } catch (e: Exception) {
                // 기타 네트워크 오류 등
                e.printStackTrace()
                _backupState.value = BackupState.Error("복원 실패: 저장된 백업이 없거나 오류가 발생했습니다.")
            }
        }
    }

    // 상태 초기화 (다이얼로그 닫을 때 등)
    fun clearBackupState() {
        _backupState.value = BackupState.Idle
    }


    // =========================================================================
    // 인증 관련 기능 (로그인, 로그아웃, 탈퇴) - 기존 유지
    // =========================================================================

    fun signInWithGoogle() {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                supabase.auth.signInWith(Google) {
                    scopes.add("email")
                    scopes.add("profile")
                }
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

                // 1. Edge Function 호출을 위한 JWT 준비
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

                // 2. Edge Function으로 계정 및 백업 데이터 삭제 요청
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

    // =========================================================================
    // UI States
    // =========================================================================

    sealed interface AuthState {
        object Idle : AuthState
        object Loading : AuthState
        object WaitingForRedirect : AuthState
        data class Authenticated(val user: UserInfo?) : AuthState
        object NotAuthenticated : AuthState
        data class Error(val message: String) : AuthState
    }

    sealed interface BackupState {
        object Idle : BackupState
        object Loading : BackupState
        data class Success(val message: String) : BackupState
        data class Error(val message: String) : BackupState
    }
}