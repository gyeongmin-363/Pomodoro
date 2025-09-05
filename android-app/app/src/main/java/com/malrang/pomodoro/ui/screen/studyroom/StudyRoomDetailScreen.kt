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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
    // ✅ [추가] 공유 인텐트를 위해 LocalContext를 가져옵니다.
    val context = LocalContext.current
    var tappedDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val headerText = "${selectedDate.year}년 ${selectedDate.month.getDisplayName(TextStyle.FULL, Locale.KOREAN)}"

    // 화면이 처음 구성될 때 roomId를 사용하여 스터디룸 정보를 불러옵니다.
    LaunchedEffect(roomId) {
        if (roomId != null) {
            roomVm.loadStudyRoomById(roomId)
            roomVm.loadStudyRoomMembers(roomId)
        }
    }

    val uiState by roomVm.studyRoomUiState.collectAsState()
    val room = uiState.currentStudyRoom
    val members = uiState.currentRoomMembers

    Box(modifier = Modifier.fillMaxSize()){
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(top = 16.dp)
        ){
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(room?.name ?: "스터디룸 로딩 중...", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "돌아가기",
                        tint = Color.White
                    )
                }
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
                    Icon(Icons.Default.Share, contentDescription = "스터디룸 공유")
                }
            }

            if (room == null) {
                // 로딩 상태 표시
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // 스터디룸 정보 및 멤버 목록 표시
                Text(
                    text = room.inform ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "참여자 (${members.size}명)",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(members) { member ->
                        Text(
                            text = "- ${member.nickname}",
                            style = MaterialTheme.typography.bodyLarge
                        )
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




                // TODO: 여기에 스터디룸 관련 추가 UI(채팅, 현황 등)를 구현할 수 있습니다.
            }
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
