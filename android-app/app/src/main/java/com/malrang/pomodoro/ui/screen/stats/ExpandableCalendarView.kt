package com.malrang.pomodoro.ui.screen.stats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.sp
import com.malrang.pomodoro.dataclass.ui.DailyStat
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

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

    val headerText = "${selectedDate.year}년 ${selectedDate.month.getDisplayName(TextStyle.FULL, Locale.KOREAN)}"

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
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "이전", tint = Color.White)
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
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "다음", tint = Color.White)
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
                    tappedDate = tappedDate,
                    onDateTap = { date -> tappedDate = date },
                    hasRecord = { date ->
                        (dailyStats[date.toString()]?.totalStudyTimeInMinutes ?: 0) > 0
                    }
                )
            } else {
                WeeklyCalendarGrid(selectedDate = selectedDate, dailyStats = dailyStats)
            }

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

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            thickness = 1.dp,
                            color = Color.White.copy(alpha = 0.2f)
                        )

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

            HorizontalDivider(
                modifier = Modifier.padding(top = 8.dp),
                thickness = 1.dp,
                color = Color.White.copy(alpha = 0.2f)
            )
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