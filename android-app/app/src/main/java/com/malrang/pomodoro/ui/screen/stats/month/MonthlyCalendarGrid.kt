package com.malrang.pomodoro.ui.screen.stats.month

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun MonthlyCalendarGrid(
    selectedDate: LocalDate,
    tappedDate: LocalDate?,
    onDateTap: (LocalDate) -> Unit,
    onDateLongTap: (LocalDate) -> Unit, // ✅ 롱클릭 콜백 추가
    getStudyTime: (LocalDate) -> Int
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
                DayCell(
                    date = date,
                    studyTimeMinutes = getStudyTime(date),
                    isToday = date == today,
                    isSelected = date == tappedDate,
                    onClick = { onDateTap(date) },
                    onLongClick = { onDateLongTap(date) } // ✅ 롱클릭 연결
                )
            } else {
                Spacer(modifier = Modifier.size(40.dp))
            }
        }
    }
}