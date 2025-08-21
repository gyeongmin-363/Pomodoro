package com.malrang.pomodoro.ui.screen

import android.R.attr.data
import android.R.attr.enabled
import android.R.attr.text
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathSegment
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malrang.pomodoro.dataclass.ui.DailyStat
import com.malrang.pomodoro.dataclass.ui.Screen
import com.malrang.pomodoro.viewmodel.PomodoroViewModel
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.models.AnimationMode
import ir.ehsannarmani.compose_charts.models.DividerProperties
import ir.ehsannarmani.compose_charts.models.DotProperties
import ir.ehsannarmani.compose_charts.models.DrawStyle
import ir.ehsannarmani.compose_charts.models.GridProperties
import ir.ehsannarmani.compose_charts.models.HorizontalIndicatorProperties
import ir.ehsannarmani.compose_charts.models.IndicatorCount
import ir.ehsannarmani.compose_charts.models.LabelProperties
import ir.ehsannarmani.compose_charts.models.Line
import ir.ehsannarmani.compose_charts.models.LineProperties
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun StatsScreen(vm: PomodoroViewModel) {
    val state by vm.uiState.collectAsState()
    val last7Sessions = remember(state.dailyStats) {
        val today = LocalDate.now()
        (0..6).map { i -> today.minusDays((6 - i).toLong()) }.map { d ->
            val key = d.toString()
            // 세션 수는 이제 studyTime / studyDuration으로 계산하거나, 다르게 추적해야 합니다.
            // 여기서는 간단하게 studyTimeInMinutes 값으로 대체합니다.
            state.dailyStats[key]?.studyTimeInMinutes ?: 0
        }
    }
    val labels = (0..6).map { LocalDate.now().minusDays((6 - it).toLong()) }.map {
        it.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.KOREAN)
    }

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

        // --- 새로운 주간 시간 차트 추가 ---
        WeeklyTimeChart(dailyStats = state.dailyStats)

        Spacer(Modifier.height(24.dp))

        Text(
            "지난 7일간 공부 시간 (분)",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
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

            LineChart(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                indicatorProperties = HorizontalIndicatorProperties(
                    contentBuilder = { minute -> minute.toInt().toString() + "분" },
                    count = IndicatorCount.StepBased(stepBy = 10.0)
                ),
                gridProperties = GridProperties(
                    xAxisProperties = GridProperties.AxisProperties(
                        lineCount = 1
                    )
                ),
                labelProperties = LabelProperties(
                    enabled = true,
                    labels = weekLabels
                ),
                data = remember {
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
//            MultiLineChart(
//                 data = { listOf(studyLineData, breakLineData) },
//                 modifier = Modifier
//                     .fillMaxWidth()
//                     .height(200.dp),
//                smoothLineCurve = true, // 부드러운 선
//                showFilledArea = true, // 채워진 영역 표시
//                showLineStroke = true, // 선 두께 표시
//                labelConfig = LabelConfig.default().copy(
//                    showXLabel = true, // X축 레이블 표시
//                    showYLabel = true, // Y축 레이블 표시
//                    textColor = ChartColor.Solid(Color.Gray) // 레이블 텍스트 색상
//                ),
//                chartConfig = LineChartConfig(
//                    lineConfig = LineConfig(drawPointerCircle = true), // 포인터 원 표시
//                )
//             )
//            // X축 레이블
//            Spacer(Modifier.height(10.dp))
//            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
//                weekLabels.forEach {
//                    Text(
//                        it,
//                        color = Color.White.copy(alpha = 0.8f),
//                        fontSize = 12.sp,
//                        textAlign = TextAlign.Center,
//                        modifier = Modifier.weight(1f)
//                    )
//                }
//            }
        }
    }
}