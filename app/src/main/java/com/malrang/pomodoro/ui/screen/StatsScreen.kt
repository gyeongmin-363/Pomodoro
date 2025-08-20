package com.malrang.pomodoro.ui.screen

// ... 기존 import 문들 ...
import android.R.attr.x
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathSegment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.himanshoe.charty.common.ChartColor
import com.himanshoe.charty.common.LabelConfig
import com.himanshoe.charty.line.LineChart
import com.himanshoe.charty.line.MultiLineChart
import com.himanshoe.charty.line.config.LineChartColorConfig
import com.himanshoe.charty.line.config.LineChartConfig
import com.himanshoe.charty.line.config.LineConfig
import com.himanshoe.charty.line.model.LineData
import com.himanshoe.charty.line.model.MultiLineData
import com.malrang.pomodoro.dataclass.ui.DailyStat
import com.malrang.pomodoro.dataclass.ui.Screen
import com.malrang.pomodoro.viewmodel.PomodoroViewModel
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

    val studyData = weeklyData.map { it.studyTimeInMinutes.toFloat() }
    val breakData = weeklyData.map { it.breakTimeInMinutes.toFloat() }

    val studyLineData = MultiLineData(
        data = studyData.mapIndexed { x, y -> LineData(y, x) },
        colorConfig = LineChartColorConfig.default().copy(
            lineColor = ChartColor.Solid(Color(0xFF4ADE80)), //선 색깔
            lineFillColor = ChartColor.Solid(Color(0xFF4ADE80)), //선 채움 색깔
        )
    )


    val breakLineData = MultiLineData(
        data = breakData.mapIndexed { x, y -> LineData(y, x) },
        colorConfig = LineChartColorConfig.default().copy(
            lineColor = ChartColor.Solid(Color(0xFF60A5FA)), //선 색깔
            lineFillColor = ChartColor.Solid(Color(0xFF60A5FA)), //선 채움 색깔
        )
    )

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
            MultiLineChart(
                 data = { listOf(studyLineData, breakLineData) },
                 modifier = Modifier
                     .fillMaxWidth()
                     .height(200.dp),
                smoothLineCurve = true, // 부드러운 선
                showFilledArea = true, // 채워진 영역 표시
                showLineStroke = true, // 선 두께 표시
                labelConfig = LabelConfig.default().copy(
//                    showXLabel = true, // X축 레이블 표시
                    showYLabel = true, // Y축 레이블 표시
                    textColor = ChartColor.Solid(Color.Gray) // 레이블 텍스트 색상
                ),
                chartConfig = LineChartConfig(
                    lineConfig = LineConfig(drawPointerCircle = true), // 포인터 원 표시
                )
             )
            // X축 레이블
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                weekLabels.forEach {
                    Text(
                        it,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}