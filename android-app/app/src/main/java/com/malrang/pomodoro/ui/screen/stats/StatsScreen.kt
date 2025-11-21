package com.malrang.pomodoro.ui.screen.stats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malrang.pomodoro.dataclass.ui.DailyStat
import com.malrang.pomodoro.dataclass.ui.Screen
import com.malrang.pomodoro.viewmodel.StatsViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun StatsScreen(
    statsViewModel: StatsViewModel,
    onNavigateTo: (Screen) -> Unit,
    onNavigateToDetail: (LocalDate) -> Unit // ✅ 상세 화면 이동 콜백 추가
) {
    val state by statsViewModel.uiState.collectAsState()
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    val monthlyTotalMinutes = remember(selectedDate, state.dailyStats) {
        val targetMonth = YearMonth.from(selectedDate)
        state.dailyStats.values.filter { stat ->
            try {
                val statDate = LocalDate.parse(stat.date)
                YearMonth.from(statDate) == targetMonth
            } catch (e: Exception) {
                false
            }
        }.sumOf { it.totalStudyTimeInMinutes }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📊 통계", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            IconButton(onClick = { onNavigateTo(Screen.Main) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "돌아가기",
                    tint = Color.White
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        MonthlySummaryBar(monthlyTotalMinutes = monthlyTotalMinutes)
        Spacer(Modifier.height(16.dp))

        MonthlyStatsCalendar(
            dailyStats = state.dailyStats,
            selectedDate = selectedDate,
            onDateSelected = { newDate -> selectedDate = newDate },
            onDetailRequested = onNavigateToDetail // ✅ 상세 요청 전달
        )
    }
}

@Composable
private fun MonthlySummaryBar(monthlyTotalMinutes: Int) {
    val hours = monthlyTotalMinutes / 60
    val minutes = monthlyTotalMinutes % 60
    val timeText = if (hours > 0) "${hours}시간 ${minutes}분" else "${minutes}분"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF424242))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "이번 달 총 집중시간",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = timeText,
                color = Color(0xFFFBBF24),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun MonthlyStatsCalendar(
    dailyStats: Map<String, DailyStat>,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDetailRequested: (LocalDate) -> Unit
) {
    var tappedDate by remember { mutableStateOf<LocalDate?>(null) }
    val headerText = "${selectedDate.year}년 ${selectedDate.month.getDisplayName(TextStyle.FULL, Locale.KOREAN)}"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF525252))
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
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
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "이전 달", tint = Color.White)
                }
                Text(
                    text = headerText,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                IconButton(onClick = {
                    onDateSelected(selectedDate.plusMonths(1))
                    tappedDate = null
                }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "다음 달", tint = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // 요일 헤더
            val daysOfWeek = listOf("일", "월", "화", "수", "목", "금", "토")
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

            // 달력 그리드
            MonthlyCalendarGrid(
                selectedDate = selectedDate,
                tappedDate = tappedDate,
                onDateTap = { date -> tappedDate = date },
                onDateLongTap = { date -> onDetailRequested(date) }, // 롱클릭 시 상세 이동
                getStudyTime = { date ->
                    dailyStats[date.toString()]?.totalStudyTimeInMinutes ?: 0
                }
            )

            // ✅ 날짜 선택 시 하단 요약 정보 표시
            AnimatedVisibility(visible = tappedDate != null) {
                tappedDate?.let { date ->
                    val stats = dailyStats[date.toString()]
                    val studyTime = stats?.totalStudyTimeInMinutes ?: 0
                    val checklistTotal = stats?.checklist?.size ?: 0
                    val checklistDone = stats?.checklist?.values?.count { it } ?: 0
                    val retrospect = stats?.retrospect ?: "작성된 회고가 없습니다."

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "${date.monthValue}월 ${date.dayOfMonth}일 요약",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // 1. 당일 공부 총 시간
                        Text("⏱️ 총 공부 시간: ${studyTime}분", color = Color.White, fontSize = 14.sp)

                        // 2. 체크리스트 완료/총개수
                        Text("✅ 체크리스트: $checklistDone / $checklistTotal", color = Color.White, fontSize = 14.sp)

                        // 3. 회고 1줄 (말줄임표)
                        Text(
                            text = "📝 회고: $retrospect",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 4. 상세보기 버튼
                        Button(
                            onClick = { onDetailRequested(date) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0E4429))
                        ) {
                            Text("상세보기", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}