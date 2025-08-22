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
import com.malrang.pomodoro.dataclass.ui.WorkPreset // --- import 추가 ---
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

    init {
        viewModelScope.launch {
            // --- ▼▼▼ 수정된 초기화 로직 ▼▼▼ ---
            val seenIds = repo.loadSeenIds()
            val daily = repo.loadDailyStats()
            val seenAnimals = seenIds.mapNotNull { id -> AnimalsTable.byId(id) }

            // Work 프리셋과 현재 Work ID 로드
            val presets = repo.loadWorkPresets()
            val currentWorkId = repo.loadCurrentWorkId() ?: presets.firstOrNull()?.id

            // 현재 Work에 맞는 설정 찾기
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
            // --- ▲▲▲ 수정된 초기화 로직 ▲▲▲ ---

            if (TimerService.isServiceActive()) {
                timerService.requestStatus()
            } else {
                resetTimer()
            }
        }
    }

    // --- ▼▼▼ 추가된 함수 ▼▼▼ ---
    /**
     * 사용자가 선택한 Work 프리셋으로 설정을 변경합니다.
     * @param presetId 선택된 WorkPreset의 고유 ID
     */
    fun selectWorkPreset(presetId: String) {
        viewModelScope.launch {
            val selectedPreset = _uiState.value.workPresets.find { it.id == presetId } ?: return@launch

            // 선택된 ID를 저장소에 저장
            repo.saveCurrentWorkId(presetId)

            // UI 상태 업데이트
            _uiState.update {
                it.copy(
                    currentWorkId = presetId,
                    settings = selectedPreset.settings
                )
            }
            // 타이머를 리셋하여 새 설정을 즉시 반영
            resetTimer()
        }
    }
    // --- ▲▲▲ 추가된 함수 ▲▲▲ ---

    // ... 기존 ViewModel 함수들 ...
    // (startTimer, resetTimer, pauseTimer 등 모든 함수는 그대로 유지)

    // updateSettings 함수는 이제 Work 프리셋을 직접 수정할 때 사용될 수 있습니다.
    // (이 예제에서는 프리셋 수정 기능까지는 구현하지 않음)
    private fun updateSettings(transform: Settings.() -> Settings) {
        _uiState.update { state ->
            val updated = state.settings.transform()
            viewModelScope.launch {
                repo.saveSettings(updated)
            }
            state.copy(settings = updated)
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

    fun onTimerFinishedFromService() {
        val finishedMode = _uiState.value.currentMode
        viewModelScope.launch {
            updateTodayStats(finishedMode)
        }

        if (finishedMode == Mode.STUDY) {
            handleStudySessionCompletion()
        }
    }

    private suspend fun updateTodayStats(finishedMode: Mode) {
        val today = LocalDate.now().toString()
        val s = _uiState.value
        val currentStatsMap = repo.loadDailyStats().toMutableMap()
        val todayStat = currentStatsMap[today] ?: DailyStat(today)

        // --- ▼▼▼ 수정된 통계 기록 로직 ▼▼▼ ---

        // 1. 현재 Work의 이름을 찾습니다.
        val currentWorkName = s.workPresets.find { it.id == s.currentWorkId }?.name ?: "알 수 없는 Work"

        // 2. 끝난 모드(공부/휴식)에 따라 해당 Work의 시간을 업데이트합니다.
        val updatedStat = when (finishedMode) {
            Mode.STUDY -> {
                // studyTimeByWork가 null이면 emptyMap()을 사용한 후 toMutableMap() 호출
                val newStudyTimeMap = (todayStat.studyTimeByWork ?: emptyMap()).toMutableMap()
                val currentWorkTime = newStudyTimeMap.getOrDefault(currentWorkName, 0)
                newStudyTimeMap[currentWorkName] = currentWorkTime + s.settings.studyTime
                todayStat.copy(studyTimeByWork = newStudyTimeMap)
            }
            Mode.SHORT_BREAK -> {
                // breakTimeByWork가 null이면 emptyMap()을 사용한 후 toMutableMap() 호출
                val newBreakTimeMap = (todayStat.breakTimeByWork ?: emptyMap()).toMutableMap()
                val currentWorkTime = newBreakTimeMap.getOrDefault(currentWorkName, 0)
                newBreakTimeMap[currentWorkName] = currentWorkTime + s.settings.shortBreakTime
                todayStat.copy(breakTimeByWork = newBreakTimeMap)
            }
            Mode.LONG_BREAK -> {
                // breakTimeByWork가 null이면 emptyMap()을 사용한 후 toMutableMap() 호출
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
    fun updateShortBreakTime(v: Int) {
        updateSettings { copy(shortBreakTime = v) }
        _uiState.update { state ->
            if (state.currentMode == Mode.SHORT_BREAK && !state.isRunning && !state.isPaused) {
                state.copy(timeLeft = v * 60)
            } else {
                state
            }
        }
    }

    fun updateLongBreakTime(v: Int) {
        updateSettings { copy(longBreakTime = v) }
        _uiState.update { state ->
            if (state.currentMode == Mode.LONG_BREAK && !state.isRunning && !state.isPaused) {
                state.copy(timeLeft = v * 60)
            } else {
                state
            }
        }
    }

    fun updateLongBreakInterval(v: Int) {
        updateSettings { copy(longBreakInterval = v) }
    }

    fun toggleSound(b: Boolean) = updateSettings{copy(soundEnabled = b)}
    fun toggleVibration(b: Boolean) = updateSettings{ copy(vibrationEnabled = b) }
    fun toggleAutoStart(b: Boolean) = updateSettings{ copy(autoStart = b) }


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