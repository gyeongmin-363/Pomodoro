package com.malrang.pomodoro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malrang.pomodoro.dataclass.ui.Mode
import com.malrang.pomodoro.dataclass.ui.Settings
import com.malrang.pomodoro.localRepo.PomodoroRepository
import com.malrang.pomodoro.service.TimerServiceProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TimerUiState(
    val timeLeft: Int = 0,
    val isRunning: Boolean = false,
    val isTimerStartedOnce: Boolean = false,
    val currentMode: Mode = Mode.STUDY,
    val totalSessions: Int = 0
)

class TimerViewModel(
    private val localRepo: PomodoroRepository,
    private val timerService: TimerServiceProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val savedState = localRepo.loadTimerState()
            if (savedState != null) {
                _uiState.update {
                    it.copy(
                        timeLeft = savedState.timeLeft,
                        currentMode = savedState.currentMode,
                        totalSessions = savedState.totalSessions,
                        isRunning = false
                    )
                }
            } else {
                // [수정] 저장된 상태가 없다면(첫 실행 등), 활성 프리셋의 설정을 가져와 초기화
                val activePreset = localRepo.getActiveWorkPreset()
                val currentSettings = activePreset.settings

                _uiState.update {
                    it.copy(
                        timeLeft = currentSettings.studyTime * 60,
                        currentMode = Mode.STUDY,
                        totalSessions = 0
                    )
                }
            }
        }
    }

    fun startTimer(settings: Settings) {
        val currentState = _uiState.value
        _uiState.update { it.copy(isRunning = true, isTimerStartedOnce = true) }
        timerService.start(
            settings,
            currentState.timeLeft,
            currentState.currentMode.name,
            currentState.totalSessions
        )
    }

    fun pauseTimer() {
        _uiState.update { it.copy(isRunning = false) }
        timerService.pause()
    }

    fun reset(settings: Settings) {
        viewModelScope.launch {
            localRepo.clearTimerState()
            _uiState.update {
                it.copy(
                    timeLeft = settings.studyTime * 60,
                    isRunning = false,
                    isTimerStartedOnce = false,
                    currentMode = Mode.STUDY,
                    totalSessions = 0
                )
            }
            timerService.reset(settings)
        }
    }

    // ✅ [수정] ViewModel은 더 이상 상태를 전달하지 않고, 명령만 보냅니다.
    fun skipSession() {
        timerService.skip()
    }

    fun updateTimerStateFromService(timeLeft: Int, isRunning: Boolean, currentMode: Mode, totalSessions: Int) {
        _uiState.update {
            it.copy(
                timeLeft = timeLeft,
                isRunning = isRunning,
                currentMode = currentMode,
                totalSessions = totalSessions
            )
        }
    }

    fun requestTimerStatus() {
        timerService.requestStatus()
    }
}