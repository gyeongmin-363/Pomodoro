package com.malrang.pomodoro.ui.screen.stats.month

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.malrang.pomodoro.dataclass.ui.Screen
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

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary)
            .verticalScroll(scrollState)
    ) {
        // 1. 상단 영역 (파란 배경)
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 12.dp)
        ) {
            // 네비게이션 + 필터
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatsFilterDropdown(
                    currentFilter = state.selectedFilter,
                    options = state.filterOptions,
                    onFilterSelected = { statsViewModel.updateFilter(it) }
                )
            }

            Spacer(Modifier.height(8.dp))

            // 캘린더
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
                // [수정] 없는 함수 onDetailRequested 제거. 캘린더에서는 날짜 선택만 담당.
                onDetailRequested = { /* 필요 시 구현, 현재는 하단 시트에서 상세 이동 */ }
            )

            Spacer(Modifier.height(24.dp))

            // [위치 이동] 월간 요약 (아이콘 없는 텍스트 버전)
            MonthlySummaryCard(monthlyTotalMinutes = monthlyTotalMinutes)
        }

        // 2. 하단 영역 (흰색 시트)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = screenHeight / 2),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 일별 상세 정보
                AnimatedVisibility(
                    visible = tappedDate != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    tappedDate?.let { date ->
                        val stats = state.dailyStats[date.toString()]
                        val filteredTime = stats?.getStudyTime(state.selectedFilter) ?: 0

                        // [수정] 불필요한 텍스트 제거하고 카드만 표시
                        // 클릭 시 StatsScreen이 받은 onNavigateToDetail 실행
                        DailySummaryCard(
                            date = date,
                            stats = stats,
                            displayedStudyTime = filteredTime,
                            onDetailClick = { onNavigateToDetail(date) }
                        )
                    }
                }

                // 날짜 미선택 시 안내 (선택 사항)
                if (tappedDate == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "날짜를 눌러 상세 정보를 확인해보세요",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }

                Spacer(Modifier.height(48.dp))
            }
        }
    }
}
