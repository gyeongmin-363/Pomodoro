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

    fun updateTimerStateFromService(timeLeft: Int, isRunning: Boolean) {
        _uiState.update {
            it.copy(
                timeLeft = timeLeft,
                isRunning = isRunning,
                isPaused = !isRunning && timeLeft > 0
            )
        }
    }

    fun onTimerFinishedFromService() {
        completeSession()
    }

    fun startTimer() {
        if (_uiState.value.isRunning) return
        val s = _uiState.value
        _uiState.update { it.copy(isRunning = true, isPaused = false) }
        // --- 변경: 서비스 시작 시 현재 상태(설정, 모드, 세션 수) 전달 ---
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
     * 세션이 완료되었을 때 호출 (앱이 활성 상태일 때)
     * 이제 이 함수는 UI 업데이트와 데이터 저장만 담당합니다.
     * 소리, 진동, 자동 시작은 서비스가 처리합니다.
     */
    private fun completeSession() {
        val s = _uiState.value

        // --- 변경: 소리, 진동 로직은 서비스에서 처리하므로 ViewModel에서는 제거 ---

        if (s.currentMode == Mode.STUDY) {
            val newTotalSessions = s.totalSessions + 1
            val isLongBreakTime = newTotalSessions > 0 && newTotalSessions % s.settings.longBreakInterval == 0
            val nextMode = if (isLongBreakTime) Mode.LONG_BREAK else Mode.SHORT_BREAK
            val nextTime = if (isLongBreakTime) s.settings.longBreakTime else s.settings.shortBreakTime
            val animal = getRandomAnimal()
            val sprite = makeSprite(animal)
            val updatedCollectedAnimals = s.collectedAnimals + animal
            val updatedSeenIds = updatedCollectedAnimals.map { it.id }.toSet()
            viewModelScope.launch {
                repo.saveSeenIds(updatedSeenIds)
            }
            _uiState.update {
                it.copy(
                    currentMode = nextMode,
                    timeLeft = nextTime * 60,
                    currentScreen = Screen.Main,
                    activeSprites = it.activeSprites + sprite,
                    totalSessions = newTotalSessions,
                    isPaused = false, // isRunning 상태는 서비스의 브로드캐스트를 통해 업데이트됨
                    collectedAnimals = updatedCollectedAnimals
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    cycleCount = it.cycleCount + 1,
                    currentMode = Mode.STUDY,
                    timeLeft = it.settings.studyTime * 60,
                    currentScreen = Screen.Main,
                    isPaused = false
                )
            }
        }

        // --- 변경: 타이머 재시작 로직은 서비스가 담당하므로 ViewModel에서는 제거 ---
    }


    fun updateStudyTime(v: Int) {
        updateSettings { copy(studyTime = v) }
        _uiState.update { state ->
            if (state.currentMode == Mode.STUDY) {
                state.copy(timeLeft = v * 60)
            } else {
                state
            }
        }
    }
    fun updateShortBreakTime(v: Int) {
        updateSettings { copy(shortBreakTime = v) }
        _uiState.update { state ->
            if (state.currentMode == Mode.SHORT_BREAK) {
                state.copy(timeLeft = v * 60)
            } else {
                state
            }
        }
    }

    fun updateLongBreakTime(v: Int) {
        updateSettings { copy(longBreakTime = v) }
        _uiState.update { state ->
            if (state.currentMode == Mode.LONG_BREAK) {
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