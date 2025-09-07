// android-app/app/src/main/java/com/malrang/pomodoro/ui/screen/studyroom/ChatScreen.kt

package com.malrang.pomodoro.ui.screen.studyroom

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext // LocalContext는 이제 필요 없습니다.
import androidx.compose.ui.unit.dp
// import androidx.lifecycle.viewmodel.compose.viewModel // viewModel() 함수를 사용하지 않으므로 제거합니다.
import com.malrang.pomodoro.viewmodel.AuthViewModel
import com.malrang.pomodoro.viewmodel.StudyRoomViewModel


@Composable
fun ChatScreen(
    studyRoomId: String,
    studyRoomViewModel: StudyRoomViewModel, // ViewModel을 매개변수로 받습니다.
) {
    // ViewModel의 StateFlow를 구독하여 chatMessages 상태를 가져옵니다.
    val chatMessages by studyRoomViewModel.chatMessages.collectAsState()
    val uiState by studyRoomViewModel.studyRoomUiState.collectAsState()
    val currentUser = uiState.currentUser
    val membersMap = uiState.currentRoomMembers.associate { it.user_id to it.nickname }
    var messageText by remember { mutableStateOf("") }

    // ChatScreen이 보일 때 메시지 구독을 시작합니다.
    LaunchedEffect(studyRoomId) {
        studyRoomViewModel.subscribeToMessages(studyRoomId)
    }

    DisposableEffect(Unit) {
        onDispose {
            studyRoomViewModel.disSubscribeMessage()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            reverseLayout = true // 채팅은 아래부터 쌓이도록 설정
        ) {
            if(uiState.isChatLoading){
                item{
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            else {
                items(chatMessages.reversed()) { message -> // 최신 메시지가 아래에 보이도록 reversed()
                    // TODO: 채팅 UI를 더 예쁘게 꾸며보세요. (예: 보낸 사람/받는 사람 구분)
                    Text(
                        text = "${message.nickname}: ${message.message}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier.weight(1f),
                label = { Text("메시지 입력") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    currentUser?.id?.let { userId ->
                        if (messageText.isNotBlank()) {
                            studyRoomViewModel.sendChatMessage(studyRoomId, userId, messageText, membersMap[userId].toString())
                            messageText = ""
                        }
                    }
                },
                enabled = messageText.isNotBlank() && !uiState.isChatLoading
            ) {
                Text("전송")
            }
        }
    }
}