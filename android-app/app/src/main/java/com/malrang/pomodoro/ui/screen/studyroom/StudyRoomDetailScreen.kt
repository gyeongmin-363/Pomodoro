package com.malrang.pomodoro.ui.screen.studyroom

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malrang.pomodoro.networkRepo.StudyRoomMemberWithProgress
import com.malrang.pomodoro.ui.theme.backgroundColor
import com.malrang.pomodoro.viewmodel.StudyRoomViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale


/**
 * 스터디룸 상세 정보를 표시하는 화면 Composable
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyRoomDetailScreen(
    roomId: String?,
    roomVm: StudyRoomViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var tappedDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val headerText =
        "${selectedDate.year}년 ${selectedDate.month.getDisplayName(TextStyle.FULL, Locale.KOREAN)}"
    val uiState by roomVm.studyRoomUiState.collectAsState()
    val room = uiState.currentStudyRoom
    val members = uiState.currentRoomMembers
    val currentUser = uiState.currentUser
    val habitProgressMap = uiState.habitProgressMap


    // ✅ roomId가 변경되거나, 달력의 월(selectedDate)이 변경될 때 데이터를 새로고침합니다.
    LaunchedEffect(roomId, selectedDate) {
        if (roomId != null) {
            roomVm.loadStudyRoomById(roomId)
            roomVm.loadStudyRoomMembers(roomId)
            roomVm.loadHabitSummaryForMonth(roomId, selectedDate)
        }
    }

    // ✅ 랭킹 계산 로직: 멤버 목록과 습관 진행 현황을 조합하여 랭킹 리스트를 생성합니다.
    val rankingList by remember(members, habitProgressMap, selectedDate) {
        mutableStateOf(
            members.map { member ->
                val progress = habitProgressMap[member.user_id]
                val completedDays = progress?.daily_progress?.count { it == '1' } ?: 0
                val totalDaysInMonth = YearMonth.from(selectedDate).lengthOfMonth()
                val progressValue = if (totalDaysInMonth > 0) completedDays.toFloat() / totalDaysInMonth else 0f
                StudyRoomMemberWithProgress(member, completedDays, progressValue)
            }.sortedByDescending { it.completedDays }
        )
    }

    // ✅ 오늘 챌린지를 완료했는지 확인합니다.
    val isChallengeCompletedToday = remember(habitProgressMap, currentUser) {
        val today = LocalDate.now()
        val userProgress = habitProgressMap[currentUser?.id]
        if (userProgress != null && userProgress.year_month == today.format(
                java.time.format.DateTimeFormatter.ofPattern(
                    "yyyy-MM"
                )
            )
        ) {
            userProgress.daily_progress.getOrNull(today.dayOfMonth - 1) == '1'
        } else {
            false
        }
    }

    // Box를 사용하여 컨텐츠와 버튼을 겹치게 배치합니다.
    Box(modifier = Modifier
        .fillMaxSize()
        .background(backgroundColor)) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 80.dp) // 버튼이 차지할 공간 확보
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    room?.name ?: "스터디룸 로딩 중...",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Row {
                    IconButton(onClick = {
                        val shareUrl = "https://pixbbo.netlify.app/study-room/$roomId"
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "[뽀모도로 스터디] '${room?.name}' 스터디룸에 참여해보세요!\n$shareUrl"
                            )
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "스터디룸 공유")
                        context.startActivity(shareIntent)
                    }) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "스터디룸 공유",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "돌아가기",
                            tint = Color.White
                        )
                    }
                }
            }

            if (room == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = room.inform ?: "",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "참여자 (${members.size}명)",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        members.forEach { member ->
                            Text(
                                text = "- ${member.nickname}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }


                //달력
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            selectedDate = selectedDate.minusMonths(1)
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "이전",
                                tint = Color.White
                            )
                        }
                        Text(
                            text = headerText,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        IconButton(onClick = {
                            selectedDate = selectedDate.plusMonths(1)
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "다음",
                                tint = Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

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

                    // ✅ 현재 로그인한 유저의 완료 기록을 Set으로 만들어 전달
                    val completedDaysSet = remember(habitProgressMap, currentUser) {
                        habitProgressMap[currentUser?.id]?.daily_progress?.mapIndexedNotNull { index, c ->
                            if (c == '1') index + 1 else null
                        }?.toSet() ?: emptySet()
                    }

                    StudyCalendar(
                        selectedDate = selectedDate,
                        tappedDate = tappedDate,
                        completedDays = completedDaysSet,
                        onDateTap = { date -> tappedDate = date }
                    )
                } //달력 끝

                // ✅ 랭킹 UI에 동적 데이터 적용
                Column {
                    rankingList.forEachIndexed { index, item ->
                        RankingItem(
                            rank = index + 1,
                            name = item.member.nickname,
                            status = "${item.completedDays}일 완료",
                            progress = item.progress
                        )
                    }
                }

            }
        }

        // ✅ 화면 하단 고정 버튼 로직 수정
        Button(
            onClick = { if (roomId != null) roomVm.completeTodayChallenge(roomId) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            // isChallengeCompletedToday 값에 따라 버튼 활성화/비활성화
            enabled = !isChallengeCompletedToday,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50),
                disabledContainerColor = Color.Gray
            )
        ) {
            Text(
                text = if (isChallengeCompletedToday) "오늘 챌린지 완료됨!" else "오늘 챌린지 완료하기",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

// 이하 다른 @Composable 함수들은 변경 없음
@Composable
fun RankingItem(
    rank: Int,
    name: String,
    status: String,
    progress: Float // 0.0f ~ 1.0f 사이의 값
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.Top // 컨텐츠를 위쪽으로 정렬
    ) {
        // 1. 순위 표시 (노란색 박스)
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(Color(0xFFFFC107)), // 노란색
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$rank",
                color = Color.Black,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // 2. 이름, 상태, 프로그레스 바 (세로 정렬)
        Column(
            modifier = Modifier.weight(1f) // 남은 공간을 모두 차지
        ) {
            Text(
                text = name,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = status,
                fontSize = 14.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
            // 3. 프로그레스 바
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = Color(0xFF4CAF50), // 초록색
                trackColor = Color.DarkGray
            )
        }
    }
}

@Composable
private fun StudyCalendar(
    selectedDate: LocalDate,
    completedDays: Set<Int>, // ✅ 완료된 날짜(일)를 Set으로 받음
    tappedDate: LocalDate?,
    onDateTap: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    val currentMonth = YearMonth.from(selectedDate)

    val firstDayOfMonth = currentMonth.atDay(1)
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7
    val daysInMonth = currentMonth.lengthOfMonth()
    val calendarDays =
        (0 until firstDayOfWeek).map<Int?, LocalDate?> { null } + (1..daysInMonth).map {
            firstDayOfMonth.withDayOfMonth(
                it
            )
        }

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        userScrollEnabled = false,
        modifier = Modifier.height(240.dp) // 달력 높이 고정
    ) {
        items(calendarDays.size) { index ->
            val date = calendarDays[index]
            if (date != null) {
                // ✅ 해당 날짜가 완료되었는지 확인
                val hasRecord = completedDays.contains(date.dayOfMonth)
                CalendarDay(
                    date = date,
                    hasRecord = hasRecord,
                    isToday = date == today,
                    isSelected = date == tappedDate,
                    onClick = { onDateTap(date) }
                )
            } else {
                Spacer(modifier = Modifier.size(40.dp))
            }
        }
    }

}


@Composable
private fun CalendarDay(
    date: LocalDate,
    hasRecord: Boolean, // ✅ hasRecord 파라미터 추가
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val dayColor = when (date.dayOfWeek) {
        DayOfWeek.SATURDAY -> Color(0xFF64B5F6)
        DayOfWeek.SUNDAY -> Color(0xFFE57373)
        else -> Color.White
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .padding(4.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.3f), CircleShape)
            )
        } else if (isToday) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
            )
        }

        // ✅ hasRecord가 true이면 발바닥 아이콘 표시
        if (hasRecord) {
            Text(
                text = "🐾",
                fontSize = 28.sp,
                color = Color(0xFFFBBF24).copy(alpha = 0.6f),
            )
        }
        Text(
            text = date.dayOfMonth.toString(),
            color = dayColor,
            fontWeight = FontWeight.Medium
        )
    }
}