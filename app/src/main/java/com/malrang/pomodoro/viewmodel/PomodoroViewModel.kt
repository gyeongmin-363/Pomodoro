package com.malrang.pomodoro.viewmodel

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malrang.pomodoro.dataclass.animalInfo.AnimalsTable
import com.malrang.pomodoro.dataclass.sprite.SpriteState
import com.malrang.pomodoro.dataclass.ui.Mode
import com.malrang.pomodoro.dataclass.ui.PermissionInfo
import com.malrang.pomodoro.dataclass.ui.PermissionType
import com.malrang.pomodoro.dataclass.ui.PomodoroUiState
import com.malrang.pomodoro.dataclass.ui.Screen
import com.malrang.pomodoro.dataclass.ui.Settings
import com.malrang.pomodoro.dataclass.ui.WorkPreset
import com.malrang.pomodoro.localRepo.PomodoroRepository
import com.malrang.pomodoro.service.TimerService
import com.malrang.pomodoro.service.TimerServiceProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

class PomodoroViewModel(
    private val repo: PomodoroRepository,
    private val timerService: TimerServiceProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(PomodoroUiState())
    val uiState: StateFlow<PomodoroUiState> = _uiState.asStateFlow()

    private val _sessionAttemptedPermissions = MutableStateFlow<Set<PermissionType>>(emptySet())
    val sessionAttemptedPermissions: StateFlow<Set<PermissionType>> = _sessionAttemptedPermissions.asStateFlow()

    /** 알림 권한 '거부 횟수'를 관리하는 StateFlow */
    private val _notificationDenialCount = MutableStateFlow(0)
    val notificationDenialCount: StateFlow<Int> = _notificationDenialCount.asStateFlow()

    private val _editingWorkPreset = MutableStateFlow<WorkPreset?>(null)
    val editingWorkPreset: StateFlow<WorkPreset?> = _editingWorkPreset.asStateFlow()

    private val _draftSettings = MutableStateFlow<Settings?>(null)
    val draftSettings: StateFlow<Settings?> = _draftSettings.asStateFlow()

    init {
        viewModelScope.launch {
            // ViewModel 생성 시 저장소에서 거부 횟수를 불러옵니다.
            _notificationDenialCount.value = repo.loadNotificationDenialCount()

            val seenIds = repo.loadSeenIds()
            val daily = repo.loadDailyStats()
            val presets = repo.loadWorkPresets()
            val whitelistedApps = repo.loadWhitelistedApps()
            val currentWorkId = repo.loadCurrentWorkId() ?: presets.firstOrNull()?.id
            val sprites = repo.loadActiveSprites()
            val useGrassBackground = repo.loadUseGrassBackground()

            val seenAnimals = seenIds.mapNotNull { id -> AnimalsTable.byId(id) }
            val currentWork = presets.find { it.id == currentWorkId }
            val currentSettings = currentWork?.settings ?: Settings()

            _uiState.update {
                it.copy(
                    collectedAnimals = seenAnimals.toSet(),
                    dailyStats = daily,
                    settings = currentSettings,
                    workPresets = presets,
                    currentWorkId = currentWorkId,
                    activeSprites = sprites,
                    useGrassBackground = useGrassBackground,
                    whitelistedApps = whitelistedApps
                )
            }

            if (TimerService.isServiceActive()) {
                timerService.requestStatus()
            } else {
                reset()
            }
        }
    }

    /**
     * 권한 요청 후 화면으로 돌아왔을 때 호출되는 함수
     * @param context 애플리케이션 컨텍스트
     */
    fun onPermissionRequestResult(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            checkAndupdatePermissions(context)
            return
        }

        val notificationPermissionInfo = uiState.value.permissions.find { it.type == PermissionType.NOTIFICATION }
        // 알림 권한 정보가 없으면 일반 업데이트만 수행
        if (notificationPermissionInfo == null) {
            checkAndupdatePermissions(context)
            return
        }

        val wasAttempted = sessionAttemptedPermissions.value.contains(PermissionType.NOTIFICATION)
        val isGrantedNow = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        // 조건: 1. 이번 세션에서 시도했고, 2. 시도 전에도 허용되지 않았고, 3. 지금도 허용되지 않았다면 -> '새로운 거부'로 간주
        if (wasAttempted && !notificationPermissionInfo.isGranted && !isGrantedNow) {
            val newCount = _notificationDenialCount.value + 1
            viewModelScope.launch {
                repo.saveNotificationDenialCount(newCount)
                _notificationDenialCount.value = newCount
            }
        }

        // 최종적으로 권한 상태를 다시 확인하여 UI 전체를 업데이트
        checkAndupdatePermissions(context)
    }


    fun setPermissionAttemptedInSession(permissionType: PermissionType) {
        _sessionAttemptedPermissions.update { it + permissionType }
    }

    fun checkAndupdatePermissions(context: Context): Boolean {
        val permissionList = mutableListOf<PermissionInfo>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionList.add(
                PermissionInfo(
                    type = PermissionType.NOTIFICATION,
                    title = "알림",
                    description = "타이머 진행 상황을 알림으로 표시하기 위해 필요합니다.",
                    isGranted = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                )
            )
        }

        permissionList.add(
            PermissionInfo(
                type = PermissionType.OVERLAY,
                title = "다른 앱 위에 표시",
                description = "공부 중 다른 앱 사용 시 경고창을 띄우기 위해 필요합니다.",
                isGranted = android.provider.Settings.canDrawOverlays(context)
            )
        )

        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        permissionList.add(
            PermissionInfo(
                type = PermissionType.USAGE_STATS,
                title = "사용 정보 접근",
                description = "공부에 방해되는 앱 사용을 감지하기 위해 필요합니다.",
                isGranted = mode == AppOpsManager.MODE_ALLOWED
            )
        )

        _uiState.update { it.copy(permissions = permissionList) }

        val alreadyGrantedTypes = permissionList.filter { it.isGranted }.map { it.type }
        _sessionAttemptedPermissions.update { currentAttempts -> currentAttempts + alreadyGrantedTypes }

        return permissionList.all { it.isGranted }
    }

    fun initializeDraftSettings() {
        val settingsToEdit = _editingWorkPreset.value?.settings ?: _uiState.value.settings
        _draftSettings.value = settingsToEdit
    }

    private suspend fun performResetLogic(settings: Settings) {
        repo.saveActiveSprites(emptyList())
        _uiState.update {
            it.copy(
                timeLeft = settings.studyTime * 60,
                totalSessions = 0,
                isRunning = false,
                isPaused = true,
                isTimerStartedOnce = false,
                currentMode = Mode.STUDY,
                settings = settings,
                activeSprites = emptyList()
            )
        }
        timerService.resetCompletely(settings)
    }

    fun reset() {
        viewModelScope.launch {
            val currentSettings = _uiState.value.workPresets.find { it.id == _uiState.value.currentWorkId }?.settings ?: Settings()
            performResetLogic(currentSettings)
        }
    }

    fun saveSettingsAndReset() {
        viewModelScope.launch {
            val settingsToSave = _draftSettings.value ?: return@launch
            val editingId = _editingWorkPreset.value?.id
            val currentId = _uiState.value.currentWorkId

            // 1. 수정 중인 프리셋 ID 또는 현재 프리셋 ID를 기준으로 설정 값을 업데이트합니다.
            val presetIdToUpdate = editingId ?: currentId
            val updatedPresets = _uiState.value.workPresets.map { preset ->
                if (preset.id == presetIdToUpdate) {
                    preset.copy(settings = settingsToSave)
                } else {
                    preset
                }
            }

            // 2. 업데이트된 프리셋 목록 전체를 저장소에 저장합니다.
            repo.saveWorkPresets(updatedPresets)

            // 3. UI 상태의 프리셋 목록도 최신 버전으로 업데이트합니다.
            _uiState.update { it.copy(workPresets = updatedPresets) }

            // 4. '현재 활성화된' 프리셋의 최신 설정을 가져옵니다.
            //    (다른 프리셋을 수정했더라도, 메인 화면의 타이머는 활성 프리셋 설정을 따라야 합니다.)
            val newActiveSettings = updatedPresets.find { it.id == currentId }?.settings ?: Settings()

            // 5. 최신 활성 설정으로 타이머와 UI 상태를 리셋합니다.
            performResetLogic(newActiveSettings)

            // 6. 임시 설정 값을 초기화합니다.
            clearDraftSettings()
        }
    }

    fun clearDraftSettings() {
        _draftSettings.value = null
        _editingWorkPreset.value = null
    }

    private fun updateDraftSettings(transform: Settings.() -> Settings) {
        _draftSettings.update { it?.transform() }
    }

    fun addToWhitelist(packageName: String) {
        viewModelScope.launch {
            val updatedWhitelist = _uiState.value.whitelistedApps + packageName
            repo.saveWhitelistedApps(updatedWhitelist)
            _uiState.update { it.copy(whitelistedApps = updatedWhitelist) }
        }
    }

    fun removeFromWhitelist(packageName: String) {
        viewModelScope.launch {
            val updatedWhitelist = _uiState.value.whitelistedApps - packageName
            repo.saveWhitelistedApps(updatedWhitelist)
            _uiState.update { it.copy(whitelistedApps = updatedWhitelist) }
        }
    }

    fun selectWorkPreset(presetId: String) {
        viewModelScope.launch {
            repo.saveCurrentWorkId(presetId)
            _uiState.update {
                it.copy(currentWorkId = presetId)
            }
            reset()
        }
    }

    fun addWorkPreset() {
        viewModelScope.launch {
            val newPreset = WorkPreset(name = "새 Work", settings = Settings())
            val updatedPresets = _uiState.value.workPresets + newPreset
            repo.saveWorkPresets(updatedPresets)
            _uiState.update { it.copy(workPresets = updatedPresets) }
        }
    }

    fun deleteWorkPreset(id: String) {
        viewModelScope.launch {
            val updatedPresets = _uiState.value.workPresets.filterNot { it.id == id }
            repo.saveWorkPresets(updatedPresets)
            _uiState.update { it.copy(workPresets = updatedPresets) }
            if (_uiState.value.currentWorkId == id) {
                selectWorkPreset(updatedPresets.firstOrNull()?.id ?: "")
            }
        }
    }

    fun updateWorkPresetName(id: String, newName: String) {
        viewModelScope.launch {
            val updatedPresets = _uiState.value.workPresets.map {
                if (it.id == id) it.copy(name = newName) else it
            }
            repo.saveWorkPresets(updatedPresets)
            _uiState.update { it.copy(workPresets = updatedPresets) }
        }
    }

    fun startEditingWorkPreset(id: String) {
        val preset = _uiState.value.workPresets.find { it.id == id }
        _editingWorkPreset.value = preset
    }

    fun stopEditingWorkPreset() {
        _editingWorkPreset.value = null
        val currentSettings = _uiState.value.workPresets.find { it.id == _uiState.value.currentWorkId }?.settings ?: Settings()
        _uiState.update { it.copy(settings = currentSettings) }
    }

    fun updateBlockMode(mode: com.malrang.pomodoro.dataclass.ui.BlockMode) = updateDraftSettings { copy(blockMode = mode) }
    fun updateStudyTime(v: Int) = updateDraftSettings { copy(studyTime = v) }
    fun updateShortBreakTime(v: Int) = updateDraftSettings { copy(shortBreakTime = v) }
    fun updateLongBreakTime(v: Int) = updateDraftSettings { copy(longBreakTime = v) }
    fun updateLongBreakInterval(v: Int) = updateDraftSettings { copy(longBreakInterval = v) }
    fun toggleSound(b: Boolean) = updateDraftSettings { copy(soundEnabled = b) }
    fun toggleVibration(b: Boolean) = updateDraftSettings { copy(vibrationEnabled = b) }
    fun toggleAutoStart(b: Boolean) = updateDraftSettings { copy(autoStart = b) }

    fun toggleBackground() {
        viewModelScope.launch {
            val newPreference = !_uiState.value.useGrassBackground
            repo.saveUseGrassBackground(newPreference)
            _uiState.update { it.copy(useGrassBackground = newPreference) }
        }
    }

    fun startTimer() {
        if (_uiState.value.isRunning) return
        val s = _uiState.value
        _uiState.update { it.copy(isRunning = true, isPaused = false, isTimerStartedOnce = true) }
        timerService.start(s.timeLeft, s.settings, s.currentMode, s.totalSessions)
    }

    fun pauseTimer() {
        _uiState.update { it.copy(isRunning = false, isPaused = true) }
        timerService.pause()
    }

    fun skipSession() {
        val s = _uiState.value
        val nextMode: Mode
        val nextTime: Int
        var newTotalSessions = s.totalSessions
        if (s.currentMode == Mode.STUDY) {
            newTotalSessions++
            val isLongBreakTime = newTotalSessions > 0 && newTotalSessions % s.settings.longBreakInterval == 0
            nextMode = if (isLongBreakTime) Mode.LONG_BREAK else Mode.SHORT_BREAK
            nextTime = if (isLongBreakTime) s.settings.longBreakTime else s.settings.shortBreakTime
        } else {
            nextMode = Mode.STUDY
            nextTime = s.settings.studyTime
        }
        _uiState.update {
            it.copy(
                currentMode = nextMode,
                totalSessions = newTotalSessions,
                timeLeft = nextTime * 60,
                isRunning = false,
                isPaused = false
            )
        }
        timerService.skip(s.currentMode, newTotalSessions)
    }

    fun updateTimerStateFromService(timeLeft: Int, isRunning: Boolean, currentMode: Mode, totalSessions: Int) {
        _uiState.update {
            it.copy(
                timeLeft = timeLeft,
                isRunning = isRunning,
                isPaused = !isRunning && timeLeft > 0,
                currentMode = currentMode,
                totalSessions = totalSessions
            )
        }
    }

    fun showScreen(s: Screen) { _uiState.update { it.copy(currentScreen = s) } }

    fun updateSprites(deltaSec: Float, widthPx: Int, heightPx: Int) {
        _uiState.update { s ->
            val updated = s.activeSprites.map { sp ->
                var nx = sp.x + sp.vx * deltaSec
                var ny = sp.y + sp.vy * deltaSec
                var vx = sp.vx
                var vy = sp.vy
                val margin = 16f
                if (nx < margin) { nx = margin; vx = -vx }
                if (ny < margin) { ny = margin; vy = -vy }
                if (nx > widthPx - margin) { nx = widthPx - margin; vx = -vx }
                if (ny > heightPx - margin) { ny = heightPx - margin; vy = -vy }
                val nextState = if (sp.spriteState == SpriteState.IDLE && Random.nextFloat() < 0.003f) {
                    SpriteState.JUMP
                } else {
                    sp.spriteState
                }
                sp.copy(x = nx, y = ny, vx = vx, vy = vy, spriteState = nextState)
            }
            s.copy(activeSprites = updated)
        }
    }

    fun onJumpFinished(spriteId: String) {
        _uiState.update { s ->
            val updated = s.activeSprites.map { sp ->
                if (sp.id == spriteId) sp.copy(spriteState = SpriteState.IDLE) else sp
            }
            s.copy(activeSprites = updated)
        }
    }
}