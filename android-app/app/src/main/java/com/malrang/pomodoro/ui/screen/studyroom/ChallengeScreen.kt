package com.malrang.pomodoro.ui.screen.studyroom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malrang.pomodoro.dataclass.animalInfo.Animal
import com.malrang.pomodoro.networkRepo.User
import com.malrang.pomodoro.ui.theme.backgroundColor
import com.malrang.pomodoro.viewmodel.AuthViewModel
import com.malrang.pomodoro.viewmodel.StudyRoomViewModel
import kotlinx.serialization.json.jsonPrimitive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengeScreen(
    collectAnimal : Set<Animal>,
    authVM: AuthViewModel,
    roomVM: StudyRoomViewModel,
    inviteStudyRoomId: String?,
    onNavigateBack: () -> Unit,
    onNavigateToDelete: () -> Unit
) {
    val authState by authVM.uiState.collectAsState()

    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthViewModel.AuthState.Authenticated -> {
                state.user?.let { userInfo ->
                    val userName = userInfo.userMetadata?.get("name")?.jsonPrimitive?.content ?: "사용자"
                    val appUser = User(id = userInfo.id, name = userName)
                    roomVM.onUserAuthenticated(appUser)
                }
            }
            is AuthViewModel.AuthState.NotAuthenticated -> {
                roomVM.onUserNotAuthenticated()
            }
            else -> { /* 로딩, 에러 등 */ }
        }
    }

    LaunchedEffect(inviteStudyRoomId) {
        inviteStudyRoomId?.let {
            roomVM.handleInviteLink(it)
        }
    }

    val uiState by roomVM.studyRoomUiState.collectAsState()
    val currentUser = uiState.currentUser
    val createdRooms = uiState.createdStudyRooms
    val joinedRooms = uiState.joinedStudyRooms
    val isLoading = uiState.isLoading

    // ✅ [추가] 탭 상태 관리를 위한 변수
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("내 챌린지룸", "참여 챌린지룸")

    // FloatingActionButton은 Scaffold와 함께 사용하는 것이 일반적이지만,
    // 현재 구조를 유지하기 위해 Box로 감싸 화면 위에 표시되도록 합니다.
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(top = 16.dp)
            // ❌ [제거] 중첩 스크롤을 유발하는 원인이므로 제거합니다.
            // .verticalScroll(rememberScrollState())
        ){
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("챌린지룸", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 👇 '내 챌린지룸' 탭일 때만 삭제 버튼 표시
                    if (selectedTabIndex == 0) {
                        IconButton(onClick = onNavigateToDelete) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "챌린지룸 삭제",
                                tint = Color.White
                            )
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
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = backgroundColor,
                contentColor = Color.White
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(text = title, style = MaterialTheme.typography.bodyLarge) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (currentUser == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                    Text("사용자 정보를 불러오는 중...", modifier = Modifier.padding(top = 60.dp))
                }
            }
            else if(isLoading){
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                    Text("챌린지룸 불러오는 중...", modifier = Modifier.padding(top = 60.dp))
                }
            }
            else {
                // ✅ [변경] 선택된 탭에 따라 다른 컨텐츠를 보여줌
                when (selectedTabIndex) {
                    // "내가 생성한 챌린지룸" 탭
                    0 -> {
                        if (createdRooms.isNotEmpty()) {
                            LazyColumn(
                                modifier = Modifier
                                    // ❗ [수정] fillMaxSize() 대신 weight(1f)를 사용해 남은 공간을 모두 차지하도록 합니다.
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                items(createdRooms) { room ->
                                    StudyRoomItem(room = room, onClick = {
                                        roomVM.onJoinStudyRoom(room)
                                    })
                                }
                                item {
                                    Spacer(Modifier.height(16.dp))
                                }
                            }
                        } else {
                            EmptyStateMessage(
                                message = "생성한 챌린지룸이 없습니다.\nFAB를 눌러 챌린지룸을 만들어보세요!",
                                // ❗ [수정] weight(1f)를 적용하여 남은 공간을 모두 차지하도록 합니다.
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    // "내가 참여한 챌린지룸" 탭
                    1 -> {
                        if (joinedRooms.isNotEmpty()) {
                            LazyColumn(
                                modifier = Modifier
                                    // ❗ [수정] fillMaxSize() 대신 weight(1f)를 사용해 남은 공간을 모두 차지하도록 합니다.
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                items(joinedRooms) { room ->
                                    StudyRoomItem(room = room, onClick = {
                                        roomVM.onJoinStudyRoom(room)
                                    })
                                }
                                item {
                                    Spacer(Modifier.height(16.dp))
                                }
                            }
                        } else {
                            EmptyStateMessage(
                                message = "참여한 챌린지룸이 없습니다.",
                                // ❗ [수정] weight(1f)를 적용하여 남은 공간을 모두 차지하도록 합니다.
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        if (currentUser != null) {
            FloatingActionButton(
                onClick = { roomVM.showCreateStudyRoomDialog(true) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(32.dp) // 화면 가장자리에 적절한 여백 추가
            ) {
                Icon(Icons.Default.Add, contentDescription = "챌린지룸 생성")
            }
        }
    }


    // 챌린지룸 생성 다이얼로그
    if (uiState.showCreateStudyRoomDialog) {
        currentUser?.let { user ->
            CreateStudyRoomDialog(
                currentUser = user,
                viewModel = roomVM,
                onDismiss = { roomVM.showCreateStudyRoomDialog(false) }
            )
        }
    }

    // 챌린지룸 참여 다이얼로그
    uiState.showJoinStudyRoomDialog?.let { room ->
        currentUser?.let { user ->
            JoinStudyRoomDialog(
                room = room,
                currentUser = user,
                collectedAnimals = collectAnimal,
                viewModel = roomVM,
                onDismiss = { roomVM.dismissJoinStudyRoomDialog() }
            )
        }
    }
}


// ✅ [추가] 룸이 비어있을 때 표시할 메시지 Composable
@Composable
fun EmptyStateMessage(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            // ❗ [수정] 이 Composable을 사용하는 곳에서 크기를 결정하도록 fillMaxSize()를 제거합니다.
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}