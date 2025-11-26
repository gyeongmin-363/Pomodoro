package com.malrang.pomodoro.ui.screen.stats.month

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.malrang.pomodoro.viewmodel.StatsViewModel
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun StatsScreen(
    statsViewModel: StatsViewModel,
    onNavigateToDetail: (LocalDate) -> Unit
) {
    val state by statsViewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    var currentMonthDate by remember { mutableStateOf(LocalDate.now()) }
    var tappedDate by remember { mutableStateOf<LocalDate?>(null) }

    // 월간 총 시간 계산
    val monthlyTotalMinutes = remember(currentMonthDate, state.dailyStats, state.selectedFilter) {
        val targetMonth = YearMonth.from(currentMonthDate)
        state.dailyStats.values.filter { stat ->
            try {
                val statDate = LocalDate.parse(stat.date)
                YearMonth.from(statDate) == targetMonth
            } catch (e: Exception) {
                false
            }
        }.sumOf { it.getStudyTime(state.selectedFilter) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // NeoBackground
            .verticalScroll(scrollState)
            .padding(24.dp)
    ) {
        // 1. 헤더 영역 (타이틀 + 필터)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 타이틀 배지
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(8.dp))
                    .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "통계 기록",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSecondary
                )
            }

            // 필터 드롭다운
            StatsFilterDropdown(
                currentFilter = state.selectedFilter,
                options = state.filterOptions,
                onFilterSelected = { statsViewModel.updateFilter(it) }
            )
        }

        Spacer(Modifier.height(24.dp))

        // 2. 월간 요약 카드 (배너 스타일)
        MonthlySummaryCard(monthlyTotalMinutes = monthlyTotalMinutes)

        Spacer(Modifier.height(24.dp))

        // 3. 캘린더 영역
        MonthlyStatsCalendar(
            dailyStats = state.dailyStats,
            currentMonthDate = currentMonthDate,
            selectedDate = tappedDate,
            selectedFilter = state.selectedFilter,
            onMonthChanged = { newDate ->
                currentMonthDate = newDate
                tappedDate = null
            },
            onDateSelected = { date ->
                tappedDate = if (tappedDate == date) null else date
            },
            onDetailRequested = { /* Long click action if needed */ }
        )

        Spacer(Modifier.height(32.dp))

        // 4. 하단 상세 정보 (애니메이션)
        AnimatedVisibility(
            visible = tappedDate != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            tappedDate?.let { date ->
                val stats = state.dailyStats[date.toString()]
                val filteredTime = stats?.getStudyTime(state.selectedFilter) ?: 0

                DailySummaryCard(
                    date = date,
                    stats = stats,
                    displayedStudyTime = filteredTime,
                    onDetailClick = { onNavigateToDetail(date) }
                )
            }
        }

        // 날짜 미선택 시 안내 메시지
        if (tappedDate == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "날짜를 눌러 상세 정보를 확인해보세요",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }

        Spacer(Modifier.height(48.dp))
    }
}