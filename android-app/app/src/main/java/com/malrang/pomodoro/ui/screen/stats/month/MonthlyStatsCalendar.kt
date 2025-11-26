package com.malrang.pomodoro.ui.screen.stats.month

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
    currentMonthDate: LocalDate,
    selectedDate: LocalDate?,
    selectedFilter: String,
    onMonthChanged: (LocalDate) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
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
            // 네비게이션 버튼 (작은 사각형 스타일)
            IconButton(
                onClick = { onMonthChanged(currentMonthDate.minusMonths(1)) },
                modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)).size(32.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "이전 달",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = headerText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            IconButton(
                onClick = { onMonthChanged(currentMonthDate.plusMonths(1)) },
                modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)).size(32.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "다음 달",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))

        // 요일 헤더
        val daysOfWeek = listOf("일", "월", "화", "수", "목", "금", "토")
        Row(Modifier.fillMaxWidth()) {
            daysOfWeek.forEach { day ->
                val color = when (day) {
                    "토" -> Color(0xFF3B82F6) // NeoBlue
                    "일" -> Color(0xFFFF4848) // NeoRed
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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
            selectedDate = currentMonthDate,
            tappedDate = selectedDate,
            onDateTap = onDateSelected,
            onDateLongTap = onDetailRequested,
            getStudyTime = { date ->
                dailyStats[date.toString()]?.getStudyTime(selectedFilter) ?: 0
            }
        )
    }
}