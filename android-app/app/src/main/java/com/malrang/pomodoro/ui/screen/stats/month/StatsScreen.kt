package com.malrang.pomodoro.ui.screen.stats.month

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.malrang.pomodoro.dataclass.ui.Screen
import com.malrang.pomodoro.viewmodel.StatsViewModel
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    statsViewModel: StatsViewModel,
    onNavigateTo: (Screen) -> Unit,
    onNavigateToDetail: (LocalDate) -> Unit
) {
    val state by statsViewModel.uiState.collectAsState()
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    // [수정] 필터가 적용된 월간 총 시간 계산
    val monthlyTotalMinutes = remember(selectedDate, state.dailyStats, state.selectedFilter) {
        val targetMonth = YearMonth.from(selectedDate)
        state.dailyStats.values.filter { stat ->
            try {
                val statDate = LocalDate.parse(stat.date)
                YearMonth.from(statDate) == targetMonth
            } catch (e: Exception) {
                false
            }
        }.sumOf { it.getStudyTime(state.selectedFilter) } // 확장 함수 사용
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("통계", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { onNavigateTo(Screen.Main) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // [추가] 필터 드롭다운
            StatsFilterDropdown(
                currentFilter = state.selectedFilter,
                options = state.filterOptions,
                onFilterSelected = { statsViewModel.updateFilter(it) }
            )

            // 월간 요약 카드
            MonthlySummaryCard(monthlyTotalMinutes = monthlyTotalMinutes)

            // 캘린더 섹션 (필터 전달)
            MonthlyStatsCalendar(
                dailyStats = state.dailyStats,
                selectedDate = selectedDate,
                selectedFilter = state.selectedFilter, // 필터 전달
                onDateSelected = { newDate -> selectedDate = newDate },
                onDetailRequested = onNavigateToDetail
            )

            Spacer(Modifier.height(32.dp))
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
    // [수정 1] 메뉴가 닫힌 시점을 기록하기 위한 변수 추가
    var dismissalTimestamp by remember { mutableLongStateOf(0L) }

    // focusable=false일 때는 뒤로가기 버튼 처리를 수동으로 해야 합니다.
    BackHandler(enabled = expanded) {
        expanded = false
    }

    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
        // 버튼 UI
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF212121))
                .clickable {
                    // [수정 2] 방금 닫힌 게 아니라면 토글 수행
                    // onDismissRequest가 실행된 직후(약 100~200ms 이내)에 클릭 이벤트가 들어오면 무시합니다.
                    if (System.currentTimeMillis() - dismissalTimestamp > 200) {
                        expanded = !expanded
                    }
                }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "필터 : $currentFilter",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = "필터 선택",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }

        // 드롭다운 메뉴
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
                // [수정 3] 닫히는 시점 기록
                dismissalTimestamp = System.currentTimeMillis()
            },
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .width(140.dp),
            properties = PopupProperties(
                focusable = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            color = if(option == currentFilter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        onFilterSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}