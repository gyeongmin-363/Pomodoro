package com.malrang.pomodoro.ui.screen.studyroom

import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.malrang.pomodoro.dataclass.animalInfo.AnimalsTable
import com.malrang.pomodoro.dataclass.sprite.SpriteMap
import com.malrang.pomodoro.networkRepo.StudyRoomMember
import com.malrang.pomodoro.networkRepo.StudyRoomMemberWithProgress
import com.malrang.pomodoro.ui.screen.stats.MonthlyCalendarGrid
import com.malrang.pomodoro.ui.theme.backgroundColor
import com.malrang.pomodoro.viewmodel.StudyRoomViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt


/**
 * 챌린지룸 상세 정보를 표시하는 화면 Composable
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyRoomDetailScreen(
    roomId: String?,
    roomVm: StudyRoomViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String) -> Unit
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

    var showDelegateDialog by remember { mutableStateOf(false) }
    var showLeaveConfirmDialog by remember { mutableStateOf(false) }


    // roomId가 변경되거나, 달력의 월(selectedDate)이 변경될 때 데이터를 새로고침합니다.
    LaunchedEffect(roomId, selectedDate) {
        if (roomId != null) {
            roomVm.loadStudyRoomById(roomId)
            roomVm.loadStudyRoomMembers(roomId)
            roomVm.loadHabitSummaryForMonth(roomId, selectedDate)
        }
    }

    // ViewModel의 네비게이션 이벤트를 감지하여 화면을 전환합니다.
    LaunchedEffect(Unit) {
        roomVm.navigationEvents.collect { event ->
            if (event == "navigate_back") {
                onNavigateBack()
            }
        }
    }

    // 랭킹 계산 로직: 멤버 목록과 습관 진행 현황을 조합하여 랭킹 리스트를 생성합니다.
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

    // '오늘 챌린지 완료' 버튼의 상태를 별도로 관리합니다.
    var isChallengeCompletedToday by remember { mutableStateOf(false) }
    LaunchedEffect(habitProgressMap, currentUser) {
        val today = LocalDate.now()
        val currentYearMonth = today.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val userProgress = habitProgressMap[currentUser?.id]

        if (userProgress?.year_month == currentYearMonth) {
            isChallengeCompletedToday = userProgress.daily_progress.getOrNull(today.dayOfMonth - 1) == '1'
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
                    .padding(start = 16.dp, end = 4.dp, top = 16.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = room?.name ?: "로딩 중...",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    var menuExpanded by remember { mutableStateOf(false) }

                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "더보기",
                                tint = Color.White
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("채팅하기") },
                                onClick = {
                                    roomId?.let { onNavigateToChat(it) }
                                    menuExpanded = false
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.MailOutline,
                                        contentDescription = "채팅하기"
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("챌린지룸 공유") },
                                onClick = {
                                    val shareUrl = "https://pixbbo.netlify.app/study-room/$roomId"
                                    val sendIntent: Intent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "[픽뽀] '${room?.name}' 챌린지룸에 참여해보세요!\n$shareUrl"
                                        )
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "챌린지룸 공유")
                                    context.startActivity(shareIntent)
                                    menuExpanded = false
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Share,
                                        contentDescription = "챌린지룸 공유"
                                    )
                                }
                            )

                            HorizontalDivider()

                            // 방장/멤버에 따라 다른 메뉴 아이템 표시
                            val isCreator = room?.creator_id == currentUser?.id
                            val hasOtherMembers = members.any { it.user_id != currentUser?.id }

                            if (isCreator && hasOtherMembers) {
                                // 방장이면서 다른 멤버가 있을 경우: 방장 위임하기
                                DropdownMenuItem(
                                    text = { Text("방장 위임하기") },
                                    onClick = {
                                        showDelegateDialog = true
                                        menuExpanded = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Warning,
                                            contentDescription = "방장 위임"
                                        )
                                    }
                                )
                            } else {
                                // 그 외의 모든 경우 (멤버이거나, 방장이지만 혼자 있는 경우): 방 나가기
                                DropdownMenuItem(
                                    text = { Text("방 나가기") },
                                    onClick = {
                                        showLeaveConfirmDialog = true
                                        menuExpanded = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ExitToApp,
                                            contentDescription = "방 나가기"
                                        )
                                    }
                                )
                            }
                        }
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
                    // ✅ 설명란 확장/축소 기능 추가
                    if (!room.inform.isNullOrBlank()) {
                        var isDescriptionExpanded by remember { mutableStateOf(false) }
                        var isExpandable by remember { mutableStateOf(false) }

                        Column(
                            modifier = Modifier.animateContentSize()
                        ) {
                            if (isDescriptionExpanded) {
                                // 확장된 상태: 전체 텍스트와 위쪽 화살표 버튼
                                Text(
                                    text = room.inform,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.Gray,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (isExpandable) {
                                    IconButton(
                                        onClick = { isDescriptionExpanded = !isDescriptionExpanded },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowUp,
                                            contentDescription = "설명 축소",
                                            tint = Color.Gray
                                        )
                                    }
                                }
                            } else {
                                // 축소된 상태: 한 줄 텍스트와 오른쪽 아래 화살표 버튼
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = room.inform,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color.Gray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .weight(1f) // 텍스트가 남은 공간을 채우도록
                                            .padding(end = if (isExpandable) 4.dp else 0.dp), // 아이콘과의 간격
                                        onTextLayout = { textLayoutResult ->
                                            isExpandable = textLayoutResult.hasVisualOverflow
                                        }
                                    )
                                    if (isExpandable) {
                                        IconButton(
                                            onClick = { isDescriptionExpanded = !isDescriptionExpanded },
                                            modifier = Modifier.size(28.dp) // 작은 크기로 조절
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowDown,
                                                contentDescription = "설명 확장",
                                                tint = Color.Gray,
                                                modifier = Modifier.size(20.dp) // 아이콘 크기 조절
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
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

                        val currentMonth = YearMonth.now()
                        IconButton(
                            onClick = { selectedDate = selectedDate.plusMonths(1) },
                            enabled = YearMonth.from(selectedDate) < currentMonth
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "다음",
                                tint = if (YearMonth.from(selectedDate) < currentMonth) Color.White else Color.Gray
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

                    val completedDaysSet = remember(habitProgressMap, currentUser, selectedDate) {
                        val progress = habitProgressMap[currentUser?.id]
                        if (progress != null && YearMonth.parse(progress.year_month) == YearMonth.from(selectedDate)) {
                            progress.daily_progress.mapIndexedNotNull { index, c ->
                                if (c == '1') index + 1 else null
                            }.toSet()
                        } else {
                            emptySet()
                        }
                    }

                    MonthlyCalendarGrid(
                        selectedDate = selectedDate,
                        tappedDate = tappedDate,
                        onDateTap = { date -> tappedDate = date },
                        hasRecord = { date ->
                            completedDaysSet.contains(date.dayOfMonth)
                        }
                    )
                } //달력 끝

                var isParticipantListExpanded by remember { mutableStateOf(true) }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isParticipantListExpanded = !isParticipantListExpanded }
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "참여자 (${members.size}명)",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { isParticipantListExpanded = !isParticipantListExpanded }) {
                        Icon(
                            imageVector = if (isParticipantListExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isParticipantListExpanded) "참여자 목록 축소" else "참여자 목록 확장",
                            tint = Color.White
                        )
                    }
                }

                // 랭킹 UI에 동적 데이터 적용
                Column(modifier = Modifier.animateContentSize()) {
                    if (isParticipantListExpanded) {
                        val totalDaysInMonth = YearMonth.from(selectedDate).lengthOfMonth()
                        rankingList.forEach { item ->
                            RankingItem(
                                name = item.member.nickname,
                                completedDays = item.completedDays,
                                totalDaysInMonth = totalDaysInMonth,
                                progress = item.progress,
                                animalId = item.member.animal
                            )
                        }
                    }
                }
            }
        }

        // 화면 하단 고정 버튼 로직 수정
        Button(
            onClick = { if (roomId != null) roomVm.completeTodayChallenge(roomId) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
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

        // ✅ 날짜 클릭 시 나타날 픽셀아트 다이얼로그
        if (tappedDate != null) {
            val completers = remember(tappedDate, members, habitProgressMap) {
                members.filter { member ->
                    val progress = habitProgressMap[member.user_id]
                    val isCorrectMonth = progress?.year_month == YearMonth.from(tappedDate).format(DateTimeFormatter.ofPattern("yyyy-MM"))
                    if (isCorrectMonth) {
                        progress?.daily_progress?.getOrNull(tappedDate!!.dayOfMonth - 1) == '1'
                    } else {
                        false
                    }
                }
            }

            CompletionStatusDialog(
                date = tappedDate!!,
                completers = completers,
                onDismiss = { tappedDate = null }
            )
        }

        // 방장 위임 다이얼로그
        if (showDelegateDialog) {
            val otherMembers = members.filter { it.user_id != currentUser?.id }
            DelegateAdminDialog(
                members = otherMembers,
                onDismiss = { showDelegateDialog = false },
                onConfirm = { newAdminId ->
                    roomId?.let { roomVm.delegateAdmin(it, newAdminId) }
                    showDelegateDialog = false
                }
            )
        }

        // 방 나가기 확인 다이얼로그
        if (showLeaveConfirmDialog) {
            ConfirmationDialog(
                title = "방 나가기",
                text = "정말로 이 방을 나가시겠습니까?",
                onDismiss = { showLeaveConfirmDialog = false },
                onConfirm = {
                    roomId?.let { roomVm.leaveStudyRoom(it) }
                    showLeaveConfirmDialog = false
                }
            )
        }
    }
}

// ✅ 픽셀아트 스타일로 재구성된 CompletionStatusDialog Composable
@Composable
fun CompletionStatusDialog(
    date: LocalDate,
    completers: List<StudyRoomMember>,
    onDismiss: () -> Unit
) {
    val dialogTitle = date.format(DateTimeFormatter.ofPattern("M월 d일")) + " 완료 멤버"

    // 픽셀아트 컨셉의 색상 팔레트
    val pixelDarkGreen = Color(0xFF33691E) // 어두운 녹색
    val pixelLightGreen = Color(0xFF8BC34A) // 밝은 녹색
    val pixelBrown = Color(0xFF795548)     // 갈색
    val pixelBorder = Color(0xFF212121)    // 진한 테두리 색
    val pixelText = Color(0xFFE0E0E0)      // 밝은 텍스트 색

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false) // 기본 다이얼로그 폭 사용 안함
    ) {
        Column(
            modifier = Modifier
                .width(300.dp) // 다이얼로그 폭 고정
                .clip(RoundedCornerShape(0.dp)) // 각진 모서리
                .background(pixelDarkGreen) // 기본 배경
                .border(4.dp, pixelBorder) // 진한 테두리
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 제목 (상단 강조)
            Text(
                text = dialogTitle,
                color = pixelLightGreen,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // 구분선
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(pixelBrown)
                    .padding(bottom = 12.dp)
            )

            // 멤버 목록
            if (completers.isEmpty()) {
                Text(
                    text = "이날 챌린지를 완료한 멤버가 없습니다.",
                    color = pixelText,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (completers.size > 5) 180.dp else (completers.size * 30).dp) // 최대 높이 설정
                        .clip(RoundedCornerShape(0.dp)) // 각진 모서리
                        .background(Color.Black.copy(alpha = 0.3f)) // 목록 배경
                        .border(2.dp, pixelBrown) // 목록 테두리
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(completers) { member ->
                        Text(
                            text = "🐾 ${member.nickname}", // 발바닥 아이콘 추가
                            color = pixelText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 닫기 버튼
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = pixelLightGreen,
                    contentColor = pixelDarkGreen
                ),
                shape = RoundedCornerShape(0.dp), // 각진 버튼
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .border(2.dp, pixelBorder) // 버튼 테두리
            ) {
                Text(
                    text = "닫기",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}


@Composable
fun RankingItem(
    name: String,
    completedDays: Int,
    totalDaysInMonth: Int,
    progress: Float,
    animalId: String?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        val animal = animalId?.let { AnimalsTable.byId(it) }
        val spriteData = animal?.let { SpriteMap.map[it] }

        Box(
            modifier = Modifier.size(56.dp),
            contentAlignment = Alignment.Center
        ) {
            if (spriteData != null) {
                val imageBitmap = ImageBitmap.imageResource(id = spriteData.idleRes)
                val frameWidth = imageBitmap.width / spriteData.idleCols
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawImage(
                        image = imageBitmap,
                        srcOffset = IntOffset.Zero,
                        srcSize = IntSize(frameWidth, imageBitmap.height),
                        dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                        filterQuality = FilterQuality.None
                    )
                }
            } else {
                // Fallback to the original rank display
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF676767)),
                    contentAlignment = Alignment.Center
                ) {
//                    Text(
//                        text = "$rank",
//                        color = Color.Black,
//                        fontSize = 24.sp,
//                        fontWeight = FontWeight.Bold
//                    )
                }
            }
        }


        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = name,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = "($completedDays / $totalDaysInMonth 일 완료)",
                fontSize = 14.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp),
                color = Color(0xFF4CAF50),
                trackColor = Color.DarkGray,
                strokeCap = StrokeCap.Butt,
                gapSize = 0.dp
            )
        }
    }
}

/**
 * 방장 위임 시 멤버를 선택하는 다이얼로그
 */
@Composable
fun DelegateAdminDialog(
    members: List<StudyRoomMember>,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedUserId by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("방장 위임하기") },
        text = {
            Column {
                Text("새로운 방장을 선택해주세요. 방장을 위임하면 회원님은 방에서 나가게 됩니다.")
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn {
                    items(members) { member ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { selectedUserId = member.user_id }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (selectedUserId == member.user_id),
                                onClick = { selectedUserId = member.user_id }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(member.nickname)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedUserId?.let { onConfirm(it) }
                },
                enabled = selectedUserId != null
            ) {
                Text("확인")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

/**
 * 작업을 재확인하는 공용 다이얼로그
 */
@Composable
fun ConfirmationDialog(
    title: String,
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = { Text(text = text) },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                }
            ) {
                Text("확인")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}