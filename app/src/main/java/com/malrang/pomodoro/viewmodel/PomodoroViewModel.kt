package com.malrang.pomodoro.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.malrang.pomodoro.R
import com.malrang.pomodoro.data.Animal
import com.malrang.pomodoro.data.AnimalSprite
import com.malrang.pomodoro.data.AnimalsTable
import com.malrang.pomodoro.data.DailyStat
import com.malrang.pomodoro.data.Mode
import com.malrang.pomodoro.data.PomodoroRepository
import com.malrang.pomodoro.data.PomodoroUiState
import com.malrang.pomodoro.data.Rarity
import com.malrang.pomodoro.data.Screen
import com.malrang.pomodoro.data.Settings
import com.malrang.pomodoro.data.SpriteData
import com.malrang.pomodoro.data.SpriteMap
import com.malrang.pomodoro.data.SpriteState
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
    private val app: Application
) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(PomodoroUiState())
    val uiState: StateFlow<PomodoroUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    // 초기 로드
    init {
        viewModelScope.launch {
            val seenIds = repo.loadSeenIds() //발견한 동물
            val daily = repo.loadDailyStats() //통계
            val loaded = repo.loadSettings() //설정 정보

            // 도감 채우기: id만 있으니 이름/희귀도는 테이블에서 채움
            val seenAnimals = seenIds.mapNotNull { id -> AnimalsTable.byId(id) }
            _uiState.update { it.copy(collectedAnimals = seenAnimals, dailyStats = daily, settings = loaded) }
        }
    }

    // —— 타이머 제어 ——
    fun startTimer() {
        if (_uiState.value.isRunning) return
        _uiState.update { it.copy(isRunning = true, isPaused = false) }

        timerJob = viewModelScope.launch {
            while (_uiState.value.timeLeft > 0 && _uiState.value.isRunning) {
                delay(1000)
                _uiState.update { s -> s.copy(timeLeft = s.timeLeft - 1) }
            }
            if (_uiState.value.timeLeft <= 0) completeSession()
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
        _uiState.update { it.copy(isRunning = false, isPaused = true) }
    }

    fun resetTimer() {
        timerJob?.cancel()
        val s = _uiState.value
        val base = if (s.currentMode == Mode.STUDY) s.settings.studyTime else s.settings.breakTime
        _uiState.update { it.copy(isRunning = false, isPaused = false, timeLeft = base * 60) }
    }

    private fun completeSession() {
        val s = _uiState.value
        if (s.currentMode == Mode.STUDY) {
            // 공부 세션 끝 → 브레이크 시작
            val animal = getRandomAnimal()
            val sprite = makeSprite(animal.id)

            _uiState.update {
                it.copy(
                    currentMode = Mode.BREAK,
                    timeLeft = it.settings.breakTime * 60,
                    currentScreen = Screen.Main, // AnimalScreen으로 전환하는 대신 MainScreen 유지
                    activeSprites = it.activeSprites + sprite,
                    totalSessions = it.totalSessions + 1,
                    isRunning = it.settings.autoStart,
                    isPaused = false
                )
            }
        } else {
            // 브레이크 끝 → 다음 공부 시작
            _uiState.update {
                it.copy(
                    cycleCount = it.cycleCount + 1,
                    currentMode = Mode.STUDY,
                    timeLeft = it.settings.studyTime * 60,
                    currentScreen = Screen.Main,
                    isRunning = it.settings.autoStart,
                    isPaused = false
                )
            }
        }
    }

    // —— 설정 변경 ——
    fun updateStudyTime(v: Int) {
        //설정 영속성 변경
        updateSettings {
            copy(studyTime = v)
        }

        //시간 ui 변경
        _uiState.update { state ->
            state.copy(
                timeLeft = if (state.currentMode == Mode.STUDY) v * 60 else state.timeLeft
            )
        }
    }
    fun updateBreakTime(v: Int) {
        //설정 영속성 변경
        updateSettings {
            copy(breakTime = v)
        }

        _uiState.update { it.copy(timeLeft = if (it.currentMode == Mode.BREAK) v * 60 else it.timeLeft) }
    }
    fun toggleSound(b: Boolean) = updateSettings{copy(soundEnabled = b)}
    fun toggleVibration(b: Boolean) = updateSettings{ copy(vibrationEnabled = b) }
    fun toggleAutoStart(b: Boolean) = updateSettings{ copy(autoStart = b) }

    /** ✅ Settings 업데이트 함수 */
    fun updateSettings(transform: Settings.() -> Settings) {
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

    // —— 스프라이트 유틸 ——
    private fun makeSprite(animalId: String): AnimalSprite {
        val spriteData = SpriteMap.map[animalId] ?: SpriteData(R.drawable.classical_idle, 7, 1)

        return when (animalId) {
            "cat" -> AnimalSprite(
                id = UUID.randomUUID().toString(),
                animalId = animalId,
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
            else -> TODO()
        }
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

                // Idle 상태일 때 확률적으로 Jump 발동
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



    // —— 랜덤 동물 뽑기 (등장 확률은 고정 분포) ——
    private fun getRandomAnimal(): Animal {
        val roll = kotlin.random.Random.nextInt(100)
        val rarity = when {
            roll < 60 -> Rarity.COMMON
            roll < 85 -> Rarity.RARE
            roll < 97 -> Rarity.EPIC
            else -> Rarity.LEGENDARY
        }
        return AnimalsTable.randomByRarity(rarity)
    }
}