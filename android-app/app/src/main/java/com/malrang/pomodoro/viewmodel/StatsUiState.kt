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

data class StatsUiState(
    val dailyStats: Map<String, DailyStat> = emptyMap()
)

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

    // [공통] DailyStat 업데이트 헬퍼 함수
    private fun updateDailyStat(date: String, transform: (DailyStat) -> DailyStat) {
        viewModelScope.launch {
            val currentStat = _uiState.value.dailyStats[date] ?: DailyStat(date)
            val updatedStat = transform(currentStat)

            repository.saveDailyStat(updatedStat)

            _uiState.update { state ->
                val newStats = state.dailyStats.toMutableMap()
                newStats[date] = updatedStat
                state.copy(dailyStats = newStats)
            }
        }
    }

    // 회고 저장
    fun saveRetrospect(date: String, retrospect: String) {
        updateDailyStat(date) { it.copy(retrospect = retrospect) }
    }

    // [추가] 체크리스트 아이템 추가
    fun addChecklistItem(date: String, task: String) {
        if (task.isBlank()) return
        updateDailyStat(date) { current ->
            val newChecklist = current.checklist.toMutableMap()
            // 이미 존재하지 않을 때만 추가 (기본값 false)
            if (!newChecklist.containsKey(task)) {
                newChecklist[task] = false
            }
            current.copy(checklist = newChecklist)
        }
    }

    // [추가] 체크리스트 아이템 삭제
    fun deleteChecklistItem(date: String, task: String) {
        updateDailyStat(date) { current ->
            val newChecklist = current.checklist.toMutableMap()
            newChecklist.remove(task)
            current.copy(checklist = newChecklist)
        }
    }

    // [추가] 체크리스트 아이템 토글
    fun toggleChecklistItem(date: String, task: String) {
        updateDailyStat(date) { current ->
            val newChecklist = current.checklist.toMutableMap()
            val currentState = newChecklist[task] ?: false
            newChecklist[task] = !currentState
            current.copy(checklist = newChecklist)
        }
    }

    // [추가] 체크리스트 아이템 수정
    fun modifyChecklistItem(date: String, oldTask: String, newTask: String) {
        if (newTask.isBlank() || oldTask == newTask) return
        updateDailyStat(date) { current ->
            val newChecklist = current.checklist.toMutableMap()
            if (newChecklist.containsKey(oldTask)) {
                val status = newChecklist[oldTask] ?: false
                newChecklist.remove(oldTask)
                // 순서 유지를 위해 LinkedHashMap을 사용하므로 remove 후 put 하면 맨 뒤로 갈 수 있음.
                // 순서가 중요하다면 리스트 구조로 변경을 고려해야 하지만, 현재 Map 구조에서는 새로 추가됨.
                newChecklist[newTask] = status
            }
            current.copy(checklist = newChecklist)
        }
    }
}