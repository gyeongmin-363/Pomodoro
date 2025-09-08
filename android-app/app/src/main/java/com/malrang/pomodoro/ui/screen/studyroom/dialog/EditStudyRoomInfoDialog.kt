package com.malrang.pomodoro.ui.screen.studyroom.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.malrang.pomodoro.networkRepo.StudyRoom
import com.malrang.pomodoro.ui.PixelArtConfirmDialog
import com.malrang.pomodoro.viewmodel.StudyRoomViewModel

/**
 * 방 정보 수정을 위한 다이얼로그. PixelArtConfirmDialog를 사용합니다.
 */
@Composable
fun EditStudyRoomInfoDialog(
    room: StudyRoom,
    viewModel: StudyRoomViewModel,
    onDismiss: () -> Unit
) {
    var roomName by remember { mutableStateOf(room.name) }
    var roomInform by remember { mutableStateOf(room.inform ?: "") }

    PixelArtConfirmDialog(
        onDismissRequest = onDismiss,
        title = "챌린지룸 정보 수정",
        confirmText = "수정",
        onConfirm = {
            val updatedRoom = room.copy(
                name = roomName,
                inform = roomInform
            )
            viewModel.updateStudyRoom(room.id, updatedRoom)
            onDismiss()
        },
        confirmButtonEnabled = roomName.isNotBlank() && roomName.length <= 20 && roomInform.length <= 100
    ) {
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
    }
}

