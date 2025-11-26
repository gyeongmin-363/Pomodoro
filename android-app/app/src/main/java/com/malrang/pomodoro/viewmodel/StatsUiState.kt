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
    val dailyStats: Map<String, DailyStat> = emptyMap(),
    val selectedFilter: String = "전체",
    val filterOptions: List<String> = listOf("전체")
)

class StatsViewModel(
    private val repository: PomodoroRepository,
    // [삭제] supabaseRepository 제거
) : ViewModel() {
    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        loadDailyStats()
    }

    fun loadDailyStats() {
        viewModelScope.launch {
            val stats = repository.loadDailyStats()

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

    fun updateFilter(filter: String) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    // [수정] 자동 동기화 로직 제거 (로컬 저장만 수행)
    private fun updateDailyStat(date: String, transform: (DailyStat) -> DailyStat) {
        viewModelScope.launch {
            val currentStat = _uiState.value.dailyStats[date] ?: DailyStat(date)
            val updatedStat = transform(currentStat)

            // 1. 로컬 저장 (백업을 위해 updatedAt 갱신 필요 시 transform 내부나 여기서 처리)
            repository.saveDailyStat(updatedStat)

            // [삭제] 서버 자동 동기화 로직 제거됨

            // UI 상태 업데이트
            _uiState.update { state ->
                val newStats = state.dailyStats.toMutableMap()
                newStats[date] = updatedStat
                state.copy(dailyStats = newStats)
            }
        }
    }

    // 회고 저장
    fun saveRetrospect(date: String, retrospect: String) {
        updateDailyStat(date) {
            it.copy(retrospect = retrospect, updatedAt = System.currentTimeMillis())
        }
    }

    // 체크리스트 아이템 추가
    fun addChecklistItem(date: String, task: String) {
        if (task.isBlank()) return
        updateDailyStat(date) { current ->
            val newChecklist = current.checklist.toMutableMap()
            if (!newChecklist.containsKey(task)) {
                newChecklist[task] = false
            }
            current.copy(checklist = newChecklist, updatedAt = System.currentTimeMillis())
        }
    }

    // 체크리스트 아이템 삭제
    fun deleteChecklistItem(date: String, task: String) {
        updateDailyStat(date) { current ->
            val newChecklist = current.checklist.toMutableMap()
            newChecklist.remove(task)
            current.copy(checklist = newChecklist, updatedAt = System.currentTimeMillis())
        }
    }

    // 체크리스트 아이템 토글
    fun toggleChecklistItem(date: String, task: String) {
        updateDailyStat(date) { current ->
            val newChecklist = current.checklist.toMutableMap()
            val currentState = newChecklist[task] ?: false
            newChecklist[task] = !currentState
            current.copy(checklist = newChecklist, updatedAt = System.currentTimeMillis())
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
            current.copy(checklist = newChecklist, updatedAt = System.currentTimeMillis())
        }
    }
}