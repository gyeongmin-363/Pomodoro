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

    fun loadDailyStats() {
        viewModelScope.launch {
            val stats = repository.loadDailyStats()
            _uiState.update { it.copy(dailyStats = stats) }
        }
    }

    // [추가] 회고 저장 함수
    fun saveRetrospect(date: String, retrospect: String) {
        viewModelScope.launch {
            val currentStat = _uiState.value.dailyStats[date] ?: DailyStat(date)
            val updatedStat = currentStat.copy(retrospect = retrospect)

            // Repository에 업데이트 요청
            // 주의: Repository에 saveDailyStat(dailyStat: DailyStat) 메서드가 구현되어 있어야 합니다.
            // 예: dao.insertDailyStats(listOf(updatedStat.toEntity()))
            repository.saveDailyStat(updatedStat)

            // UI 상태 즉시 업데이트
            _uiState.update { state ->
                val newStats = state.dailyStats.toMutableMap()
                newStats[date] = updatedStat
                state.copy(dailyStats = newStats)
            }
        }
    }
}