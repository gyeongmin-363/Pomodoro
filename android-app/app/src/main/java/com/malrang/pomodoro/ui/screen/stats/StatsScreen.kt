package com.malrang.pomodoro.ui.screen.stats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malrang.pomodoro.dataclass.ui.DailyStat
import com.malrang.pomodoro.dataclass.ui.Screen
import com.malrang.pomodoro.viewmodel.StatsViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    statsViewModel: StatsViewModel,
    onNavigateTo: (Screen) -> Unit,
    onNavigateToDetail: (LocalDate) -> Unit
) {
    val state by statsViewModel.uiState.collectAsState()
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    val monthlyTotalMinutes = remember(selectedDate, state.dailyStats) {
        val targetMonth = YearMonth.from(selectedDate)
        state.dailyStats.values.filter { stat ->
            try {
                val statDate = LocalDate.parse(stat.date)
                YearMonth.from(statDate) == targetMonth
            } catch (e: Exception) {
                false
            }
        }.sumOf { it.totalStudyTimeInMinutes }
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
            // 월간 요약 카드
            MonthlySummaryCard(monthlyTotalMinutes = monthlyTotalMinutes)

            // 캘린더 섹션
            MonthlyStatsCalendar(
                dailyStats = state.dailyStats,
                selectedDate = selectedDate,
                onDateSelected = { newDate -> selectedDate = newDate },
                onDetailRequested = onNavigateToDetail
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun MonthlySummaryCard(monthlyTotalMinutes: Int) {
    val hours = monthlyTotalMinutes / 60
    val minutes = monthlyTotalMinutes % 60
    val timeText = if (hours > 0) "${hours}시간 ${minutes}분" else "${minutes}분"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "이번 달 총 집중",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun MonthlyStatsCalendar(
    dailyStats: Map<String, DailyStat>,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDetailRequested: (LocalDate) -> Unit
) {
    var tappedDate by remember { mutableStateOf<LocalDate?>(null) }
    val headerText = "${selectedDate.year}년 ${selectedDate.month.getDisplayName(TextStyle.FULL, Locale.KOREAN)}"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            // 월 이동 헤더
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    onDateSelected(selectedDate.minusMonths(1))
                    tappedDate = null
                }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "이전 달")
                }
                Text(
                    text = headerText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = {
                    onDateSelected(selectedDate.plusMonths(1))
                    tappedDate = null
                }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "다음 달")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // 요일 헤더
            val daysOfWeek = listOf("일", "월", "화", "수", "목", "금", "토")
            Row(Modifier.fillMaxWidth()) {
                daysOfWeek.forEach { day ->
                    val color = when (day) {
                        "토" -> Color(0xFF42A5F5) // Material Blue 400
                        "일" -> Color(0xFFEF5350) // Material Red 400
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        color = color,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // 달력 그리드
            MonthlyCalendarGrid(
                selectedDate = selectedDate,
                tappedDate = tappedDate,
                onDateTap = { date -> tappedDate = date },
                onDateLongTap = { date -> onDetailRequested(date) },
                getStudyTime = { date ->
                    dailyStats[date.toString()]?.totalStudyTimeInMinutes ?: 0
                }
            )
        }
    }

    Spacer(Modifier.height(16.dp))

    // 날짜 선택 시 하단 요약 정보 (애니메이션)
    AnimatedVisibility(
        visible = tappedDate != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        tappedDate?.let { date ->
            val stats = dailyStats[date.toString()]
            DailySummaryCard(
                date = date,
                stats = stats,
                onDetailClick = { onDetailRequested(date) }
            )
        }
    }
}

@Composable
fun DailySummaryCard(
    date: LocalDate,
    stats: DailyStat?,
    onDetailClick: () -> Unit
) {
    val studyTime = stats?.totalStudyTimeInMinutes ?: 0
    val checklistTotal = stats?.checklist?.size ?: 0
    val checklistDone = stats?.checklist?.values?.count { it } ?: 0
    val retrospect = stats?.retrospect ?: "작성된 회고가 없습니다."

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onDetailClick), // 카드 전체 클릭 가능
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${date.monthValue}월 ${date.dayOfMonth}일",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                FilledTonalButton(
                    onClick = onDetailClick,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("상세보기")
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("총 공부 시간", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${studyTime}분", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("체크리스트", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$checklistDone / $checklistTotal", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = retrospect,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}