package com.malrang.pomodoro.ui.screen

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malrang.pomodoro.dataclass.ui.DailyStat
import com.malrang.pomodoro.dataclass.ui.Screen
import com.malrang.pomodoro.viewmodel.PomodoroViewModel
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.extensions.format
import ir.ehsannarmani.compose_charts.models.AnimationMode
import ir.ehsannarmani.compose_charts.models.DotProperties
import ir.ehsannarmani.compose_charts.models.DrawStyle
import ir.ehsannarmani.compose_charts.models.GridProperties
import ir.ehsannarmani.compose_charts.models.HorizontalIndicatorProperties
import ir.ehsannarmani.compose_charts.models.IndicatorCount
import ir.ehsannarmani.compose_charts.models.LabelProperties
import ir.ehsannarmani.compose_charts.models.Line
import ir.ehsannarmani.compose_charts.models.PopupProperties
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun StatsScreen(vm: PomodoroViewModel) {
    val state by vm.uiState.collectAsState()

    var isCalendarExpanded by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1B4B))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📊 통계", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            IconButton(onClick = { vm.showScreen(Screen.Main) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "돌아가기",
                    tint = Color.White
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        ExpandableCalendarView(
            dailyStats = state.dailyStats,
            isExpanded = isCalendarExpanded,
            onToggle = { isCalendarExpanded = !isCalendarExpanded },
            selectedDate = selectedDate,
            onDateSelected = { newDate -> selectedDate = newDate }
        )

        AnimatedVisibility(visible = !isCalendarExpanded) {
            Column {
                Spacer(Modifier.height(24.dp))
                WeeklyTimeChart(
                    dailyStats = state.dailyStats,
                    displayDate = selectedDate
                )
            }
        }
    }
}

