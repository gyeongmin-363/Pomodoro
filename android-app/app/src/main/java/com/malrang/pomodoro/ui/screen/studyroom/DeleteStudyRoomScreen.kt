// com/malrang/pomodoro/ui/screen/studyroom/DeleteStudyRoomScreen.kt

package com.malrang.pomodoro.ui.screen.studyroom

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.malrang.pomodoro.networkRepo.StudyRoom
import com.malrang.pomodoro.ui.theme.dialogColor
import com.malrang.pomodoro.viewmodel.StudyRoomViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteStudyRoomScreen(
    roomVM: StudyRoomViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by roomVM.studyRoomUiState.collectAsState()
    val deletableRooms = uiState.deletableStudyRooms
    val isLoading = uiState.isLoading
    var selectedRoomIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    // 화면 진입 시 삭제 가능한 챌린지룸 목록을 불러옵니다.
    LaunchedEffect(Unit) {
        roomVM.loadDeletableStudyRooms()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("챌린지룸 삭제", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = dialogColor
                )
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    roomVM.deleteStudyRooms(selectedRoomIds.toList())
                    onNavigateBack() // 삭제 후 이전 화면으로 돌아갑니다.
                },
                enabled = selectedRoomIds.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("선택한 챌린지룸 삭제 (${selectedRoomIds.size})")
            }
        },
        containerColor = dialogColor
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (deletableRooms.isEmpty()) {
                EmptyStateMessage(
                    message = "삭제 가능한 챌린지룸이 없습니다.\n(멤버가 없는 방만 삭제할 수 있습니다)",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    items(deletableRooms, key = { it.id }) { room ->
                        DeletableStudyRoomItem(
                            room = room,
                            isSelected = room.id in selectedRoomIds,
                            onToggleSelection = {
                                selectedRoomIds = if (room.id in selectedRoomIds) {
                                    selectedRoomIds - room.id
                                } else {
                                    selectedRoomIds + room.id
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DeletableStudyRoomItem(
    room: StudyRoom,
    isSelected: Boolean,
    onToggleSelection: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleSelection)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggleSelection() }
        )
        // 기존 StudyRoomItem UI를 재사용합니다.
        // 클릭 이벤트는 Row에서 처리하므로 Box로 감싸 클릭을 막습니다.
        Box(modifier = Modifier.weight(1f)) {
            StudyRoomItem(room = room, onClick = {onToggleSelection()})
        }
    }
}