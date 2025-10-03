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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

class PomodoroViewModel(
    private val localRepo: PomodoroRepository,
    private val timerService: TimerServiceProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PomodoroUiState())
    val uiState: StateFlow<PomodoroUiState> = _uiState.asStateFlow()

    private val _sessionAttemptedPermissions = MutableStateFlow<Set<PermissionType>>(emptySet())
    val sessionAttemptedPermissions: StateFlow<Set<PermissionType>> = _sessionAttemptedPermissions.asStateFlow()
    private val _notificationDenialCount = MutableStateFlow(0)
    val notificationDenialCount: StateFlow<Int> = _notificationDenialCount.asStateFlow()
    private val _editingWorkPreset = MutableStateFlow<WorkPreset?>(null)
    val editingWorkPreset: StateFlow<WorkPreset?> = _editingWorkPreset.asStateFlow()
    private val _draftSettings = MutableStateFlow<Settings?>(null)
    val draftSettings: StateFlow<Settings?> = _draftSettings.asStateFlow()

    // 네비게이션 이벤트를 위한 SharedFlow
    private val _navigationEvents = MutableSharedFlow<Screen>()
    val navigationEvents = _navigationEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            _notificationDenialCount.value = localRepo.loadNotificationDenialCount()

            val seenIds = localRepo.loadSeenIds()
            val daily = localRepo.loadDailyStats()
            val presets = localRepo.loadWorkPresets()
            val whitelistedApps = localRepo.loadWhitelistedApps()
            val currentWorkId = localRepo.loadCurrentWorkId() ?: presets.firstOrNull()?.id
            val sprites = localRepo.loadActiveSprites()
            val useGrassBackground = localRepo.loadUseGrassBackground()

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
                // 서비스가 비활성 상태일 때, 저장된 타이머 상태를 불러옵니다.
                val savedState = localRepo.loadTimerState()
                if (savedState != null) {
                    _uiState.update {
                        it.copy(
                            timeLeft = savedState.timeLeft,
                            currentMode = savedState.currentMode,
                            totalSessions = savedState.totalSessions,
                            isRunning = false,
                            isPaused = true, // 저장된 상태는 항상 '일시정지' 상태로 로드
                            isTimerStartedOnce = savedState.timeLeft < (it.settings.studyTime * 60)
                        )
                    }
                } else {
                    // 저장된 상태가 없으면 초기화
                    reset()
                }
            }
        }
    }


    /**
     * 권한을 확인하고, 필요하다면 권한 화면으로 이동시키는 함수
     */
    fun checkPermissionsAndNavigateIfNeeded(context: Context) {
        val allGranted = checkAndupdatePermissions(context)
        if (!allGranted) {
            // 권한이 하나라도 없으면 권한 화면으로 보냅니다.
            navigateTo(Screen.Permission)
        } else {
            // 모든 권한이 있고, 현재 화면이 혹시 권한 화면이었다면 메인으로 보냅니다.
            if (_uiState.value.currentScreen == Screen.Permission) {
                navigateTo(Screen.Main)
            }
            // 이미 메인 화면이라면 아무것도 하지 않습니다.
        }
    }

    fun onPermissionRequestResult(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            checkAndupdatePermissions(context)
            return
        }

        val notificationPermissionInfo = uiState.value.permissions.find { it.type == PermissionType.NOTIFICATION }
        if (notificationPermissionInfo == null) {
            checkAndupdatePermissions(context)
            return
        }

        val wasAttempted = sessionAttemptedPermissions.value.contains(PermissionType.NOTIFICATION)
        val isGrantedNow = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (wasAttempted && !notificationPermissionInfo.isGranted && !isGrantedNow) {
            val newCount = _notificationDenialCount.value + 1
            viewModelScope.launch {
                localRepo.saveNotificationDenialCount(newCount)
                _notificationDenialCount.value = newCount
            }
        }
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
                    type = PermissionType.NOTIFICATION, title = "알림",
                    description = "타이머 진행 상황을 알림으로 표시하기 위해 필요합니다.",
                    isGranted = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                )
            )
        }
        permissionList.add(
            PermissionInfo(
                type = PermissionType.OVERLAY, title = "다른 앱 위에 표시",
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
                type = PermissionType.USAGE_STATS, title = "사용 정보 접근",
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
    fun refreshActiveSprites() {
        viewModelScope.launch {
            val updatedSprites = localRepo.loadActiveSprites()
            _uiState.update { it.copy(activeSprites = updatedSprites) }
        }
    }
    fun requestTimerStatus() {
        if (TimerService.isServiceActive()) {
            timerService.requestStatus()
        }
    }


    private suspend fun performResetLogic(settings: Settings) {
        // [수정] 저장된 타이머 상태와 활성 스프라이트를 모두 삭제합니다.
        localRepo.clearTimerState()
        localRepo.saveActiveSprites(emptyList())

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


    fun startTimer() {
        if (_uiState.value.isRunning) return
        val s = _uiState.value
        _uiState.update { it.copy(isRunning = true, isPaused = false, isTimerStartedOnce = true) }
        timerService.start(s.timeLeft, s.settings, s.currentMode, s.totalSessions)
    }

    fun pauseTimer() {
        // 일시정지 시 동물들의 현재 상태(위치 등)를 저장합니다.
        viewModelScope.launch {
            localRepo.saveActiveSprites(uiState.value.activeSprites)
        }
        _uiState.update { it.copy(isRunning = false, isPaused = true) }
        timerService.pause()
    }

    fun saveSettingsAndReset() {
        viewModelScope.launch {
            val settingsToSave = _draftSettings.value ?: return@launch
            val editingId = _editingWorkPreset.value?.id
            val currentId = _uiState.value.currentWorkId
            val presetIdToUpdate = editingId ?: currentId
            val updatedPresets = _uiState.value.workPresets.map { preset ->
                if (preset.id == presetIdToUpdate) {
                    preset.copy(settings = settingsToSave)
                } else {
                    preset
                }
            }
            localRepo.saveWorkPresets(updatedPresets)
            _uiState.update { it.copy(workPresets = updatedPresets) }
            val newActiveSettings = updatedPresets.find { it.id == currentId }?.settings ?: Settings()
            performResetLogic(newActiveSettings)
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
            localRepo.saveWhitelistedApps(updatedWhitelist)
            _uiState.update { it.copy(whitelistedApps = updatedWhitelist) }
        }
    }
    fun removeFromWhitelist(packageName: String) {
        viewModelScope.launch {
            val updatedWhitelist = _uiState.value.whitelistedApps - packageName
            localRepo.saveWhitelistedApps(updatedWhitelist)
            _uiState.update { it.copy(whitelistedApps = updatedWhitelist) }
        }
    }
    fun selectWorkPreset(presetId: String) {
        viewModelScope.launch {
            localRepo.saveCurrentWorkId(presetId)
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
            localRepo.saveWorkPresets(updatedPresets)
            _uiState.update { it.copy(workPresets = updatedPresets) }
        }
    }
    fun deleteWorkPreset(id: String) {
        viewModelScope.launch {
            val updatedPresets = _uiState.value.workPresets.filterNot { it.id == id }
            localRepo.saveWorkPresets(updatedPresets)
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
            localRepo.saveWorkPresets(updatedPresets)
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
            localRepo.saveUseGrassBackground(newPreference)
            _uiState.update { it.copy(useGrassBackground = newPreference) }
        }
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
                currentMode = nextMode, totalSessions = newTotalSessions, timeLeft = nextTime * 60,
                isRunning = false, isPaused = false
            )
        }
        timerService.skip(s.currentMode, newTotalSessions)
    }
    fun updateTimerStateFromService(timeLeft: Int, isRunning: Boolean, currentMode: Mode, totalSessions: Int) {
        _uiState.update {
            it.copy(
                timeLeft = timeLeft, isRunning = isRunning, isPaused = !isRunning && timeLeft > 0,
                currentMode = currentMode, totalSessions = totalSessions
            )
        }
    }

    /**
     * 화면 이동을 위한 함수. 이제 상태를 직접 바꾸지 않고 이벤트를 발생시킵니다.
     */

    fun navigateTo(s: Screen) {
        viewModelScope.launch {
            _navigationEvents.emit(s)
        }
    }

}