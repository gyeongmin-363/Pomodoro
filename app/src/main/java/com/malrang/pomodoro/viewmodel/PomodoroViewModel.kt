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
import com.malrang.pomodoro.data.SpriteData
import com.malrang.pomodoro.data.SpriteMap
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
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
            val seenIds = repo.loadSeenIds()
            val daily = repo.loadDailyStats()

            // 도감 채우기: id만 있으니 이름/희귀도는 테이블에서 채움
            val seenAnimals = seenIds.mapNotNull { id -> AnimalsTable.byId(id) }
            _uiState.update { it.copy(collectedAnimals = seenAnimals, dailyStats = daily) }
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
            // 공부 세션 완료 → 일별 통계 +1, 브레이크 시작 + 스프라이트 추가 + 도감 저장
            viewModelScope.launch { incTodayStat() }
            val animal = getRandomAnimal()

            // 도감 업데이트(영구)
            viewModelScope.launch {
                val curr = repo.loadSeenIds().toMutableSet()
                if (!curr.contains(animal.id)) {
                    curr.add(animal.id)
                    repo.saveSeenIds(curr)
                }
                // 메모리 도감 동기화
                val merged = (curr.mapNotNull { AnimalsTable.byId(it) }).distinctBy { it.id }
                _uiState.update { it.copy(collectedAnimals = merged) }
            }

            // 스프라이트 추가(세션 메모리 전용)
            val sprite = makeSprite(animal.id)
            _uiState.update {
                it.copy(
                    currentMode = Mode.BREAK,
                    timeLeft = it.settings.breakTime * 60,
                    currentScreen = Screen.Animal,      // 동물 화면
                    activeSprites = it.activeSprites + sprite,
                    totalSessions = it.totalSessions + 1
                )
            }
        } else {
            // 브레이크 끝 → 다음 공부
            _uiState.update {
                it.copy(
                    cycleCount = it.cycleCount + 1,
                    currentMode = Mode.STUDY,
                    timeLeft = it.settings.studyTime * 60,
                    currentScreen = Screen.Main
                )
            }
        }
    }

    // —— 설정 변경 ——
    fun updateStudyTime(v: Int) {
        _uiState.update { it.copy(settings = it.settings.copy(studyTime = v), timeLeft = if (it.currentMode == Mode.STUDY) v * 60 else it.timeLeft) }
    }
    fun updateBreakTime(v: Int) {
        _uiState.update { it.copy(settings = it.settings.copy(breakTime = v), timeLeft = if (it.currentMode == Mode.BREAK) v * 60 else it.timeLeft) }
    }
    fun toggleSound(b: Boolean) { _uiState.update { it.copy(settings = it.settings.copy(soundEnabled = b)) } }
    fun toggleVibration(b: Boolean) { _uiState.update { it.copy(settings = it.settings.copy(vibrationEnabled = b)) } }

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
        val spriteData = SpriteMap.map[animalId] ?: SpriteData(R.drawable.idle_catttt, 7, 1)


        val rnd = Random(System.currentTimeMillis())
        return AnimalSprite(
            animalId = animalId,
            sheetRes = spriteData.sheetRes,
            frameCols = spriteData.cols,
            frameRows = spriteData.rows,
            frameDurationMs = 120L,
            x = rnd.nextInt(0, 600).toFloat(),
            y = rnd.nextInt(0, 1000).toFloat(),
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
                sp.copy(x = nx, y = ny, vx = vx, vy = vy)
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

