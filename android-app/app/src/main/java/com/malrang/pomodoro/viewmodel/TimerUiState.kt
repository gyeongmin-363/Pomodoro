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
            // ✅ [추가] ViewModel이 생성될 때 저장된 타이머 상태를 불러옵니다.
            val savedState = localRepo.loadTimerState()
            if (savedState != null) {
                // 저장된 상태가 있으면 UI 상태를 즉시 업데이트합니다.
                _uiState.update {
                    it.copy(
                        timeLeft = savedState.timeLeft,
                        currentMode = savedState.currentMode,
                        totalSessions = savedState.totalSessions,
                        isRunning = false // 앱 시작 시에는 항상 '일시정지' 상태입니다.
                    )
                }
            } else {
                // ✅ [수정] 저장된 상태가 없으면, 현재 설정에 맞는 기본값으로 초기화합니다.
                val workPresets = localRepo.loadWorkPresets()
                val currentWorkId = localRepo.loadCurrentWorkId()
                val currentSettings = workPresets.find { it.id == currentWorkId }?.settings
                if (currentSettings != null) {
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
    }

    fun startTimer(settings: Settings) {
        val s = _uiState.value
        _uiState.update { it.copy(isRunning = true, isTimerStartedOnce = true) }
        timerService.start(
            s.timeLeft,
            settings,
            s.currentMode,
            s.totalSessions
        )
    }

    fun pauseTimer() {
        _uiState.update { it.copy(isRunning = false) }
        timerService.pause()
    }

    fun reset(settings: Settings) {
        viewModelScope.launch {
            // ✅ [추가] 리셋 시 저장된 타이머 상태를 삭제합니다.
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
            timerService.resetCompletely(settings)
        }
    }

    fun skipSession() {
        val s = _uiState.value
        timerService.skip(s.currentMode , s.totalSessions)
        // 서비스로부터 상태 업데이트를 받으므로 UI 직접 변경 로직 제거
    }

    fun updateTimerStateFromService(timeLeft: Int, isRunning: Boolean, currentMode: Mode, totalSessions: Int) {
        _uiState.update {
            it.copy(
                timeLeft = timeLeft, isRunning = isRunning,
                currentMode = currentMode, totalSessions = totalSessions
            )
        }
    }

    fun requestTimerStatus() {
        timerService.requestStatus()
    }
}