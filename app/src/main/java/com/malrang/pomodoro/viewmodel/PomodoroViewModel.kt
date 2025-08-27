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

    private val _editingWorkPreset = MutableStateFlow<WorkPreset?>(null)
    val editingWorkPreset: StateFlow<WorkPreset?> = _editingWorkPreset.asStateFlow()

    private val _draftSettings = MutableStateFlow<Settings?>(null)
    val draftSettings: StateFlow<Settings?> = _draftSettings.asStateFlow()

    init {
        viewModelScope.launch {
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
     * ✅ 현재 필요한 모든 권한의 상태를 확인하고 UI 상태를 업데이트합니다.
     * 모든 권한이 부여되었는지 여부를 반환합니다.
     * @return 모든 권한이 부여되었다면 true, 아니면 false.
     */
    fun checkAndupdatePermissions(context: Context): Boolean {
        val permissionList = mutableListOf<PermissionInfo>()

        // 1. 알림 권한 (API 33+)
        // [설명] POST_NOTIFICATIONS 권한은 티라미수(API 33) 이상에서만 존재합니다.
        // 따라서 해당 버전 이상일 경우에만 권한 목록에 추가하고 상태를 확인합니다.
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
        // [설명] 티라미수 미만 버전에서는 이 권한이 없으므로, 권한 확인 목록에 추가하지 않습니다.
        // 이것이 하위 버전에 대한 올바른 처리 방식입니다.

        // 2. 다른 앱 위에 표시 권한
        permissionList.add(
            PermissionInfo(
                type = PermissionType.OVERLAY,
                title = "다른 앱 위에 표시",
                description = "공부 중 다른 앱 사용 시 경고창을 띄우기 위해 필요합니다.",
                isGranted = android.provider.Settings.canDrawOverlays(context)
            )
        )

        // 3. 사용 정보 접근 권한
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

        return permissionList.all { it.isGranted }
    }


    /**
     * 설정 화면에 진입할 때, 현재 설정을 임시 설정으로 복사합니다.
     */
    fun initializeDraftSettings() {
        val settingsToEdit = _editingWorkPreset.value?.settings ?: _uiState.value.settings
        _draftSettings.value = settingsToEdit
    }

    /**
     * 핵심 리셋 로직을 담고 있는 비공개 헬퍼 함수입니다.
     * UI 상태 업데이트와 서비스 리셋을 담당합니다.
     */
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

    /**
     * 메인 화면의 리셋 버튼을 위한 함수입니다. 현재 설정을 기준으로 리셋을 수행합니다.
     */
    fun reset() {
        viewModelScope.launch {
            val currentSettings = _uiState.value.workPresets.find { it.id == _uiState.value.currentWorkId }?.settings ?: Settings()
            performResetLogic(currentSettings)
        }
    }

    /**
     * 설정 화면에서 변경된 내용을 저장하고, 그 설정에 맞춰 타이머를 리셋합니다.
     */
    fun saveSettingsAndReset() {
        viewModelScope.launch {
            val settingsToSave = _draftSettings.value ?: return@launch
            val editingId = _editingWorkPreset.value?.id
            val currentId = _uiState.value.currentWorkId

            val updatedPresets = _uiState.value.workPresets.map { preset ->
                val presetIdToUpdate = editingId ?: currentId
                if (preset.id == presetIdToUpdate) {
                    preset.copy(settings = settingsToSave)
                } else {
                    preset
                }
            }
            repo.saveWorkPresets(updatedPresets)

            val newMainUiSettings = if (currentId == (editingId ?: currentId)) {
                settingsToSave
            } else {
                _uiState.value.settings
            }

            // 리셋 로직을 직접 작성하는 대신, 헬퍼 함수 호출로 대체합니다.
            performResetLogic(newMainUiSettings)

            // 작업이 완료되었으므로 임시 설정(draft)을 초기화합니다.
            clearDraftSettings()
        }
    }

    /**
     * 설정 변경을 취소하고 임시 데이터를 초기화합니다.
     */
    fun clearDraftSettings() {
        _draftSettings.value = null
        _editingWorkPreset.value = null
    }

    /**
     * 임시 설정 값을 업데이트하는 내부 함수
     */
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
            val selectedPreset = _uiState.value.workPresets.find { it.id == presetId } ?: return@launch
            repo.saveCurrentWorkId(presetId)
            // settings 객체를 직접 업데이트하는 대신 reset()을 호출하여 일관성을 유지합니다.
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