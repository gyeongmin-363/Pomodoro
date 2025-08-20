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
import com.malrang.pomodoro.localRepo.PomodoroRepository
import com.malrang.pomodoro.localRepo.SoundPlayer
import com.malrang.pomodoro.localRepo.VibratorHelper
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
    private val soundPlayer: SoundPlayer,
    private val vibratorHelper: VibratorHelper,
    private val timerService: TimerServiceProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(PomodoroUiState())
    val uiState: StateFlow<PomodoroUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val seenIds = repo.loadSeenIds()
            val daily = repo.loadDailyStats()
            val loaded = repo.loadSettings()
            val seenAnimals = seenIds.mapNotNull { id -> AnimalsTable.byId(id) }
            _uiState.update {
                it.copy(
                    collectedAnimals = seenAnimals.toSet(),
                    dailyStats = daily,
                    settings = loaded,
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
     * --- 변경: 서비스로부터 모든 세션 상태를 받아 UI를 업데이트합니다. ---
     * @param currentMode 서비스의 현재 모드
     * @param totalSessions 서비스가 계산한 총 완료 세션 수
     */
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

    /**
     * 서비스로부터 세션 종료 신호를 받으면 호출됩니다.
     */
    fun onTimerFinishedFromService() {
        // --- 변경: '공부' 세션이 끝났을 때만 동물 획득 로직을 처리합니다. ---
        if (_uiState.value.currentMode == Mode.STUDY) {
            handleStudySessionCompletion()
        }
    }

    fun startTimer() {
        if (_uiState.value.isRunning) return
        val s = _uiState.value
        _uiState.update { it.copy(isRunning = true, isPaused = false) }
        timerService.start(s.timeLeft, s.settings, s.currentMode, s.totalSessions)
    }

    fun pauseTimer() {
        _uiState.update { it.copy(isRunning = false, isPaused = true) }
        timerService.pause()
    }

    fun resetTimer() {
        val s = _uiState.value
        val newTimeLeft = when (s.currentMode) {
            Mode.STUDY -> s.settings.studyTime
            Mode.SHORT_BREAK -> s.settings.shortBreakTime
            Mode.LONG_BREAK -> s.settings.longBreakTime
        }
        _uiState.update { it.copy(isRunning = false, isPaused = false, timeLeft = newTimeLeft * 60) }
        timerService.reset(newTimeLeft * 60)
    }

    /**
     * --- 변경: 함수 이름 및 역할 변경 ---
     * '공부' 세션 완료 시 보상(동물 획득, 저장)만 처리합니다.
     * UI의 세션 상태(모드, 시간 등)는 변경하지 않습니다. (서비스가 보내주는 정보로만 업데이트)
     */
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
                // 동물 관련 UI만 업데이트
                activeSprites = it.activeSprites + sprite,
                collectedAnimals = updatedCollectedAnimals
            )
        }
    }

    // ... 나머지 코드는 동일 ...

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

    private fun updateSettings(transform: Settings.() -> Settings) {
        _uiState.update { state ->
            val updated = state.settings.transform()
            viewModelScope.launch {
                repo.saveSettings(updated)
            }
            state.copy(settings = updated)
        }
    }

    fun showScreen(s: Screen) { _uiState.update { it.copy(currentScreen = s) } }

    private suspend fun incTodayStat() {
        val today = LocalDate.now().toString()
        val current = repo.loadDailyStats().toMutableMap()
        val prev = current[today]?.studySessions ?: 0
        current[today] = DailyStat(today, prev + 1)
        repo.saveDailyStats(current)
        _uiState.update { it.copy(dailyStats = current) }
    }

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