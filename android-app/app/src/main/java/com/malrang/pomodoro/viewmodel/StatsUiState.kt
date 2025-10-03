package com.malrang.pomodoro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malrang.pomodoro.dataclass.ui.DailyStat
import com.malrang.pomodoro.localRepo.PomodoroRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

// 통계 화면의 UI 상태를 나타내는 데이터 클래스
data class StatsUiState(
    val dailyStats:  Map<String, DailyStat> = emptyMap()
)

// 통계 관련 로직을 처리하는 ViewModel
class StatsViewModel(private val repository: PomodoroRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        loadDailyStats()
    }

    private fun loadDailyStats() {
        viewModelScope.launch {
            // Repository에서 일일 통계 데이터를 로드합니다.
            // (PomodoroRepository에 loadDailyStats 함수가 구현되어 있다고 가정합니다)
            val stats = repository.loadDailyStats()
            _uiState.update { it.copy(dailyStats = stats) }
        }
    }
}