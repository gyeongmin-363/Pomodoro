package com.malrang.pomodoro.viewmodel

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malrang.pomodoro.dataclass.ui.DailyStat
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
import kotlin.math.max

class AuthViewModel(
    private val supabase: SupabaseClient,
    private val repository: PomodoroRepository,
    private val supabaseRepository: SupabaseRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    // 동기화 진행 상태 (UI 로딩 표시용)
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // 자동 동기화 설정 상태
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
                    syncLocalDataToSupabase(currentUser.id)
                }
            }
        }
    }

    // [기능] 자동 동기화 스위치 토글
    fun toggleAutoSync(isEnabled: Boolean) {
        viewModelScope.launch {
            repository.saveAutoSyncEnabled(isEnabled)
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
                _isSyncing.value = true
                try {
                    syncLocalDataToSupabase(currentUser.id)
                } finally {
                    _isSyncing.value = false
                }
            } else {
                _authState.value = AuthState.Error("로그인이 필요합니다.")
            }
        }
    }

    // [핵심 로직] 서버 <-> 로컬 동기화 (Smart Merge)
    private suspend fun syncLocalDataToSupabase(userId: String) {
        try {
            // 1. 서버 데이터 다운로드
            val remoteStats = supabaseRepository.getDailyStats(userId)
            val remotePresets = supabaseRepository.getWorkPresets(userId)

            // 2. 로컬 DB 병합 (충돌 해결 로직 적용)
            if (remoteStats.isNotEmpty()) {
                val currentStats = repository.loadDailyStats().toMutableMap()

                remoteStats.forEach { remoteStat ->
                    val localStat = currentStats[remoteStat.date]
                    if (localStat != null) {
                        // 로컬 데이터가 있으면 병합 (Smart Merge)
                        currentStats[remoteStat.date] = mergeDailyStats(localStat, remoteStat)
                    } else {
                        // 로컬 데이터가 없으면 서버 데이터 사용
                        currentStats[remoteStat.date] = remoteStat
                    }
                }
                repository.saveDailyStats(currentStats)
            }

            // 프리셋 병합 (ID 충돌 해결은 리포지토리 내부 로직 위임)
            if (remotePresets.isNotEmpty()) {
                repository.upsertWorkPresets(remotePresets)
            }

            // 3. 병합된 최신 로컬 데이터를 서버로 업로드 (Upsert)
            // 이때 updatedAt을 현재 시간으로 갱신하여 업로드
            val finalStats = repository.loadDailyStats()
            val finalPresets = repository.loadWorkPresets()
            val now = System.currentTimeMillis()

            finalStats.values.forEach { stat ->
                // 동기화 시점의 시간으로 갱신 후 업로드
                supabaseRepository.upsertDailyStat(userId, stat.copy(updatedAt = now))
            }

            if (finalPresets.isNotEmpty()) {
                supabaseRepository.upsertWorkPresets(userId, finalPresets)
            }

            Log.d("AuthViewModel", "동기화 성공 완료")

        } catch (e: Exception) {
            Log.e("AuthViewModel", "동기화 실패: ${e.message}")
            // 에러 상황을 UI에 알리려면 _authState.value = Error(...) 처리 가능
        }
    }

    /**
     * [스마트 병합 로직]
     * 1. 시간 데이터(Study/Break): Max 전략 사용 (오프라인 누적분 보호, 중복 합산 방지)
     * 2. 내용 데이터(Checklist/Retrospect): updatedAt 비교 (최신 수정본 우선)
     */
    private fun mergeDailyStats(local: DailyStat, remote: DailyStat): DailyStat {
        // [1] 시간 데이터: Max 전략
        val localStudyKeys = local.studyTimeByWork?.keys.orEmpty()
        val remoteStudyKeys = remote.studyTimeByWork?.keys.orEmpty()
        val allStudyKeys = localStudyKeys + remoteStudyKeys

        val mergedStudyTimeMap = allStudyKeys.associateWith { key ->
            val localTime = local.studyTimeByWork?.get(key) ?: 0
            val remoteTime = remote.studyTimeByWork?.get(key) ?: 0
            max(localTime, remoteTime)
        }

        val localBreakKeys = local.breakTimeByWork?.keys.orEmpty()
        val remoteBreakKeys = remote.breakTimeByWork?.keys.orEmpty()
        val allBreakKeys = localBreakKeys + remoteBreakKeys

        val mergedBreakTimeMap = allBreakKeys.associateWith { key ->
            val localTime = local.breakTimeByWork?.get(key) ?: 0
            val remoteTime = remote.breakTimeByWork?.get(key) ?: 0
            max(localTime, remoteTime)
        }

        // [수정] totalStudyTimeInMinutes는 copy 대상이 아님 (studyTimeByWork가 바뀌면 자동 계산됨)

        // [2] 내용 데이터: Last Write Wins (최신 업데이트 우선)
        val isLocalNewer = local.updatedAt >= remote.updatedAt

        val mergedChecklist = if (isLocalNewer) local.checklist else remote.checklist
        val mergedRetrospect = if (isLocalNewer) local.retrospect else remote.retrospect

        // 병합된 결과의 시간은 둘 중 더 최신 시간으로 설정
        val newUpdatedAt = max(local.updatedAt, remote.updatedAt)

        return local.copy(
            // totalStudyTimeInMinutes 제거됨 (자동 계산)
            studyTimeByWork = mergedStudyTimeMap,
            breakTimeByWork = mergedBreakTimeMap,
            checklist = mergedChecklist,
            retrospect = mergedRetrospect,
            updatedAt = newUpdatedAt
        )
    }

    // --- 기존 로그인/로그아웃/탈퇴 로직 ---

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