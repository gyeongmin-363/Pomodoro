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
import com.malrang.pomodoro.service.TimerServiceProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private val timerService: TimerServiceProvider // app 대신 TimerServiceProvider 주입
) : ViewModel() {

    private val _uiState = MutableStateFlow(PomodoroUiState())
    val uiState: StateFlow<PomodoroUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    // 초기 로드
    init {
        viewModelScope.launch {
            val seenIds = repo.loadSeenIds() // 발견한 동물 ID들을 불러옵니다.
            val daily = repo.loadDailyStats() // 통계 로드
            val loaded = repo.loadSettings() // 설정 정보 로드

            // 도감 채우기: id를 기반으로 AnimalsTable에서 Animal 객체로 변환
            val seenAnimals = seenIds.mapNotNull { id -> AnimalsTable.byId(id) }

            _uiState.update {
                it.copy(
                    collectedAnimals = seenAnimals.toSet(), // 불러온 동물 목록으로 UI 상태를 업데이트
                    dailyStats = daily,
                    settings = loaded,
                )
            }

            resetTimer() //timeLeft를 계산하기 위함
        }
    }

    // —— 타이머 제어 ——
    private fun runTimerLoop() {
        timerJob = viewModelScope.launch {
            while (_uiState.value.timeLeft > 0 && _uiState.value.isRunning) {
                delay(1000)
                _uiState.update { s -> s.copy(timeLeft = s.timeLeft - 1) }
            }
            if (_uiState.value.timeLeft <= 0) {
                completeSession()
            }
        }
    }

    fun startTimer() {
        if (_uiState.value.isRunning) return
        _uiState.update { it.copy(isRunning = true, isPaused = false) }
        runTimerLoop()

        // 서비스 시작
        timerService.start(_uiState.value.timeLeft)
    }

    fun pauseTimer() {
        timerJob?.cancel()
        _uiState.update { it.copy(isRunning = false, isPaused = true) }
        // 서비스 일시정지
        timerService.pause()
    }

    fun resetTimer() {
        timerJob?.cancel()
        val s = _uiState.value
        val newTimeLeft = when (s.currentMode) {
            Mode.STUDY -> s.settings.studyTime
            Mode.SHORT_BREAK -> s.settings.shortBreakTime
            Mode.LONG_BREAK -> s.settings.longBreakTime
        }
        _uiState.update { it.copy(isRunning = false, isPaused = false, timeLeft = newTimeLeft * 60) }

        // 서비스 리셋
        timerService.reset(newTimeLeft * 60)
    }

    private fun completeSession() {
        val s = _uiState.value
        val autoStart = s.settings.autoStart

        if (s.settings.soundEnabled) {
            soundPlayer.playSound()
        }
        if (s.settings.vibrationEnabled) {
            vibratorHelper.vibrate()
        }

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
                    isRunning = autoStart,
                    isPaused = false,
                    collectedAnimals = updatedCollectedAnimals
                )
            }
        } else { // SHORT_BREAK or LONG_BREAK
            _uiState.update {
                it.copy(
                    cycleCount = it.cycleCount + 1,
                    currentMode = Mode.STUDY,
                    timeLeft = it.settings.studyTime * 60,
                    currentScreen = Screen.Main,
                    isRunning = autoStart,
                    isPaused = false
                )
            }
        }

        // 세션 완료 후 서비스 리셋
        resetTimer()

        if (autoStart) {
            startTimer()
        }
    }


    // —— 설정 변경 ——
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

    /** ✅ Settings 업데이트 함수 */
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

    // —— 통계 업데이트 ——
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