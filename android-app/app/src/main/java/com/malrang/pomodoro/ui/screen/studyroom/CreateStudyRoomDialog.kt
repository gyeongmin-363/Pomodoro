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
    var roomInform by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("새 챌린지룸 생성") },
        text = {
            Column {
                OutlinedTextField(
                    value = roomName,
                    onValueChange = {
                        if (it.length <= 20) { // 20자 이하로 제한
                            roomName = it
                        }
                    },
                    label = { Text("챌린지룸 이름 (${roomName.length}/20)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true // 한 줄로 제한
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = roomInform,
                    onValueChange = {
                        // 줄바꿈 문자의 개수와 텍스트 길이를 동시에 제한
                        if (it.count { char -> char == '\n' } < 10 && it.length <= 100) {
                            roomInform = it
                        }
                    },
                    label = { Text("설명 (${roomInform.length}/100)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 1,
                    maxLines = 10 // 최대 10줄
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newRoom = StudyRoom(
                        id = UUID.randomUUID().toString(),
                        name = roomName,
                        inform = roomInform,
                        creator_id = currentUser.id
                    )
                    viewModel.createStudyRoom(newRoom)
                },
                enabled = roomName.isNotBlank() && roomName.length <= 20 && roomInform.length <= 100
            ) {
                Text("생성")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}
