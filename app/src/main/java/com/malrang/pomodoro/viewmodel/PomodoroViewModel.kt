package com.malrang.pomodoro.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malrang.pomodoro.R
import com.malrang.pomodoro.dataclass.animalInfo.Animal
import com.malrang.pomodoro.dataclass.animalInfo.AnimalsTable
import com.malrang.pomodoro.dataclass.animalInfo.Rarity
import com.malrang.pomodoro.dataclass.sprite.AnimalSprite
import com.malrang.pomodoro.dataclass.sprite.SpriteData
import com.malrang.pomodoro.dataclass.sprite.SpriteMap
import com.malrang.pomodoro.dataclass.sprite.SpriteState
import com.malrang.pomodoro.dataclass.ui.DailyStat
import com.malrang.pomodoro.dataclass.ui.Mode
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
import java.time.LocalDate
import kotlin.random.Random

class PomodoroViewModel(
    private val repo: PomodoroRepository,
    private val timerService: TimerServiceProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(PomodoroUiState())
    val uiState: StateFlow<PomodoroUiState> = _uiState.asStateFlow()

    private val _editingWorkPreset = MutableStateFlow<WorkPreset?>(null)
    val editingWorkPreset: StateFlow<WorkPreset?> = _editingWorkPreset.asStateFlow()

    init {
        viewModelScope.launch {
            val seenIds = repo.loadSeenIds()
            val daily = repo.loadDailyStats()
            val presets = repo.loadWorkPresets()
            val whitelistedApps = repo.loadWhitelistedApps() // ✅ 화이트리스트 로드
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
                    whitelistedApps = whitelistedApps // ✅ UI 상태에 화이트리스트 추가
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
     * ✅ 화이트리스트에 앱을 추가합니다.
     * @param packageName 추가할 앱의 패키지 이름
     */
    fun addToWhitelist(packageName: String) {
        viewModelScope.launch {
            val updatedWhitelist = _uiState.value.whitelistedApps + packageName
            repo.saveWhitelistedApps(updatedWhitelist)
            _uiState.update { it.copy(whitelistedApps = updatedWhitelist) }
        }
    }

    /**
     * ✅ 화이트리스트에서 앱을 제거합니다.
     * @param packageName 제거할 앱의 패키지 이름
     */
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
            _uiState.update {
                it.copy(
                    currentWorkId = presetId,
                    settings = selectedPreset.settings
                )
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

    private fun updateSettings(transform: Settings.() -> Settings) {
        viewModelScope.launch {
            val editingId = _editingWorkPreset.value?.id
            if (editingId != null) {
                val updatedPresets = _uiState.value.workPresets.map {
                    if (it.id == editingId) {
                        val updatedSettings = it.settings.transform()
                        _editingWorkPreset.value = it.copy(settings = updatedSettings)
                        it.copy(settings = updatedSettings)
                    } else it
                }
                repo.saveWorkPresets(updatedPresets)
                _uiState.update { it.copy(workPresets = updatedPresets) }

            } else {
                val currentId = _uiState.value.currentWorkId
                val updatedPresets = _uiState.value.workPresets.map {
                    if (it.id == currentId) it.copy(settings = it.settings.transform())
                    else it
                }
                val newSettings = updatedPresets.find { it.id == currentId }?.settings ?: Settings()
                repo.saveWorkPresets(updatedPresets)
                _uiState.update { it.copy(workPresets = updatedPresets, settings = newSettings) }
            }
        }
    }

    // ✅ 차단 모드 업데이트 함수 추가
    fun updateBlockMode(mode: com.malrang.pomodoro.dataclass.ui.BlockMode) = updateSettings { copy(blockMode = mode) }


    fun updateStudyTime(v: Int) {
        updateSettings { copy(studyTime = v) }
        _uiState.update { state ->
            if (state.currentMode == Mode.STUDY && !state.isRunning && !state.isPaused) {
                state.copy(timeLeft = v * 60)
            } else state
        }
    }
    fun updateShortBreakTime(v: Int) = updateSettings { copy(shortBreakTime = v) }
    fun updateLongBreakTime(v: Int) = updateSettings { copy(longBreakTime = v) }
    fun updateLongBreakInterval(v: Int) = updateSettings { copy(longBreakInterval = v) }
    fun toggleSound(b: Boolean) = updateSettings { copy(soundEnabled = b) }
    fun toggleVibration(b: Boolean) = updateSettings { copy(vibrationEnabled = b) }
    fun toggleAutoStart(b: Boolean) = updateSettings { copy(autoStart = b) }

    // --- ▼▼▼ 추가된 함수 ▼▼▼ ---
    /**
     * 배경화면 설정을 토글합니다 (잔디/어두운 배경).
     */
    fun toggleBackground() {
        viewModelScope.launch {
            val newPreference = !_uiState.value.useGrassBackground
            repo.saveUseGrassBackground(newPreference)
            _uiState.update { it.copy(useGrassBackground = newPreference) }
        }
    }
    // --- ▲▲▲ 추가된 함수 ▲▲▲ ---

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

    fun reset() {
        viewModelScope.launch {
            val s = _uiState.value
            val currentWorkSettings = s.workPresets.find { it.id == s.currentWorkId }?.settings ?: Settings()

            // 스프라이트 목록을 비우고 저장소에도 반영
            repo.saveActiveSprites(emptyList())

            _uiState.update {
                it.copy(
                    timeLeft = currentWorkSettings.studyTime * 60,
                    totalSessions = 0,
                    isRunning = false,
                    isPaused = true,
                    isTimerStartedOnce = false,
                    currentMode = Mode.STUDY,
                    settings = currentWorkSettings,
                    activeSprites = emptyList()
                )
            }
            // 서비스 상태도 완전히 초기화
            timerService.resetCompletely(_uiState.value.settings)
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