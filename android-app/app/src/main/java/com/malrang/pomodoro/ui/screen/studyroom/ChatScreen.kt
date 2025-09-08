package com.malrang.pomodoro.ui.screen.studyroom

import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.malrang.pomodoro.networkRepo.ChatMessage
import com.malrang.pomodoro.viewmodel.StudyRoomViewModel
import kotlinx.coroutines.launch


@Composable
fun ChatScreen(
    studyRoomId: String,
    studyRoomViewModel: StudyRoomViewModel,
) {
    val chatMessages by studyRoomViewModel.chatMessages.collectAsState()
    val uiState by studyRoomViewModel.studyRoomUiState.collectAsState()
    val currentUser = uiState.currentUser
    val membersMap = uiState.currentRoomMembers.associate { it.user_id to it.nickname }
    var messageText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // --- 1. 스낵바 및 UI 이벤트 처리 설정 ---
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // ViewModel에서 오는 UI 이벤트를 수신하여 스낵바를 표시
    LaunchedEffect(Unit) {
        studyRoomViewModel.uiEvents.collect { eventMessage ->
            scope.launch {
                snackbarHostState.showSnackbar(message = eventMessage)
            }
        }
    }
    // --- 설정 끝 ---

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> selectedImageUri = uri }
    )
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> selectedImageUri = uri }
    )

    LaunchedEffect(studyRoomId) {
        studyRoomViewModel.subscribeToMessages(studyRoomId)
    }

    DisposableEffect(Unit) {
        onDispose {
            studyRoomViewModel.disSubscribeMessage()
        }
    }

    // Scaffold를 사용하여 스낵바를 표시할 공간 확보
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                reverseLayout = true
            ) {
                if (uiState.isChatLoading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                } else {
                    // --- 3. 채팅 메시지에 이미지 표시 로직 추가 ---
                    items(chatMessages.reversed()) { message ->
                        val isMyMessage = message.user_id == currentUser?.id
                        MessageBubble(message = message, isMyMessage = isMyMessage)
                    }
                    // --- 로직 추가 끝 ---
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                if (selectedImageUri != null) {
                    Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Selected Image",
                            modifier = Modifier
                                .height(150.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        } else {
                            galleryLauncher.launch("image/*")
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = "Attach Photo"
                        )
                    }

                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("메시지 입력") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            // --- 2. 이미지/텍스트 메시지 전송 로직 수정 ---
                            currentUser?.id?.let { userId ->
                                val nickname = membersMap[userId].orEmpty()
                                if (selectedImageUri != null) {
                                    studyRoomViewModel.sendChatWithImage(
                                        context = context,
                                        studyRoomId = studyRoomId,
                                        userId = userId,
                                        message = messageText,
                                        nickname = nickname,
                                        imageUri = selectedImageUri!!
                                    )
                                } else {
                                    if (messageText.isNotBlank()) {
                                        studyRoomViewModel.sendChatMessage(
                                            studyRoomId,
                                            userId,
                                            messageText,
                                            nickname
                                        )
                                    }
                                }
                                messageText = ""
                                selectedImageUri = null
                            }
                            // --- 로직 수정 끝 ---
                        },
                        enabled = (messageText.isNotBlank() || selectedImageUri != null) && !uiState.isChatLoading
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = "Send")
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage, isMyMessage: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = if (isMyMessage) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.8f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isMyMessage) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.nickname,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))

                // 이미지가 있을 경우 표시
                message.image_url?.let { imageUrl ->
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Chat image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    // 이미지와 텍스트가 모두 있을 경우 간격 추가
                    if (message.message.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // 텍스트 메시지가 있을 경우 표시
                if (message.message.isNotBlank()) {
                    Text(
                        text = message.message,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}