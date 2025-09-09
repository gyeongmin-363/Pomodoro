package com.malrang.pomodoro.ui.screen.stats

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malrang.pomodoro.dataclass.ui.DailyStat
import ir.ehsannarmani.compose_charts.ColumnChart
import ir.ehsannarmani.compose_charts.extensions.format
import ir.ehsannarmani.compose_charts.models.AnimationMode
import ir.ehsannarmani.compose_charts.models.BarProperties
import ir.ehsannarmani.compose_charts.models.Bars
import ir.ehsannarmani.compose_charts.models.GridProperties
import ir.ehsannarmani.compose_charts.models.HorizontalIndicatorProperties
import ir.ehsannarmani.compose_charts.models.IndicatorCount
import ir.ehsannarmani.compose_charts.models.LabelHelperProperties
import ir.ehsannarmani.compose_charts.models.LabelProperties
import ir.ehsannarmani.compose_charts.models.PopupProperties
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.roundToInt

@Composable
fun WeeklyTimeChart(dailyStats: Map<String, DailyStat>, displayDate: LocalDate) {
    // 주의 시작을 일요일로 변경
    val firstDayOfWeek = displayDate.with(DayOfWeek.MONDAY)

    val weeklyData = (0..6).map { i ->
        val date = firstDayOfWeek.plusDays(i.toLong())
        dailyStats[date.toString()] ?: DailyStat(date.toString())
    }

    // 월요일부터 일요일까지의 모든 막대 데이터를 담는 리스트
    val allWeekBars = weeklyData.mapIndexed { index, dailyStat ->
        listOf(
            // 공부 시간 데이터
            Bars.Data(
                id = index, // 0:월, 1:화, ...
                label = "공부 시간",
                value = dailyStat.totalStudyTimeInMinutes.toDouble(),
                color = SolidColor(Color(0xFFFD8584)) // 원하는 색상으로 변경
            ),
            // 쉬는 시간 데이터
            Bars.Data(
                id = index,
                label = "쉬는 시간",
                value = dailyStat.totalBreakTimeInMinutes.toDouble(), // 이 부분을 실제 쉬는 시간 데이터 속성으로 변경
                color = SolidColor(Color(0xFF7AD2E9)) // 원하는 색상으로 변경
            )
        )
    }

    // 이제 allWeekBars 리스트에서 각 요일 데이터를 인덱스로 접근할 수 있습니다.
    val mondayBars = allWeekBars[0]    // 월요일
    val tuesdayBars = allWeekBars[1]   // 화요일
    val wednesdayBars = allWeekBars[2] // 수요일
    val thursdayBars = allWeekBars[3]  // 목요일
    val fridayBars = allWeekBars[4]    // 금요일
    val saturdayBars = allWeekBars[5]  // 토요일
    val sundayBars = allWeekBars[6]    // 일요일


//    val mondayBars = listOf(
//        Bars.Data(id = 0, label = "공부 시간", value = 50.0, color = SolidColor(Color.Red)),
//        Bars.Data(id = 0, label = "쉬는 시간", value = 10.0, color = SolidColor(Color.Blue)),
//    )

    val labelTextStyle = androidx.compose.ui.text.TextStyle.Default.copy(
        color = Color.White,
        fontSize = 12.sp,
    )


    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF525252))
    ) {
        Column(Modifier.padding(16.dp)) {
            val startDay = firstDayOfWeek.dayOfMonth
            val endDay = firstDayOfWeek.plusDays(6).dayOfMonth
            val month = firstDayOfWeek.monthValue
            Text(
                "주간 학습 시간\n(${month}월 ${startDay}일 ~ ${endDay}일)",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(16.dp))
            ColumnChart(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                indicatorProperties = HorizontalIndicatorProperties(
                    contentBuilder = { minute -> minute.toInt().toString() + "분" },
                    count = IndicatorCount.CountBased(4),
                    textStyle = labelTextStyle
                ),
                popupProperties = PopupProperties(
                    contentBuilder = { _, _, value -> if(value >= 0.0) value.roundToInt().toString() + "분" else value.format(1)},
                    mode = PopupProperties.Mode.PointMode(10.dp),
                    textStyle = labelTextStyle
                ),
                gridProperties = GridProperties(
                    xAxisProperties = GridProperties.AxisProperties(
                        lineCount = 4
                    ),
                    yAxisProperties = GridProperties.AxisProperties(
                        enabled = false
                    )
                ),
                labelProperties = LabelProperties(
                    enabled = true,
                    textStyle = labelTextStyle.copy(fontSize = 10.sp)
                ),
                labelHelperProperties = LabelHelperProperties(
                    enabled = true,
                    textStyle = labelTextStyle,
                    labelCountPerLine = 2
                ),
                data = remember(
                    mondayBars,
                    tuesdayBars,
                    wednesdayBars,
                    thursdayBars,
                    fridayBars,
                    saturdayBars,
                    sundayBars
                ) {
                    listOf(
                        Bars(
                            label = "월",
                            values = mondayBars
                        ),
                        Bars(
                            label = "화",
                            values = tuesdayBars
                        ),
                        Bars(
                            label = "수",
                            values = wednesdayBars
                        ),
                        Bars(
                            label = "목",
                            values = thursdayBars
                        ),
                        Bars(
                            label = "금",
                            values = fridayBars
                        ),
                        Bars(
                            label = "토",
                            values = saturdayBars
                        ),
                        Bars(
                            label = "일",
                            values = sundayBars
                        )
                    )
                },
                animationMode = AnimationMode.Together(),
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                barProperties = BarProperties(
                    cornerRadius = Bars.Data.Radius.Rectangle(topRight = 6.dp, topLeft = 6.dp),
                    spacing = 0.dp,
                    thickness = 10.dp
                ),
            )
        }
    }
}