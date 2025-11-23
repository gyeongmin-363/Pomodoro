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

// [수정] 필터 상태 추가
data class StatsUiState(
    val dailyStats: Map<String, DailyStat> = emptyMap(),
    val selectedFilter: String = "전체",
    val filterOptions: List<String> = listOf("전체")
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

            // [수정] DailyStat의 studyTimeByWork 키 값을 추출
            val allWorks = stats.values
                .flatMap { it.studyTimeByWork?.keys ?: emptySet() }
                .distinct()
                .sorted()

            val options = listOf("전체") + allWorks

            _uiState.update {
                it.copy(
                    dailyStats = stats,
                    filterOptions = options
                )
            }
        }
    }

    // [추가] 필터 변경 함수
    fun updateFilter(filter: String) {
        _uiState.update { it.copy(selectedFilter = filter) }
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
                // 데이터 변경 시 필터 옵션도 갱신이 필요할 수 있으나, 여기서는 생략하거나 필요시 로직 추가
                state.copy(dailyStats = newStats)
            }
        }
    }

    // 회고 저장
    fun saveRetrospect(date: String, retrospect: String) {
        updateDailyStat(date) { it.copy(retrospect = retrospect) }
    }

    // 체크리스트 아이템 추가
    fun addChecklistItem(date: String, task: String) {
        if (task.isBlank()) return
        updateDailyStat(date) { current ->
            val newChecklist = current.checklist.toMutableMap()
            if (!newChecklist.containsKey(task)) {
                newChecklist[task] = false
            }
            current.copy(checklist = newChecklist)
        }
    }

    // 체크리스트 아이템 삭제
    fun deleteChecklistItem(date: String, task: String) {
        updateDailyStat(date) { current ->
            val newChecklist = current.checklist.toMutableMap()
            newChecklist.remove(task)
            current.copy(checklist = newChecklist)
        }
    }

    // 체크리스트 아이템 토글
    fun toggleChecklistItem(date: String, task: String) {
        updateDailyStat(date) { current ->
            val newChecklist = current.checklist.toMutableMap()
            val currentState = newChecklist[task] ?: false
            newChecklist[task] = !currentState
            current.copy(checklist = newChecklist)
        }
    }

    // 체크리스트 아이템 수정
    fun modifyChecklistItem(date: String, oldTask: String, newTask: String) {
        if (newTask.isBlank() || oldTask == newTask) return
        updateDailyStat(date) { current ->
            val newChecklist = current.checklist.toMutableMap()
            if (newChecklist.containsKey(oldTask)) {
                val status = newChecklist[oldTask] ?: false
                newChecklist.remove(oldTask)
                newChecklist[newTask] = status
            }
            current.copy(checklist = newChecklist)
        }
    }
}