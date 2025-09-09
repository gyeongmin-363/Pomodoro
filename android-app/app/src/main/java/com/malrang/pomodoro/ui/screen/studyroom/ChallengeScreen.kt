package com.malrang.pomodoro.ui.screen.studyroom

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malrang.pomodoro.dataclass.animalInfo.Animal
import com.malrang.pomodoro.networkRepo.User
import com.malrang.pomodoro.ui.screen.studyroom.dialog.CreateStudyRoomDialog
import com.malrang.pomodoro.ui.screen.studyroom.dialog.JoinStudyRoomDialog
import com.malrang.pomodoro.ui.theme.SetBackgroundImage
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

    // ✨ 검색 기능: 검색어 입력을 위한 상태 변수
    var searchQuery by remember { mutableStateOf("") }
    // ✨ 검색 기능: 검색 UI 활성화 상태를 위한 변수
    var isSearchActive by remember { mutableStateOf(false) }


    // FloatingActionButton은 Scaffold와 함께 사용하는 것이 일반적이지만,
    // 현재 구조를 유지하기 위해 Box로 감싸 화면 위에 표시되도록 합니다.
    Box(modifier = Modifier.fillMaxSize()) {
        SetBackgroundImage()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp)
            // ❌ [제거] 중첩 스크롤을 유발하는 원인이므로 제거합니다.
            // .verticalScroll(rememberScrollState())
        ){
            // ✨ 검색 기능: 상단 바 UI를 isSearchActive 상태에 따라 동적으로 변경
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSearchActive) {
                    // ✨ 검색 활성화 상태 UI: 검색창 표시
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("챌린지룸 이름으로 검색...") },
                        textStyle = TextStyle(color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.Gray,
                            cursorColor = Color.White,
                            focusedLabelColor = Color.White,
                            unfocusedLabelColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.DarkGray,
                            unfocusedContainerColor = Color.DarkGray,
                        ),
                        singleLine = true
                    )
                } else {
                    // ✨ 검색 비활성화 상태 UI: 제목 표시
                    Text("챌린지룸", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {

                    // 👇 '내 챌린지룸' 탭일 때만 삭제 버튼 표시
                    if (!isSearchActive && selectedTabIndex == 0) {
                        IconButton(onClick = onNavigateToDelete) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "챌린지룸 삭제",
                                tint = Color.White
                            )
                        }
                    }

                    // ✨ 검색 아이콘: 클릭 시 isSearchActive 상태를 토글. 아이콘 모양도 상태에 따라 변경
                    IconButton(onClick = {
                        isSearchActive = !isSearchActive
                        if (!isSearchActive) {
                            searchQuery = "" // 검색창이 닫힐 때 검색어 초기화
                        }
                    }) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = if (isSearchActive) "검색 닫기" else "검색 열기",
                            tint = Color.White
                        )
                    }

                    // ✨ 검색 중이 아닐 때만 삭제, 뒤로가기 버튼 표시
                    if (!isSearchActive) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "돌아가기",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
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
                    Text(
                        text = "사용자 정보를 불러오는 중...",
                        modifier = Modifier.padding(top = 60.dp),
                        color = Color.LightGray
                    )
                }
            }
            else if(isLoading){
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = "챌린지룸 불러오는 중...",
                        modifier = Modifier.padding(top = 60.dp),
                        color = Color.LightGray
                    )
                }
            }
            else {
                // ✅ [변경] 선택된 탭에 따라 다른 컨텐츠를 보여줌
                when (selectedTabIndex) {
                    // "내가 생성한 챌린지룸" 탭
                    0 -> {
                        // ✨ 검색 기능: 생성한 챌린지룸 목록을 검색어로 필터링
                        val filteredCreatedRooms = createdRooms.filter {
                            it.name.contains(searchQuery, ignoreCase = true)
                        }

                        if (createdRooms.isEmpty()) {
                            EmptyStateMessage(
                                message = "챌린지룸이 없습니다.\n+버튼을 눌러 챌린지룸을 만들어보세요!",
                                // ❗ [수정] weight(1f)를 적용하여 남은 공간을 모두 차지하도록 합니다.
                                modifier = Modifier.weight(1f)
                            )
                        } else if (searchQuery.isNotEmpty() && filteredCreatedRooms.isEmpty()) {
                            // ✨ 검색 기능: 검색 결과가 없을 때 메시지 표시
                            EmptyStateMessage(
                                message = "검색 결과가 없습니다.",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        else {
                            LazyColumn(
                                modifier = Modifier
                                    // ❗ [수정] fillMaxSize() 대신 weight(1f)를 사용해 남은 공간을 모두 차지하도록 합니다.
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                items(filteredCreatedRooms) { room ->
                                    StudyRoomItem(room = room, onClick = {
                                        roomVM.onJoinStudyRoom(room)
                                    })
                                }
                                item {
                                    Spacer(Modifier.height(16.dp))
                                }
                            }
                        }
                    }
                    // "내가 참여한 챌린지룸" 탭
                    1 -> {
                        // ✨ 검색 기능: 참여한 챌린지룸 목록을 검색어로 필터링
                        val filteredJoinedRooms = joinedRooms.filter {
                            it.name.contains(searchQuery, ignoreCase = true)
                        }

                        if (joinedRooms.isEmpty()) {
                            EmptyStateMessage(
                                message = "참여한 챌린지룸이 없습니다.",
                                // ❗ [수정] weight(1f)를 적용하여 남은 공간을 모두 차지하도록 합니다.
                                modifier = Modifier.weight(1f)
                            )
                        } else if (searchQuery.isNotEmpty() && filteredJoinedRooms.isEmpty()) {
                            // ✨ 검색 기능: 검색 결과가 없을 때 메시지 표시
                            EmptyStateMessage(
                                message = "검색 결과가 없습니다.",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        else {
                            LazyColumn(
                                modifier = Modifier
                                    // ❗ [수정] fillMaxSize() 대신 weight(1f)를 사용해 남은 공간을 모두 차지하도록 합니다.
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                items(filteredJoinedRooms) { room ->
                                    StudyRoomItem(room = room, onClick = {
                                        roomVM.onJoinStudyRoom(room)
                                    })
                                }
                                item {
                                    Spacer(Modifier.height(16.dp))
                                }
                            }
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