@Composable
fun ExpandableCalendarView(
    dailyStats: Map<String, DailyStat>,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    var tappedDate by remember { mutableStateOf<LocalDate?>(null) }

    LaunchedEffect(isExpanded) {
        if (!isExpanded) {
            tappedDate = null
        }
    }

    val headerText = if (isExpanded) {
        "${selectedDate.year}년 ${selectedDate.month.getDisplayName(TextStyle.FULL, Locale.KOREAN)}"
    } else {
        val weekOfMonth = selectedDate.get(WeekFields.of(Locale.KOREAN).weekOfMonth())
        "${selectedDate.year}년 ${selectedDate.monthValue}월 ${weekOfMonth}째주"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2A64))
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    val newDate = if (isExpanded) selectedDate.minusMonths(1) else selectedDate.minusWeeks(1)
                    onDateSelected(newDate)
                }) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "이전", tint = Color.White)
                }
                Text(
                    text = headerText,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                IconButton(onClick = {
                    val newDate = if (isExpanded) selectedDate.plusMonths(1) else selectedDate.plusWeeks(1)
                    onDateSelected(newDate)
                }) {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "다음", tint = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            val daysOfWeek = if (isExpanded) {
                // 달력이 펼쳐진 상태(월간)일 때는 '일'부터 시작
                listOf("일", "월", "화", "수", "목", "금", "토")
            } else {
                // 달력이 접힌 상태(주간)일 때는 '월'부터 시작
                listOf("월", "화", "수", "목", "금", "토", "일")
            }
            Row(Modifier.fillMaxWidth()) {
                daysOfWeek.forEach { day ->
                    val color = when (day) {
                        "토" -> Color(0xFF64B5F6)
                        "일" -> Color(0xFFE57373)
                        else -> Color.White.copy(alpha = 0.7f)
                    }
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        color = color,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (isExpanded) {
                MonthlyCalendarGrid(
                    selectedDate = selectedDate,
                    dailyStats = dailyStats,
                    tappedDate = tappedDate,
                    onDateTap = { date -> tappedDate = date }
                )
            } else {
                WeeklyCalendarGrid(selectedDate = selectedDate, dailyStats = dailyStats)
            }

            // --- ▼▼▼ 수정된 부분: Work별 기록 표시 UI ▼▼▼ ---
            AnimatedVisibility(visible = isExpanded && tappedDate != null) {
                val stats = tappedDate?.let { dailyStats[it.toString()] }

                val allWorkNames = (stats?.studyTimeByWork?.keys ?: emptySet()) +
                        (stats?.breakTimeByWork?.keys ?: emptySet())

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${tappedDate?.monthValue}월 ${tappedDate?.dayOfMonth}일 기록",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (allWorkNames.isEmpty()) {
                        Text(
                            text = "이날의 기록이 없습니다.",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    } else {
                        allWorkNames.forEach { workName ->
                            val studyMinutes = stats?.studyTimeByWork?.getOrDefault(workName, 0) ?: 0
                            val breakMinutes = stats?.breakTimeByWork?.getOrDefault(workName, 0) ?: 0

                            if (studyMinutes > 0 || breakMinutes > 0) {
                                Text(
                                    text = "📌 $workName",
                                    color = Color(0xFFFBBF24),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                )
                                Text(
                                    text = "  - 📚 공부: ${studyMinutes}분, ☕ 휴식: ${breakMinutes}분",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 6.dp)
                                )
                            }
                        }

                        Divider(color = Color.White.copy(alpha = 0.2f), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                        Text(
                            text = "총 공부: ${stats?.totalStudyTimeInMinutes ?: 0}분",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "총 휴식: ${stats?.totalBreakTimeInMinutes ?: 0}분",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            // --- ▲▲▲ 수정된 부분 ---

            Divider(color = Color.White.copy(alpha = 0.2f), thickness = 1.dp, modifier = Modifier.padding(top = 8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "접기" else "펼치기",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun WeeklyCalendarGrid(selectedDate: LocalDate, dailyStats: Map<String, DailyStat>) {
    val today = LocalDate.now()
    // 주의 시작을 일요일로 변경
    val firstDayOfWeek = selectedDate.with(DayOfWeek.MONDAY)

    Row(modifier = Modifier.fillMaxWidth()) {
        (0..6).forEach { i ->
            val date = firstDayOfWeek.plusDays(i.toLong())
            // DailyStat 구조 변경에 따라 totalStudyTimeInMinutes 사용
            val hasRecord = (dailyStats[date.toString()]?.totalStudyTimeInMinutes ?: 0) > 0
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                DayCell(
                    date = date,
                    hasRecord = hasRecord,
                    isToday = date == today,
                    isSelected = false,
                    onClick = { }
                )
            }
        }
    }
}

@Composable
private fun MonthlyCalendarGrid(
    selectedDate: LocalDate,
    dailyStats: Map<String, DailyStat>,
    tappedDate: LocalDate?,
    onDateTap: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    val currentMonth = YearMonth.from(selectedDate)

    val firstDayOfMonth = currentMonth.atDay(1)
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7
    val daysInMonth = currentMonth.lengthOfMonth()
    val calendarDays = (0 until firstDayOfWeek).map<Int?, LocalDate?> { null } + (1..daysInMonth).map { firstDayOfMonth.withDayOfMonth(it) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        userScrollEnabled = false,
        modifier = Modifier.height(240.dp)
    ) {
        items(calendarDays.size) { index ->
            val date = calendarDays[index]
            if (date != null) {
                // DailyStat 구조 변경에 따라 totalStudyTimeInMinutes 사용
                val hasRecord = (dailyStats[date.toString()]?.totalStudyTimeInMinutes ?: 0) > 0
                DayCell(
                    date = date,
                    hasRecord = hasRecord,
                    isToday = date == today,
                    isSelected = date == tappedDate,
                    onClick = { onDateTap(date) }
                )
            } else {
                Spacer(modifier = Modifier.size(40.dp))
            }
        }
    }
}

@Composable
fun DayCell(
    date: LocalDate,
    hasRecord: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val dayColor = when (date.dayOfWeek) {
        DayOfWeek.SATURDAY -> Color(0xFF64B5F6)
        DayOfWeek.SUNDAY -> Color(0xFFE57373)
        else -> Color.White
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .padding(4.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.3f), CircleShape)
            )
        } else if (isToday) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
            )
        }

        if (hasRecord) {
            Text(
                text = "🐾",
                fontSize = 28.sp,
                color = Color(0xFFFBBF24).copy(alpha = 0.6f),
            )
        }
        Text(
            text = date.dayOfMonth.toString(),
            color = dayColor,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun WeeklyTimeChart(dailyStats: Map<String, DailyStat>, displayDate: LocalDate) {
    val weekLabels = listOf("월", "화", "수", "목", "금", "토", "일")
    // 주의 시작을 일요일로 변경
    val firstDayOfWeek = displayDate.with(DayOfWeek.MONDAY)

    val weeklyData = (0..6).map { i ->
        val date = firstDayOfWeek.plusDays(i.toLong())
        dailyStats[date.toString()] ?: DailyStat(date.toString())
    }

    // DailyStat 구조 변경에 따라 total... 속성 사용
    val studyData = weeklyData.map { it.totalStudyTimeInMinutes.toDouble() }
    val breakData = weeklyData.map { it.totalBreakTimeInMinutes.toDouble() }

    val max = max(studyData.maxOrNull() ?: 0.0, breakData.maxOrNull() ?: 0.0)

    val indicatorProperties = if (max > 0) {
        HorizontalIndicatorProperties(
            contentBuilder = { minute -> minute.toInt().toString() + "분" },
            count = IndicatorCount.StepBased(stepBy = 30.0)
        )
    } else {
        HorizontalIndicatorProperties(
            contentBuilder = { minute -> minute.toInt().toString() + "분" },
            count = IndicatorCount.CountBased(count = 2)
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2A64))
    ) {
        Column(Modifier.padding(16.dp)) {
            val startDay = firstDayOfWeek.dayOfMonth
            val endDay = firstDayOfWeek.plusDays(6).dayOfMonth
            val month = firstDayOfWeek.monthValue
            Text(
                "주간 학습 시간 (${month}월 ${startDay}일 ~ ${endDay}일)",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(16.dp))
            LineChart(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                indicatorProperties = indicatorProperties,
                popupProperties = PopupProperties(
                    contentBuilder = { _, _, value -> if(value >= 0.0) value.roundToInt().toString() + "분" else value.format(1)},
                    mode = PopupProperties.Mode.PointMode(10.dp)
                ),
                gridProperties = GridProperties(
                    xAxisProperties = GridProperties.AxisProperties(
                        lineCount = if (max > 0) (max / 30.0).toInt() + 1 else 1
                    )
                ),
                labelProperties = LabelProperties(
                    enabled = true,
                    labels = weekLabels
                ),
                data = remember(studyData, breakData) {
                    listOf(
                        Line(
                            label = "공부 시간",
                            values = studyData,
                            color = SolidColor(Color.Green),
                            firstGradientFillColor = Color.Green.copy(alpha = .5f),
                            secondGradientFillColor = Color.Transparent,
                            curvedEdges = true,
                            strokeAnimationSpec = tween(2000, easing = EaseInOutCubic),
                            gradientAnimationDelay = 1000,
                            drawStyle = DrawStyle.Stroke(width = 2.dp),
                            dotProperties = DotProperties(
                                enabled = true,
                                color = SolidColor(Color.White),
                                strokeWidth = 2.dp,
                                radius = 2.dp,
                                strokeColor = SolidColor(Color.Green),
                            )
                        ),
                        Line(
                            label = "휴식 시간",
                            values = breakData,
                            color = SolidColor(Color.Blue),
                            firstGradientFillColor = Color.Blue.copy(alpha = .5f),
                            secondGradientFillColor = Color.Transparent,
                            curvedEdges = true,
                            strokeAnimationSpec = tween(2000, easing = EaseInOutCubic),
                            gradientAnimationDelay = 1000,
                            drawStyle = DrawStyle.Stroke(width = 2.dp),
                            dotProperties = DotProperties(
                                enabled = true,
                                color = SolidColor(Color.White),
                                strokeWidth = 2.dp,
                                radius = 2.dp,
                                strokeColor = SolidColor(Color.Blue),
                            )
                        ),
                    )
                },
                animationMode = AnimationMode.Together(),
            )
        }
    }
}