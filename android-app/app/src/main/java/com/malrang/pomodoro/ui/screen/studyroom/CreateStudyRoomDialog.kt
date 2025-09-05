package com.malrang.pomodoro.ui.screen.studyroom

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.malrang.pomodoro.networkRepo.StudyRoom
import com.malrang.pomodoro.networkRepo.User
import com.malrang.pomodoro.viewmodel.StudyRoomViewModel
import java.util.UUID

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

