package com.malrang.pomodoro.viewmodel

import android.Manifest
import android.app.Application
import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresPermission
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.malrang.pomodoro.R
import com.malrang.pomodoro.dataclass.animalInfo.Animal
import com.malrang.pomodoro.dataclass.sprite.AnimalSprite
import com.malrang.pomodoro.dataclass.animalInfo.AnimalsTable
import com.malrang.pomodoro.dataclass.ui.DailyStat
import com.malrang.pomodoro.dataclass.ui.Mode
import com.malrang.pomodoro.localRepo.PomodoroRepository
import com.malrang.pomodoro.dataclass.ui.PomodoroUiState
import com.malrang.pomodoro.dataclass.animalInfo.Rarity
import com.malrang.pomodoro.dataclass.ui.Screen
import com.malrang.pomodoro.dataclass.ui.Settings
import com.malrang.pomodoro.dataclass.sprite.SpriteData
import com.malrang.pomodoro.dataclass.sprite.SpriteMap
import com.malrang.pomodoro.dataclass.sprite.SpriteState
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
        val autoStart = s.settings.autoStart

        if (s.settings.soundEnabled) {
            playSound()
        }
        if (s.settings.vibrationEnabled) {
            vibrate()
        }
        if (s.currentMode == Mode.STUDY) {
            val animal = getRandomAnimal()
            val sprite = makeSprite(animal)

            // **새로운 동물 저장 로직 추가**
            val updatedCollectedAnimals = s.collectedAnimals + animal
            val updatedSeenIds = updatedCollectedAnimals.map { it.id }.toSet()

            viewModelScope.launch {
                repo.saveSeenIds(updatedSeenIds) // DataStore에 저장
            }

            // UI 상태 업데이트
            _uiState.update {
                it.copy(
                    currentMode = Mode.BREAK,
                    timeLeft = it.settings.breakTime * 60,
                    currentScreen = Screen.Main,
                    activeSprites = it.activeSprites + sprite,
                    totalSessions = it.totalSessions + 1,
                    isRunning = autoStart,
                    isPaused = false,
                    collectedAnimals = updatedCollectedAnimals // UI 상태에 추가
                )
            }
        } else {
            // ... 기존 브레이크 세션 종료 로직
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

        if (autoStart) {
            runTimerLoop()
        }
    }

    private fun playSound() {
        val mediaPlayer = MediaPlayer.create(app, R.raw.notification_sound)
        mediaPlayer.start()
        mediaPlayer.setOnCompletionListener { mp ->
            mp.release()
        }
    }

    @RequiresPermission(Manifest.permission.VIBRATE)
    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12(API 31) 이상 → VibratorManager 사용
            val vibratorManager = app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            // 하위 버전 호환
            @Suppress("DEPRECATION")
            app.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        vibrator.vibrate(
            VibrationEffect.createOneShot(
                500,
                VibrationEffect.DEFAULT_AMPLITUDE
            )
        )
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



    private fun makeSprite(animal: Animal): AnimalSprite {
        // SpriteMap에서 animal에 해당하는 SpriteData를 찾습니다.
        val spriteData = SpriteMap.map[animal]
        // 만약 해당 동물의 스프라이트 정보가 없으면 기본값으로 대체합니다.
            ?: SpriteData(
                idleRes = R.drawable.classical_idle,
                jumpRes = R.drawable.classical_jump,
            )

        return AnimalSprite(
            id = UUID.randomUUID().toString(),
            animalId = animal.id, // 이제 Animal enum의 id 속성을 사용합니다.
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