package com.malrang.pomodoro.viewmodel

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
import java.util.UUID
import kotlin.random.Random

class PomodoroViewModel(
    private val repo: PomodoroRepository,
    private val timerService: TimerServiceProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(PomodoroUiState())
    val uiState: StateFlow<PomodoroUiState> = _uiState.asStateFlow()

    // --- ▼▼▼ 추가된 상태 ▼▼▼ ---
    // 현재 어떤 WorkPreset을 '편집'하고 있는지 ID로 추적. null이면 편집 중이 아님.
    private val _editingWorkPreset = MutableStateFlow<WorkPreset?>(null)
    val editingWorkPreset: StateFlow<WorkPreset?> = _editingWorkPreset.asStateFlow()
    // --- ▲▲▲ 추가된 상태 ▲▲▲ ---

    init {
        viewModelScope.launch {
            val seenIds = repo.loadSeenIds()
            val daily = repo.loadDailyStats()
            val seenAnimals = seenIds.mapNotNull { id -> AnimalsTable.byId(id) }
            val presets = repo.loadWorkPresets()
            val currentWorkId = repo.loadCurrentWorkId() ?: presets.firstOrNull()?.id
            val currentWork = presets.find { it.id == currentWorkId }
            val currentSettings = currentWork?.settings ?: Settings()

            _uiState.update {
                it.copy(
                    collectedAnimals = seenAnimals.toSet(),
                    dailyStats = daily,
                    settings = currentSettings,
                    workPresets = presets,
                    currentWorkId = currentWorkId
                )
            }
            if (TimerService.isServiceActive()) {
                timerService.requestStatus()
            } else {
                resetTimer()
            }
        }
    }

    /**
     * 사용자가 선택한 Work 프리셋으로 설정을 변경합니다.
     */
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
            resetTimer()
        }
    }

    // --- ▼▼▼ 추가/수정된 Work 관리 함수들 ▼▼▼ ---

    /**
     * 새로운 Work 프리셋을 추가합니다.
     */
    fun addWorkPreset() {
        viewModelScope.launch {
            val newPreset = WorkPreset(name = "새 Work", settings = Settings())
            val updatedPresets = _uiState.value.workPresets + newPreset
            repo.saveWorkPresets(updatedPresets)
            _uiState.update { it.copy(workPresets = updatedPresets) }
        }
    }

    /**
     * Work 프리셋을 삭제합니다.
     */
    fun deleteWorkPreset(id: String) {
        viewModelScope.launch {
            val updatedPresets = _uiState.value.workPresets.filterNot { it.id == id }
            repo.saveWorkPresets(updatedPresets)
            _uiState.update { it.copy(workPresets = updatedPresets) }
            // 만약 삭제한 프리셋이 현재 선택된 프리셋이었다면, 첫 번째 프리셋을 선택
            if (_uiState.value.currentWorkId == id) {
                selectWorkPreset(updatedPresets.firstOrNull()?.id ?: "")
            }
        }
    }

    /**
     * Work 프리셋의 이름을 변경합니다.
     */
    fun updateWorkPresetName(id: String, newName: String) {
        viewModelScope.launch {
            val updatedPresets = _uiState.value.workPresets.map {
                if (it.id == id) it.copy(name = newName) else it
            }
            repo.saveWorkPresets(updatedPresets)
            _uiState.update { it.copy(workPresets = updatedPresets) }
        }
    }

    /**
     * 특정 Work 프리셋의 '편집 모드'를 시작합니다.
     */
    fun startEditingWorkPreset(id: String) {
        val preset = _uiState.value.workPresets.find { it.id == id }
        _editingWorkPreset.value = preset
    }

    /**
     * '편집 모드'를 종료합니다.
     */
    fun stopEditingWorkPreset() {
        _editingWorkPreset.value = null
        // 편집이 끝났으므로, 현재 활성화된 Work의 설정으로 다시 UI를 동기화
        val currentSettings = _uiState.value.workPresets.find { it.id == _uiState.value.currentWorkId }?.settings ?: Settings()
        _uiState.update { it.copy(settings = currentSettings) }
    }

    /**
     * 설정 값 변경을 처리하는 범용 함수. '편집 모드'를 우선적으로 반영.
     */
    private fun updateSettings(transform: Settings.() -> Settings) {
        viewModelScope.launch {
            val editingId = _editingWorkPreset.value?.id
            if (editingId != null) {
                // 편집 모드일 경우, 해당 프리셋의 설정을 변경
                val updatedPresets = _uiState.value.workPresets.map {
                    if (it.id == editingId) {
                        val updatedSettings = it.settings.transform()
                        // 편집 상태와 UI 상태를 즉시 동기화
                        _editingWorkPreset.value = it.copy(settings = updatedSettings)
                        it.copy(settings = updatedSettings)
                    } else {
                        it
                    }
                }
                repo.saveWorkPresets(updatedPresets)
                _uiState.update { it.copy(workPresets = updatedPresets) }

            } else {
                // 편집 모드가 아닐 경우 (이전 로직), 현재 활성화된 Work의 설정을 변경
                val currentId = _uiState.value.currentWorkId
                val updatedPresets = _uiState.value.workPresets.map {
                    if (it.id == currentId) {
                        it.copy(settings = it.settings.transform())
                    } else {
                        it
                    }
                }
                val newSettings = updatedPresets.find { it.id == currentId }?.settings ?: Settings()
                repo.saveWorkPresets(updatedPresets)
                _uiState.update { it.copy(workPresets = updatedPresets, settings = newSettings) }
            }
        }
    }
    // --- ▲▲▲ 추가/수정된 Work 관리 함수들 ▲▲▲ ---

    // ... 기존 타이머 및 통계 관련 함수들 (updateTodayStats 등)은 모두 그대로 유지 ...

    // 설정 변경 함수들은 모두 내부적으로 updateSettings를 호출하므로 수정할 필요 없음
    fun updateStudyTime(v: Int) {
        updateSettings { copy(studyTime = v) }
        _uiState.update { state ->
            if (state.currentMode == Mode.STUDY && !state.isRunning && !state.isPaused) {
                state.copy(timeLeft = v * 60)
            } else {
                state
            }
        }
    }
    fun updateShortBreakTime(v: Int) = updateSettings { copy(shortBreakTime = v) }
    fun updateLongBreakTime(v: Int) = updateSettings { copy(longBreakTime = v) }
    fun updateLongBreakInterval(v: Int) = updateSettings { copy(longBreakInterval = v) }
    fun toggleSound(b: Boolean) = updateSettings { copy(soundEnabled = b) }
    fun toggleVibration(b: Boolean) = updateSettings { copy(vibrationEnabled = b) }
    fun toggleAutoStart(b: Boolean) = updateSettings { copy(autoStart = b) }


    // ... 나머지 함수들 (showScreen, makeSprite 등)은 모두 그대로 유지 ...

    private suspend fun updateTodayStats(finishedMode: Mode) {
        val today = LocalDate.now().toString()
        val s = _uiState.value
        val currentStatsMap = repo.loadDailyStats().toMutableMap()
        val todayStat = currentStatsMap[today] ?: DailyStat(today)

        val currentWorkName = s.workPresets.find { it.id == s.currentWorkId }?.name ?: "알 수 없는 Work"

        val updatedStat = when (finishedMode) {
            Mode.STUDY -> {
                val newStudyTimeMap = (todayStat.studyTimeByWork ?: emptyMap()).toMutableMap()
                val currentWorkTime = newStudyTimeMap.getOrDefault(currentWorkName, 0)
                newStudyTimeMap[currentWorkName] = currentWorkTime + s.settings.studyTime
                todayStat.copy(studyTimeByWork = newStudyTimeMap)
            }
            Mode.SHORT_BREAK -> {
                val newBreakTimeMap = (todayStat.breakTimeByWork ?: emptyMap()).toMutableMap()
                val currentWorkTime = newBreakTimeMap.getOrDefault(currentWorkName, 0)
                newBreakTimeMap[currentWorkName] = currentWorkTime + s.settings.shortBreakTime
                todayStat.copy(breakTimeByWork = newBreakTimeMap)
            }
            Mode.LONG_BREAK -> {
                val newBreakTimeMap = (todayStat.breakTimeByWork ?: emptyMap()).toMutableMap()
                val currentWorkTime = newBreakTimeMap.getOrDefault(currentWorkName, 0)
                newBreakTimeMap[currentWorkName] = currentWorkTime + s.settings.longBreakTime
                todayStat.copy(breakTimeByWork = newBreakTimeMap)
            }
        }
        currentStatsMap[today] = updatedStat
        repo.saveDailyStats(currentStatsMap)
        _uiState.update { it.copy(dailyStats = currentStatsMap) }
    }


    fun startTimer() {
        if (_uiState.value.isRunning) return
        val s = _uiState.value
        _uiState.update { it.copy(isRunning = true, isPaused = false, isTimerStartedOnce = true) }
        timerService.start(s.timeLeft, s.settings, s.currentMode, s.totalSessions)
    }

    fun resetTimer() {
        val s = _uiState.value
        val newTimeLeft = when (s.currentMode) {
            Mode.STUDY -> s.settings.studyTime
            Mode.SHORT_BREAK -> s.settings.shortBreakTime
            Mode.LONG_BREAK -> s.settings.longBreakTime
        }
        _uiState.update { it.copy(isRunning = false, isPaused = false, timeLeft = newTimeLeft * 60, isTimerStartedOnce = false) }
        timerService.reset(newTimeLeft * 60)
    }
    fun pauseTimer() {
        _uiState.update { it.copy(isRunning = false, isPaused = true) }
        timerService.pause()
    }

    fun skipSession() {
        val s = _uiState.value
        val finishedMode = s.currentMode

        // ✅ DB 저장 없음 (updateTodayStats 호출하지 않음)

        // 다음 모드 및 세션 계산
        val nextMode: Mode
        val nextTime: Int
        var newTotalSessions = s.totalSessions

        if (finishedMode == Mode.STUDY) {
            newTotalSessions++
            val isLongBreakTime = newTotalSessions > 0 &&
                    newTotalSessions % s.settings.longBreakInterval == 0
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

        // Service에도 skip 요청 전달
        timerService.skip(s.currentMode, newTotalSessions)
    }
    
    fun onTimerFinishedFromService() {
        val finishedMode = _uiState.value.currentMode
        viewModelScope.launch {
            updateTodayStats(finishedMode)
        }

        if (finishedMode == Mode.STUDY) {
            handleStudySessionCompletion()
        }
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
    private fun handleStudySessionCompletion() {
        val animal = getRandomAnimal()
        val sprite = makeSprite(animal)
        val updatedCollectedAnimals = _uiState.value.collectedAnimals + animal
        val updatedSeenIds = updatedCollectedAnimals.map { it.id }.toSet()

        viewModelScope.launch {
            repo.saveSeenIds(updatedSeenIds)
        }
        _uiState.update {
            it.copy(
                activeSprites = it.activeSprites + sprite,
                collectedAnimals = updatedCollectedAnimals
            )
        }
    }
    fun showScreen(s: Screen) { _uiState.update { it.copy(currentScreen = s) } }


    private fun makeSprite(animal: Animal): AnimalSprite {
        val spriteData = SpriteMap.map[animal]
            ?: SpriteData(
                idleRes = R.drawable.classical_idle,
                jumpRes = R.drawable.classical_jump,
            )
        return AnimalSprite(
            id = UUID.randomUUID().toString(),
            animalId = animal.id,
            idleSheetRes = spriteData.idleRes,
            idleCols = spriteData.idleCols,
            idleRows = spriteData.idleRows,
            jumpSheetRes = spriteData.jumpRes,
            jumpCols = spriteData.jumpCols,
            jumpRows = spriteData.jumpRows,
            x = Random.nextInt(0, 600).toFloat(),
            y = Random.nextInt(0, 1000).toFloat(),
            vx = listOf(-70f, 70f).random(),
            vy = listOf(-50f, 50f).random(),
            sizeDp = 48f
        )
    }

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

    private fun getRandomAnimal(): Animal {
        val roll = Random.nextInt(100)
        val rarity = when {
            roll < 60 -> Rarity.COMMON
            roll < 85 -> Rarity.RARE
            roll < 97 -> Rarity.EPIC
            else -> Rarity.LEGENDARY
        }
        return AnimalsTable.randomByRarity(rarity)
    }
}