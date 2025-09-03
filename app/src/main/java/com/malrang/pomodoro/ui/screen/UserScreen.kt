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
import com.malrang.pomodoro.dataclass.ui.Screen
import com.malrang.pomodoro.networkRepo.StudyRoom
import com.malrang.pomodoro.networkRepo.StudyRoomMember
import com.malrang.pomodoro.networkRepo.User
import com.malrang.pomodoro.viewmodel.PomodoroViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserScreen(viewModel: PomodoroViewModel) {
    // State를 최상위에서 한 번만 collect 합니다.
    val uiState by viewModel.studyRoomUiState.collectAsState()
    val currentUser = uiState.currentUser
    val userStudyRooms = uiState.userStudyRooms

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("내 스터디룸") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.showScreen(Screen.Main) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                    }
                }
            )
        },
        floatingActionButton = {
            if (currentUser != null) {
                FloatingActionButton(onClick = { viewModel.showCreateStudyRoomDialog(true) }) {
                    Icon(Icons.Default.Add, contentDescription = "스터디룸 생성")
                }
            }
        }
    ) { paddingValues ->
        // ✅ [수정] currentUser의 상태에 따라 다른 화면을 보여줍니다.
        if (currentUser == null) {
            // 데이터 로딩 중임을 알리는 화면
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
                Text("사용자 정보를 불러오는 중...")
            }
        } else {
            // 데이터 로딩이 완료되면 기존 UI를 보여줍니다.
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                items(uiState.userStudyRooms) { room ->
                    StudyRoomItem(room = room, onClick = {
                        viewModel.onJoinStudyRoom(room)
                    })
                }
            }
        }


            // 스터디룸 생성 다이얼로그
        if (uiState.showCreateStudyRoomDialog) {
            CreateStudyRoomDialog(
                currentUser = currentUser,
                allAnimals = uiState.allAnimals,
                viewModel = viewModel,
                onDismiss = { viewModel.showCreateStudyRoomDialog(false) }
            )
        }

        // 스터디룸 참여(닉네임/동물 설정) 다이얼로그
        uiState.showJoinStudyRoomDialog?.let { room ->
            JoinStudyRoomDialog(
                room = room,
                currentUser = currentUser,
                allAnimals = uiState.allAnimals,
                viewModel = viewModel,
                onDismiss = { viewModel.dismissJoinStudyRoomDialog() }
            )
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
            Text(text = "매일 ${room.habitDays}일 습관", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateStudyRoomDialog(
    currentUser: User?,
    allAnimals: List<com.malrang.pomodoro.networkRepo.Animal>,
    viewModel: PomodoroViewModel,
    onDismiss: () -> Unit
) {
    var roomName by remember { mutableStateOf("") }
    var habitDays by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var selectedAnimal by remember { mutableStateOf<com.malrang.pomodoro.networkRepo.Animal?>(null) }
    var expanded by remember { mutableStateOf(false) }

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
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("사용할 닉네임") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
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
                    // 여기에 추가했던 Log.d가 이제는 보여야 합니다.
                    android.util.Log.d("StudyRoomDebug", "버튼 클릭됨!")

                    val userId = currentUser?.id ?: return@Button

                    val newRoom = StudyRoom(
                        id = UUID.randomUUID().toString(),
                        name = roomName,
                        habitDays = habitDays.toIntOrNull() ?: 0,
                        creatorId = userId
                    )

                    val newMember = StudyRoomMember(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        nickname = nickname,
                        animal = selectedAnimal?.id,
                        isAdmin = true // 방 생성자는 관리자
                    )

                    viewModel.createStudyRoomAndJoin(newRoom, newMember)
                },
//                enabled = roomName.isNotBlank() && habitDays.isNotBlank() && nickname.isNotBlank()
            ) {
                Text("생성 및 참여")
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
    currentUser: User?,
    allAnimals: List<com.malrang.pomodoro.networkRepo.Animal>,
    viewModel: PomodoroViewModel,
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
                    val userId = currentUser?.id ?: return@Button
                    val member = StudyRoomMember(
                        id = UUID.randomUUID().toString(),
                        studyRoomId = room.id,
                        userId = userId,
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

