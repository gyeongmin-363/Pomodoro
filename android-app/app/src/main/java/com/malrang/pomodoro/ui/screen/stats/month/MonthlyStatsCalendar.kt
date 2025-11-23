package com.malrang.pomodoro.ui.screen.stats.month

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.malrang.pomodoro.dataclass.ui.DailyStat
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun MonthlyStatsCalendar(
    dailyStats: Map<String, DailyStat>,
    currentMonthDate: LocalDate,     // [변경] 현재 보여지는 달
    selectedDate: LocalDate?,        // [변경] 선택된 날짜 (Nullable)
    selectedFilter: String,
    onMonthChanged: (LocalDate) -> Unit, // [추가] 달 이동 콜백
    onDateSelected: (LocalDate) -> Unit, // [추가] 날짜 선택 콜백
    onDetailRequested: (LocalDate) -> Unit
) {
    val headerText = "${currentMonthDate.year}년 ${currentMonthDate.month.getDisplayName(TextStyle.FULL, Locale.KOREAN)}"

    Column(Modifier.fillMaxWidth()) {
        // 월 이동 헤더
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onMonthChanged(currentMonthDate.minusMonths(1)) }) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "이전 달",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            Text(
                text = headerText,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            IconButton(onClick = { onMonthChanged(currentMonthDate.plusMonths(1)) }) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "다음 달",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))

        // 요일 헤더
        val daysOfWeek = listOf("일", "월", "화", "수", "목", "금", "토")
        Row(Modifier.fillMaxWidth()) {
            daysOfWeek.forEach { day ->
                val color = when (day) {
                    "토" -> Color(0xFF90CAF9)
                    "일" -> Color(0xFFEF9A9A)
                    else -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
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
            selectedDate = currentMonthDate, // 그리드를 그릴 기준 달
            tappedDate = selectedDate,       // 하이라이트 할 날짜
            onDateTap = onDateSelected,      // 클릭 시 부모에게 알림
            onDateLongTap = onDetailRequested,
            getStudyTime = { date ->
                dailyStats[date.toString()]?.getStudyTime(selectedFilter) ?: 0
            }
        )
    }
}