package com.malrang.pomodoro.ui.screen.studyroom.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.malrang.pomodoro.networkRepo.StudyRoomMember
import com.malrang.pomodoro.ui.PixelArtConfirmDialog

/**
 * 방장 위임 시 멤버를 선택하는 다이얼로그. PixelArtConfirmDialog를 사용합니다.
 */
@Composable
fun DelegateAdminDialog(
    members: List<StudyRoomMember>,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedUserId by remember { mutableStateOf<String?>(null) }

    PixelArtConfirmDialog(
        onDismissRequest = onDismiss,
        title = "방장 위임하기",
        confirmText = "확인",
        onConfirm = { selectedUserId?.let { onConfirm(it) } },
        confirmButtonEnabled = selectedUserId != null
    ) {
        Column {
            Text(
                text = "새로운 방장을 선택해주세요. 방장을 위임하면 회원님은 방에서 나가게 됩니다.",
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn {
                items(members) { member ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { selectedUserId = member.user_id }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedUserId == member.user_id),
                            onClick = { selectedUserId = member.user_id }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = member.nickname,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
