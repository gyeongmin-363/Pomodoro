package com.malrang.pomodoro.ui.screen

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun StatsScreen(vm: PomodoroViewModel) {
    val state by vm.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1B4B))
            .padding(16.dp)
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

        // --- 주간 시간 차트 ---
        WeeklyTimeChart(dailyStats = state.dailyStats)

        Spacer(Modifier.height(24.dp))

        // --- 월간 기록 캘린더 ---
        CalendarView(dailyStats = state.dailyStats)
    }
}

/**
 * 월간 달력을 표시하고, 공부 기록이 있는 날에 스탬프를 찍어주는 Composable
 */
@Composable
fun CalendarView(dailyStats: Map<String, DailyStat>) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    val today = LocalDate.now()

    // 달력의 각 날짜들 계산
    val firstDayOfMonth = currentMonth.atDay(1)
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7 // 일요일(7)을 0으로 맞춤
    val daysInMonth = currentMonth.lengthOfMonth()
    val calendarDays = (0 until firstDayOfWeek).map<Int?, LocalDate?> { null } + (1..daysInMonth).map { firstDayOfMonth.withDayOfMonth(it) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2A64))
    ) {
        Column(Modifier.padding(16.dp)) {
            // 헤더 (월 이동 버튼, 년/월 표시)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "이전 달", tint = Color.White)
                }
                Text(
                    text = "${currentMonth.year}년 ${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.KOREAN)}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "다음 달", tint = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // 요일 헤더 (일, 월, 화, 수, 목, 금, 토)
            Row(Modifier.fillMaxWidth()) {
                listOf("일", "월", "화", "수", "목", "금", "토").forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // 날짜 그리드
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                userScrollEnabled = false // 캘린더는 스크롤되지 않도록 고정
            ) {
                items(calendarDays.size) { index ->
                    val date = calendarDays[index]
                    if (date != null) {
                        val hasRecord = (dailyStats[date.toString()]?.studyTimeInMinutes ?: 0) > 0
                        DayCell(date = date, hasRecord = hasRecord, isToday = date == today)
                    } else {
                        // 날짜가 시작되기 전의 빈 공간
                        Spacer(modifier = Modifier.size(40.dp))
                    }
                }
            }
        }
    }
}

/**
 * 캘린더의 각 날짜를 표시하는 셀 Composable
 */
@Composable
fun DayCell(date: LocalDate, hasRecord: Boolean, isToday: Boolean) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        // 오늘 날짜를 표시하는 배경 원
        if (isToday) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.3f))
            )
        }

        // 고양이 발바닥 스탬프 (기록이 있을 경우)
        if (hasRecord) {
            Text(
                text = "🐾",
                fontSize = 28.sp,
                color = Color(0xFFFBBF24).copy(alpha = 0.6f),
            )
        }

        // 날짜 텍스트
        Text(
            text = date.dayOfMonth.toString(),
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
    }
}


@Composable
fun WeeklyTimeChart(dailyStats: Map<String, DailyStat>) {
    val weekLabels = listOf("월", "화", "수", "목", "금", "토", "일")
    val today = LocalDate.now()
    val firstDayOfWeek = today.with(DayOfWeek.MONDAY)

    val weeklyData = (0..6).map { i ->
        val date = firstDayOfWeek.plusDays(i.toLong())
        dailyStats[date.toString()] ?: DailyStat(date.toString(), 0, 0)
    }

    val studyData = weeklyData.map { it.studyTimeInMinutes.toDouble() }
    val breakData = weeklyData.map { it.breakTimeInMinutes.toDouble() }

    val max = max(studyData.maxOrNull() ?: 0.0, breakData.maxOrNull() ?: 0.0)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2A64))
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "이번 주 학습 시간 (분)",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(16.dp))

            LineChart( //선 그래프
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                indicatorProperties = HorizontalIndicatorProperties( //x축과 평행한 선을 그리기 위한 속성 (y축 레이블과 관련)
                    contentBuilder = { minute -> minute.toInt().toString() + "분" }, //레이블 텍스트
                    count = IndicatorCount.StepBased(stepBy = 30.0) //평행선 갯수(countbase : 고정갯수 / StepBased : 일정 간격마다)
                ),
                popupProperties = PopupProperties( //그래프 터치시 나오는 작은 팝업
                    contentBuilder = { _, _, value -> value.roundToInt().toString() + "분" }, //팝업 텍스트
                    //팝업 표현 조건(Normal : 터치한 무조건 터치한 선의 Double 좌표 / PointMode : x축 레이블이 존재하는 곳만 팝업)
                    mode = PopupProperties.Mode.PointMode(10.dp)
                ),
                gridProperties = GridProperties( //격자선
                    xAxisProperties = GridProperties.AxisProperties( //x축과 평행한 수평선
                        lineCount = if (max > 0) (max / 30.0).toInt() + 1 else 1 //갯수
                    )
                ),
                labelProperties = LabelProperties( //x축 레이블
                    enabled = true,
                    labels = weekLabels //x축의 레이블 이름
                ),
                data = remember(studyData, breakData) { //사용 데이터
                    listOf(
                        Line(
                            label = "공부 시간", //이름
                            values = studyData, //값
                            color = SolidColor(Color.Green), //선 색상
                            firstGradientFillColor = Color.Green.copy(alpha = .5f), //채우기의 시작 색상(그라데이션)
                            secondGradientFillColor = Color.Transparent, //채우기의 마지막 색상(그라데이션)
                            curvedEdges = true, //부드러운 곡선
                            strokeAnimationSpec = tween(2000, easing = EaseInOutCubic), //선이 그려지는 애니메이션
                            gradientAnimationDelay = 1000, //선이 그려지기 시작하고, 채우기가 진행될 딜레이
                            drawStyle = DrawStyle.Stroke(width = 2.dp), //선의 스타일
                            dotProperties = DotProperties( // 각 x레이블마다 나타나는 점의 속성
                                enabled = true,
                                color = SolidColor(Color.White), //점 윤곽선
                                strokeWidth = 2.dp, //윤곽선 두께
                                radius = 2.dp, //점 반지름
                                strokeColor = SolidColor(Color.Green), //윤곽선 색상
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
                animationMode = AnimationMode.Together(), //애니메이션 모드(Together : 한꺼번에, 딜레이조절 가능 / OneByOne : 하나씩)
            )
        }
    }
}