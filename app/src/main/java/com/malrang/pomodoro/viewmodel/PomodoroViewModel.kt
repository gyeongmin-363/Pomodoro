package com.malrang.pomodoro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malrang.pomodoro.data.Animal
import com.malrang.pomodoro.data.Mode
import com.malrang.pomodoro.data.PomodoroUiState
import com.malrang.pomodoro.data.Rarity
import com.malrang.pomodoro.data.Screen
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Pomodoro 앱의 UI 상태를 관리하고 비즈니스 로직을 처리하는 ViewModel입니다.
 */
class PomodoroViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PomodoroUiState())
    val uiState: StateFlow<PomodoroUiState> = _uiState

    private var timerJob: Job? = null

    /**
     * 타이머를 시작합니다. 이미 실행 중인 경우 아무 작업도 수행하지 않습니다.
     */
    fun startTimer() {
        if (_uiState.value.isRunning) return
        _uiState.update { it.copy(isRunning = true, isPaused = false) }

        timerJob = viewModelScope.launch {
            while (_uiState.value.timeLeft > 0 && _uiState.value.isRunning) {
                delay(1000)
                _uiState.update { it.copy(timeLeft = it.timeLeft - 1) }
            }
            if (_uiState.value.timeLeft <= 0) completeSession()
        }
    }

    /**
     * 타이머를 일시정지합니다.
     */
    fun pauseTimer() {
        timerJob?.cancel()
        _uiState.update { it.copy(isRunning = false, isPaused = true) }
    }

    /**
     * 현재 모드에 맞게 타이머를 리셋합니다.
     */
    fun resetTimer() {
        timerJob?.cancel()
        val s = _uiState.value
        val newTime = if (s.currentMode == Mode.STUDY) s.settings.studyTime else s.settings.breakTime
        _uiState.update { it.copy(isRunning = false, isPaused = false, timeLeft = newTime * 60) }
    }

    /**
     * 현재 세션을 완료하고 다음 모드로 전환합니다.
     * 공부 세션이 끝나면 동물을 획득하고 휴식 모드로, 휴식 세션이 끝나면 다음 사이클의 공부 모드로 전환합니다.
     */
    private fun completeSession() {
        val s = _uiState.value
        if (s.currentMode == Mode.STUDY) {
            // 공부 끝나면 → 무조건 동물 화면
            val newAnimal = getRandomAnimal()
            _uiState.update {
                it.copy(
                    totalSessions = s.totalSessions + 1,
                    currentMode = Mode.BREAK,
                    timeLeft = s.settings.breakTime * 60,
                    currentScreen = Screen.Animal,
                    collectedAnimals = if (newAnimal != null && it.collectedAnimals.none { a -> a.id == newAnimal.id }) {
                        it.collectedAnimals + newAnimal
                    } else it.collectedAnimals
                )
            }
        } else {
            // 휴식 끝나면 → 다음 공부 시작
            _uiState.update {
                it.copy(
                    cycleCount = s.cycleCount + 1,
                    currentMode = Mode.STUDY,
                    timeLeft = s.settings.studyTime * 60,
                    currentScreen = Screen.Main
                )
            }
        }
    }

    private fun getRandomAnimal(): Animal {
        val rarityRoll = Random.nextInt(100)
        val rarity = when {
            rarityRoll < 60 -> Rarity.COMMON
            rarityRoll < 85 -> Rarity.RARE
            rarityRoll < 97 -> Rarity.EPIC
            else -> Rarity.LEGENDARY
        }

        val pool = when (rarity) {
            Rarity.COMMON -> listOf(
                Animal("cat", "고양이", "🐱", rarity),
                Animal("dog", "강아지", "🐶", rarity),
                Animal("rabbit", "토끼", "🐰", rarity),
                Animal("hamster", "햄스터", "🐹", rarity)
            )
            Rarity.RARE -> listOf(
                Animal("panda", "팬더", "🐼", rarity),
                Animal("koala", "코알라", "🐨", rarity),
                Animal("penguin", "펭귄", "🐧", rarity),
                Animal("fox", "여우", "🦊", rarity)
            )
            Rarity.EPIC -> listOf(
                Animal("lion", "사자", "🦁", rarity),
                Animal("tiger", "호랑이", "🐅", rarity),
                Animal("wolf", "늑대", "🐺", rarity),
                Animal("eagle", "독수리", "🦅", rarity)
            )
            Rarity.LEGENDARY -> listOf(
                Animal("unicorn", "유니콘", "🦄", rarity),
                Animal("dragon", "드래곤", "🐉", rarity),
                Animal("phoenix", "피닉스", "🔥🐦", rarity),
                Animal("griffin", "그리핀", "🦅🦁", rarity)
            )
        }

        return pool.random()
    }



    /**
     * 지정된 화면으로 UI를 전환합니다.
     * @param screen 표시할 화면입니다.
     */
    fun showScreen(screen: Screen) {
        _uiState.update { it.copy(currentScreen = screen) }
    }

    /**
     * 공부 시간을 업데이트합니다.
     * @param value 새로운 공부 시간(분)입니다.
     */
    fun updateStudyTime(value: Int) {
        _uiState.update { it.copy(settings = it.settings.copy(studyTime = value), timeLeft = value * 60) }
    }

    /**
     * 휴식 시간을 업데이트합니다.
     * @param value 새로운 휴식 시간(분)입니다.
     */
    fun updateBreakTime(value: Int) {
        _uiState.update { it.copy(settings = it.settings.copy(breakTime = value)) }
    }

    /**
     * 알림음 사용 여부를 토글합니다.
     * @param enabled 알림음 사용 여부입니다.
     */
    fun toggleSound(enabled: Boolean) {
        _uiState.update { it.copy(settings = it.settings.copy(soundEnabled = enabled)) }
    }

    /**
     * 진동 사용 여부를 토글합니다.
     * @param enabled 진동 사용 여부입니다.
     */
    fun toggleVibration(enabled: Boolean) {
        _uiState.update { it.copy(settings = it.settings.copy(vibrationEnabled = enabled)) }
    }
}
