package com.malrang.pomodoro.ui.screen.studyroom

import android.R.attr.onClick
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.malrang.pomodoro.dataclass.ui.DailyStat
import com.malrang.pomodoro.ui.screen.stats.DayCell
import com.malrang.pomodoro.ui.screen.stats.ExpandableCalendarView
import com.malrang.pomodoro.ui.screen.stats.MonthlyCalendarGrid
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
    val headerText = "${selectedDate.year}년 ${selectedDate.month.getDisplayName(TextStyle.FULL, Locale.KOREAN)}"

    // ✅ 1. 버튼의 상태(완료 여부)를 관리할 변수를 추가합니다.
    var isChallengeCompleted by remember { mutableStateOf(false) }


    LaunchedEffect(roomId) {
        if (roomId != null) {
            roomVm.loadStudyRoomById(roomId)
            roomVm.loadStudyRoomMembers(roomId)
        }
    }

    val uiState by roomVm.studyRoomUiState.collectAsState()
    val room = uiState.currentStudyRoom
    val members = uiState.currentRoomMembers

    // Box를 사용하여 컨텐츠와 버튼을 겹치게 배치합니다.
    Box(modifier = Modifier.fillMaxSize().background(backgroundColor)){
        val scrollState = rememberScrollState()
        // ✅ 2. Column에 verticalScroll과 하단 padding을 추가하여
        // 스크롤이 가능하게 하고, 버튼에 내용이 가려지지 않도록 합니다.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 80.dp) // 버튼이 차지할 공간 확보
        ){
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(room?.name ?: "스터디룸 로딩 중...", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Row {
                    IconButton(onClick = {
                        val shareUrl = "https://pixbbo.netlify.app/study-room/$roomId"
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, "[뽀모도로 스터디] '${room?.name}' 스터디룸에 참여해보세요!\n$shareUrl")
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "스터디룸 공유")
                        context.startActivity(shareIntent)
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "스터디룸 공유", tint = Color.White)
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
                    // 멤버 목록은 LazyColumn 대신 Column으로 변경하여 전체 스크롤에 포함시킵니다.
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
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "이전", tint = Color.White)
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
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "다음", tint = Color.White)
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

                    c(
                        selectedDate,
                        tappedDate,
                        onDateTap = { date -> tappedDate = date }
                    )
                } //달력 끝

                RankingItem(rank = 1, name = "홍길동", status = "26일 완료", progress = 0.8f)

                // TODO: 여기에 스터디룸 관련 추가 UI(채팅, 현황 등)를 구현할 수 있습니다.
            }
        }

        // ✅ 3. 화면 하단에 고정될 버튼을 추가합니다.
        Button(
            // 버튼 클릭 시 상태를 true로 변경
            onClick = { isChallengeCompleted = true },
            modifier = Modifier
                .align(Alignment.BottomCenter) // Box의 하단 중앙에 위치
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            // isChallengeCompleted가 true이면 버튼 비활성화
            enabled = !isChallengeCompleted,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50), // 활성 상태 색상
                disabledContainerColor = Color.Gray // 비활성 상태 색상
            )
        ) {
            Text(
                // isChallengeCompleted 값에 따라 텍스트 변경
                text = if (isChallengeCompleted) "오늘 챌린지 완료됨!" else "오늘 챌린지 완료하기",
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
private fun c(
    selectedDate: LocalDate,
//    dailyStats: Map<String, DailyStat>,
    tappedDate: LocalDate?,
    onDateTap: (LocalDate) -> Unit
){
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
//                val hasRecord = (dailyStats[date.toString()]?.totalStudyTimeInMinutes ?: 0) > 0
                d(
                    date = date,
//                    hasRecord = hasRecord,
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
private fun d(
    date: LocalDate,
//    hasRecord: Boolean,
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

//        if (hasRecord) {
//            Text(
//                text = "🐾",
//                fontSize = 28.sp,
//                color = Color(0xFFFBBF24).copy(alpha = 0.6f),
//            )
//        }
        Text(
            text = date.dayOfMonth.toString(),
            color = dayColor,
            fontWeight = FontWeight.Medium
        )
    }
}