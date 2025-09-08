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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.malrang.pomodoro.R
import com.malrang.pomodoro.dataclass.animalInfo.Animal
import com.malrang.pomodoro.dataclass.animalInfo.AnimalsTable
import com.malrang.pomodoro.dataclass.sprite.SpriteMap
import com.malrang.pomodoro.networkRepo.StudyRoom
import com.malrang.pomodoro.networkRepo.StudyRoomMember
import com.malrang.pomodoro.networkRepo.StudyRoomMemberWithProgress
import com.malrang.pomodoro.ui.PixelArtConfirmDialog
import com.malrang.pomodoro.ui.screen.stats.MonthlyCalendarGrid
import com.malrang.pomodoro.ui.screen.studyroom.dialog.CompletionStatusDialog
import com.malrang.pomodoro.ui.screen.studyroom.dialog.ConfirmationDialog
import com.malrang.pomodoro.ui.screen.studyroom.dialog.DelegateAdminDialog
import com.malrang.pomodoro.ui.screen.studyroom.dialog.EditMyInfoDialog
import com.malrang.pomodoro.ui.screen.studyroom.dialog.EditStudyRoomInfoDialog
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
    collectAnimal : Set<Animal>,
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
    var showEditMyInfoDialog by remember { mutableStateOf(false) }
    var showEditRoomInfoDialog by remember { mutableStateOf(false) }
    var showCompleteConfirmDialog by remember { mutableStateOf(false) }


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

                            // 내 정보 수정
                            DropdownMenuItem(
                                text = { Text("내 정보 수정") },
                                onClick = {
                                    showEditMyInfoDialog = true
                                    menuExpanded = false
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Person,
                                        contentDescription = "내 정보 수정"
                                    )
                                }
                            )

                            // 방 정보 수정 (방장일 경우)
                            if (isCreator) {
                                DropdownMenuItem(
                                    text = { Text("방 정보 수정") },
                                    onClick = {
                                        showEditRoomInfoDialog = true
                                        menuExpanded = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "방 정보 수정"
                                        )
                                    }
                                )
                            }

                            HorizontalDivider()

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
                                            painterResource(R.drawable.ic_user_attributes_24px),
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
            onClick = { showCompleteConfirmDialog = true },
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

        val myMemberInfo = remember(members, currentUser) {
            members.find { it.user_id == currentUser?.id }
        }

        if (showEditMyInfoDialog && myMemberInfo != null) {
            // JoinStudyRoomDialog 형식을 따르기 위해 collectedAnimals 파라미터가 필요합니다.
            // 현재 화면에서는 사용자가 수집한 동물 목록 데이터가 없으므로 빈 목록을 전달합니다.
            // 실제 구현 시에는 이 부분에 사용자 동물 데이터를 전달해야 합니다.
            EditMyInfoDialog(
                member = myMemberInfo,
                collectedAnimals = collectAnimal, // 빈 Set 전달
                viewModel = roomVm,
                onDismiss = { showEditMyInfoDialog = false }
            )
        }

        if (showEditRoomInfoDialog && room != null) {
            EditStudyRoomInfoDialog(
                room = room,
                viewModel = roomVm,
                onDismiss = { showEditRoomInfoDialog = false }
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

        // '오늘 챌린지 완료하기' 확인 다이얼로그
        if (showCompleteConfirmDialog) {
            PixelArtConfirmDialog(
                onDismissRequest = { showCompleteConfirmDialog = false },
                title = "챌린지 완료",
                confirmText = "완료",
                onConfirm = {
                    roomId?.let { roomVm.completeTodayChallenge(it) }
                    showCompleteConfirmDialog = false
                }
            ) {
                Text(
                    text = "오늘의 챌린지를 완료하시겠습니까?\n완료하면 취소할 수 없습니다.",
                    color = Color.White,
                    textAlign = TextAlign.Center
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