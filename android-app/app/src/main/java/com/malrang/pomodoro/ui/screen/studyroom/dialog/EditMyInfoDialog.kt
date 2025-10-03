package com.malrang.pomodoro.ui.screen.studyroom.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.malrang.pomodoro.networkRepo.StudyRoomMember
import com.malrang.pomodoro.ui.PixelArtConfirmDialog
import com.malrang.pomodoro.viewmodel.StudyRoomViewModel

/**
 * 챌린지룸 내 내 정보(닉네임, 동물) 수정을 위한 다이얼로그. PixelArtConfirmDialog를 사용합니다.
 */
@Composable
fun EditMyInfoDialog(
    member: StudyRoomMember,
    viewModel: StudyRoomViewModel,
    onDismiss: () -> Unit
) {
    var nickname by remember { mutableStateOf(member.nickname) }
    var expanded by remember { mutableStateOf(false) }

    PixelArtConfirmDialog(
        onDismissRequest = onDismiss,
        title = "내 정보 수정",
        confirmText = "수정",
        onConfirm = {
            member.study_room_id?.let {
                viewModel.updateMyInfoInRoom(
                    memberId = member.id,
                    studyRoomId = it,
                    newNickname = nickname,
                )
            }
            onDismiss()
        },
        confirmButtonEnabled = nickname.isNotBlank()
    ) {
        Column {
            OutlinedTextField(
                value = nickname,
                onValueChange = {
                    if (it.length <= 10) { // 10자 이하로 제한
                        nickname = it
                    }
                },
                label = { Text("닉네임 (${nickname.length}/10)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true, // 한 줄로 제한
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.Gray,
                    cursorColor = Color.White,
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
        }
    }
}
