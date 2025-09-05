package com.malrang.pomodoro.ui.screen.studyroom

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.malrang.pomodoro.networkRepo.User
import com.malrang.pomodoro.viewmodel.AuthViewModel
import com.malrang.pomodoro.viewmodel.StudyRoomViewModel
import kotlinx.serialization.json.jsonPrimitive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyRoomScreen(
    authVM: AuthViewModel,
    roomVM: StudyRoomViewModel,
    inviteStudyRoomId: String?,
    onNavigateBack: () -> Unit // ✅ [추가] 뒤로가기 콜백 함수
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


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("내 스터디룸") },
                navigationIcon = {
                    // ✅ [수정] 뒤로가기 콜백 호출
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                    }
                }
            )
        },
        floatingActionButton = {
            if (currentUser != null) {
                FloatingActionButton(onClick = { roomVM.showCreateStudyRoomDialog(true) }) {
                    Icon(Icons.Default.Add, contentDescription = "스터디룸 생성")
                }
            }
        }
    ) { paddingValues ->
        if (currentUser == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
                Text("사용자 정보를 불러오는 중...")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {
                // 내가 생성한 스터디룸 섹션
                if (createdRooms.isNotEmpty()) {
                    item {
                        Text(
                            text = "내가 생성한 스터디룸",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                    }
                    items(createdRooms) { room ->
                        StudyRoomItem(room = room, onClick = {
                            roomVM.onJoinStudyRoom(room)
                        })
                    }
                }

                // 생성한 룸과 참여한 룸이 모두 있을 경우 구분선 표시
                if (createdRooms.isNotEmpty() && joinedRooms.isNotEmpty()) {
                    item {
                        Divider(modifier = Modifier.padding(vertical = 16.dp))
                    }
                }

                // 내가 참여한 스터디룸 섹션
                if (joinedRooms.isNotEmpty()) {
                    item {
                        Text(
                            text = "내가 참여한 스터디룸",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                        )
                    }
                    items(joinedRooms) { room ->
                        StudyRoomItem(room = room, onClick = {
                            roomVM.onJoinStudyRoom(room)
                        })
                    }
                }
            }
        }

        // 스터디룸 생성 다이얼로그
        if (uiState.showCreateStudyRoomDialog) {
            currentUser?.let { user ->
                CreateStudyRoomDialog(
                    currentUser = user,
                    viewModel = roomVM,
                    onDismiss = { roomVM.showCreateStudyRoomDialog(false) }
                )
            }
        }

        // 스터디룸 참여 다이얼로그
        uiState.showJoinStudyRoomDialog?.let { room ->
            currentUser?.let { user ->
                JoinStudyRoomDialog(
                    room = room,
                    currentUser = user,
                    allAnimals = uiState.allAnimals,
                    viewModel = roomVM,
                    onDismiss = { roomVM.dismissJoinStudyRoomDialog() }
                )
            }
        }
    }
}



