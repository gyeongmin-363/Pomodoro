package com.malrang.pomodoro.ui.screen.stats.month

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.malrang.pomodoro.dataclass.ui.Screen
import com.malrang.pomodoro.viewmodel.StatsViewModel
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun StatsScreen(
    statsViewModel: StatsViewModel,
    onNavigateTo: (Screen) -> Unit,
    onNavigateToDetail: (LocalDate) -> Unit
) {
    val state by statsViewModel.uiState.collectAsState()
    val scrollState = rememberScrollState() // [변경] 전체 화면 스크롤 상태 관리

    // 현재 보고 있는 달과 선택된 날짜
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

    // 화면 높이 가져오기 (하단 시트 최소 높이 설정을 위해)
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary)
            .verticalScroll(scrollState) // [변경] 전체 화면에 스크롤 적용
    ) {
        // 1. 상단 영역 (헤더 + 캘린더)
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 24.dp)
        ) {
            // 네비게이션 및 필터
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onNavigateTo(Screen.Main) }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "뒤로가기",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                StatsFilterDropdown(
                    currentFilter = state.selectedFilter,
                    options = state.filterOptions,
                    onFilterSelected = { statsViewModel.updateFilter(it) }
                )
            }

            Spacer(Modifier.height(16.dp))

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
                onDetailRequested = onNavigateToDetail
            )
        }

        // 2. 하단 영역 (흰색 시트 + 상세 정보)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                // [설정] 내용이 적어도 화면 하단까지 흰색이 꽉 차 보이도록 최소 높이 설정 (선택 사항)
                .heightIn(min = screenHeight / 2),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                // [제거] 내부 verticalScroll 제거 (부모가 스크롤하므로)
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // [제거] 핸들러바(Box) 삭제됨

                // 1) 월간 요약
                MonthlySummaryCard(monthlyTotalMinutes = monthlyTotalMinutes)

                // 2) 일별 상세
                AnimatedVisibility(
                    visible = tappedDate != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    tappedDate?.let { date ->
                        val stats = state.dailyStats[date.toString()]
                        val filteredTime = stats?.getStudyTime(state.selectedFilter) ?: 0

                        Column {
                            Text(
                                text = "선택한 날짜 상세",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            DailySummaryCard(
                                date = date,
                                stats = stats,
                                displayedStudyTime = filteredTime,
                                onDetailClick = { onNavigateToDetail(date) }
                            )
                        }
                    }
                }

                // 하단 여백 (스크롤 끝부분 시야 확보)
                Spacer(Modifier.height(48.dp))
            }
        }
    }
}

@Composable
fun StatsFilterDropdown(
    currentFilter: String,
    options: List<String>,
    onFilterSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var dismissalTimestamp by remember { mutableLongStateOf(0L) }

    BackHandler(enabled = expanded) { expanded = false }

    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.2f))
                .clickable {
                    if (System.currentTimeMillis() - dismissalTimestamp > 200) {
                        expanded = !expanded
                    }
                }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currentFilter,
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
                dismissalTimestamp = System.currentTimeMillis()
            },
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .width(140.dp),
            properties = PopupProperties(focusable = false, dismissOnBackPress = true, dismissOnClickOutside = true)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(option, color = if(option == currentFilter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    },
                    onClick = { onFilterSelected(option); expanded = false }
                )
            }
        }
    }
}