package com.malrang.pomodoro.ui.screen.stats.month


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
