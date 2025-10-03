package com.malrang.pomodoro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malrang.pomodoro.dataclass.ui.Mode
import com.malrang.pomodoro.dataclass.ui.Settings
import com.malrang.pomodoro.localRepo.PomodoroRepository
import com.malrang.pomodoro.service.TimerService
import com.malrang.pomodoro.service.TimerServiceProvider
import kotlinx.coroutines.delay
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
    private var isResetting = false

    init {
        viewModelScope.launch {
            // 참고: 타이머 상태를 올바르게 초기화하기 위해 여기에서 설정을 로드합니다.
            // 이로 인해 SettingsViewModel과 일부 로직이 중복될 수 있지만, 분리된 뷰모델의 자가 초기화를 위해 필요합니다.
            val presets = localRepo.loadWorkPresets()
            val currentWorkId = localRepo.loadCurrentWorkId() ?: presets.firstOrNull()?.id
            val currentSettings = presets.find { it.id == currentWorkId }?.settings ?: Settings()

            if (TimerService.isServiceActive()) {
                timerService.requestStatus()
            } else {
                val savedState = localRepo.loadTimerState()
                if (savedState != null) {
                    _uiState.update {
                        it.copy(
                            timeLeft = savedState.timeLeft,
                            currentMode = savedState.currentMode,
                            totalSessions = savedState.totalSessions,
                            isRunning = false,
                            // 수정된 부분: 로드한 currentSettings를 사용하여 올바르게 계산합니다.
                            isTimerStartedOnce = savedState.timeLeft < (currentSettings.studyTime * 60)
                        )
                    }
                } else {
                    // 저장된 상태가 없을 경우, 현재 설정으로 타이머를 초기화합니다.
                    _uiState.update {
                        it.copy(timeLeft = currentSettings.studyTime * 60)
                    }
                }
            }
        }
    }

    fun startTimer(settings: Settings) {
        if (_uiState.value.isRunning) return
        val s = _uiState.value

        val timeForCurrentMode = when (s.currentMode) {
            Mode.STUDY -> settings.studyTime * 60
            Mode.SHORT_BREAK -> settings.shortBreakTime * 60
            Mode.LONG_BREAK -> settings.longBreakTime * 60
        }

        val newTimeLeft = if (!s.isTimerStartedOnce) {
            timeForCurrentMode
        } else {
            s.timeLeft
        }

        _uiState.update { it.copy(isRunning = true, isTimerStartedOnce = true, timeLeft = newTimeLeft) }
        timerService.start(newTimeLeft, settings, s.currentMode, s.totalSessions)
    }

    fun pauseTimer() {
        _uiState.update { it.copy(isRunning = false) }
        timerService.pause()
    }

    fun reset(settings: Settings) {
        viewModelScope.launch {
            isResetting = true
            localRepo.clearTimerState()
            _uiState.update {
                it.copy(
                    timeLeft = settings.studyTime * 60,
                    totalSessions = 0,
                    isRunning = false,
                    isTimerStartedOnce = false,
                    currentMode = Mode.STUDY,
                )
            }
            timerService.resetCompletely(settings)
            delay(200) // 서비스가 리셋되고 브로드캐스트할 시간을 줍니다.
            isResetting = false
        }
    }

    fun skipSession(settings: Settings) {
        val s = _uiState.value
        val nextMode: Mode
        val nextTime: Int
        var newTotalSessions = s.totalSessions
        if (s.currentMode == Mode.STUDY) {
            newTotalSessions++
            val isLongBreakTime = newTotalSessions > 0 && newTotalSessions % settings.longBreakInterval == 0
            nextMode = if (isLongBreakTime) Mode.LONG_BREAK else Mode.SHORT_BREAK
            nextTime = if (isLongBreakTime) settings.longBreakTime else settings.shortBreakTime
        } else {
            nextMode = Mode.STUDY
            nextTime = settings.studyTime
        }
        _uiState.update {
            it.copy(
                currentMode = nextMode, totalSessions = newTotalSessions, timeLeft = nextTime * 60,
                isRunning = false
            )
        }
        timerService.skip(s.currentMode, newTotalSessions)
    }

    fun updateTimerStateFromService(timeLeft: Int, isRunning: Boolean, currentMode: Mode, totalSessions: Int) {
        if (isResetting) return // 리셋 중에는 서비스로부터 오는 업데이트를 무시합니다.
        _uiState.update {
            it.copy(
                timeLeft = timeLeft, isRunning = isRunning,
                currentMode = currentMode, totalSessions = totalSessions
            )
        }
    }

    fun requestTimerStatus() {
        if (TimerService.isServiceActive()) {
            timerService.requestStatus()
        }
    }
}