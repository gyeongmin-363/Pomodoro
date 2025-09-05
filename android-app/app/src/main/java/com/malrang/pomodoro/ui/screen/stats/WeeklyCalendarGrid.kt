package com.malrang.pomodoro.ui.screen.stats

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.malrang.pomodoro.dataclass.ui.DailyStat
import java.time.DayOfWeek
import java.time.LocalDate


@Composable
fun WeeklyCalendarGrid(selectedDate: LocalDate, dailyStats: Map<String, DailyStat>) {
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

