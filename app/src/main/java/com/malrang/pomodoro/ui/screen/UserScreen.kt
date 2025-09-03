package com.malrang.pomodoro.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.malrang.pomodoro.networkRepo.StudyRoom
import com.malrang.pomodoro.networkRepo.StudyRoomMember
import com.malrang.pomodoro.networkRepo.User
import com.malrang.pomodoro.viewmodel.AuthViewModel
import com.malrang.pomodoro.viewmodel.StudyRoomViewModel
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserScreen(
    authVM: AuthViewModel,
    roomVM: StudyRoomViewModel,
    inviteStudyRoomId: String?
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
    val userStudyRooms = uiState.userStudyRooms

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("내 스터디룸") },
                navigationIcon = {
                    IconButton(onClick = {
//                        viewModel.showScreen(Screen.Main)
                    }) {
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
            // ✅ [수정] 생성한 룸과 참여한 룸을 분리
            val (createdRooms, joinedRooms) = userStudyRooms.partition { it.creator_id == currentUser.id }

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

@Composable
fun StudyRoomItem(room: StudyRoom, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = room.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "매일 ${room.habit_days}일 습관", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }
    }
}

// ✅ [수정] CreateStudyRoomDialog에서 닉네임 및 동물 선택 UI 제거
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateStudyRoomDialog(
    currentUser: User,
    viewModel: StudyRoomViewModel,
    onDismiss: () -> Unit
) {
    var roomName by remember { mutableStateOf("") }
    var habitDays by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("새 스터디룸 생성") },
        text = {
            Column {
                OutlinedTextField(
                    value = roomName,
                    onValueChange = { roomName = it },
                    label = { Text("스터디룸 이름") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = habitDays,
                    onValueChange = { habitDays = it },
                    label = { Text("습관 일수 (예: 30)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newRoom = StudyRoom(
                        id = UUID.randomUUID().toString(),
                        name = roomName,
                        habit_days = habitDays.toIntOrNull() ?: 0,
                        creator_id = currentUser.id
                    )
                    viewModel.createStudyRoom(newRoom)
                },
                enabled = roomName.isNotBlank() && habitDays.isNotBlank()
            ) {
                Text("생성") // 버튼 텍스트 변경
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinStudyRoomDialog(
    room: StudyRoom,
    currentUser: User,
    allAnimals: List<com.malrang.pomodoro.networkRepo.Animal>,
    viewModel: StudyRoomViewModel,
    onDismiss: () -> Unit
) {
    var nickname by remember { mutableStateOf("") }
    var selectedAnimal by remember { mutableStateOf<com.malrang.pomodoro.networkRepo.Animal?>(null) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${room.name}\n프로필 설정") },
        text = {
            Column {
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("닉네임") }
                )
                Spacer(modifier = Modifier.height(16.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        modifier = Modifier.menuAnchor(),
                        readOnly = true,
                        value = selectedAnimal?.name ?: "동물 선택 (선택사항)",
                        onValueChange = {},
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        allAnimals.forEach { animal ->
                            DropdownMenuItem(
                                text = { Text(animal.name ?: "이름 없음") },
                                onClick = {
                                    selectedAnimal = animal
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val member = StudyRoomMember(
                        id = UUID.randomUUID().toString(),
                        study_room_id = room.id,
                        user_id = currentUser.id,
                        nickname = nickname,
                        animal = selectedAnimal?.id
                    )
                    viewModel.joinStudyRoom(member)
                },
                enabled = nickname.isNotBlank()
            ) {
                Text("참여하기")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("다음에 하기")
            }
        }
    )
}