package com.malrang.pomodoro.ui.screen.studyroom.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.malrang.pomodoro.networkRepo.StudyRoomMember
import com.malrang.pomodoro.networkRepo.User
import com.malrang.pomodoro.ui.PixelArtConfirmDialog
import com.malrang.pomodoro.viewmodel.StudyRoomViewModel
import java.util.UUID

@Composable
fun JoinStudyRoomDialog(
    room: StudyRoom,
    currentUser: User,
    viewModel: StudyRoomViewModel,
    onDismiss: () -> Unit
) {
    var nickname by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    PixelArtConfirmDialog(
        onDismissRequest = onDismiss,
        title = "${room.name} 프로필 설정",
        confirmText = "참여",
        confirmButtonEnabled = nickname.isNotBlank(),
        onConfirm = {
            val member = StudyRoomMember(
                id = UUID.randomUUID().toString(),
                study_room_id = room.id,
                user_id = currentUser.id,
                nickname = nickname,
            )
            viewModel.joinStudyRoom(member)
            onDismiss() // 참여하기 버튼 클릭 후 다이얼로그 닫기
        }
    ) {
        // PixelArtConfirmDialog의 content 영역에 들어갈 UI 구성
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
                singleLine = true // 한 줄로 제한
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}