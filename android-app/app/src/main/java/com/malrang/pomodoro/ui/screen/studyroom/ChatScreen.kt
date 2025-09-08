package com.malrang.pomodoro.ui.screen.studyroom

import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
    // --- 1. 전체 화면 이미지 표시를 위한 상태 추가 ---
    var fullScreenImageUrl by remember { mutableStateOf<String?>(null) }


    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        studyRoomViewModel.uiEvents.collect { eventMessage ->
            scope.launch {
                snackbarHostState.showSnackbar(message = eventMessage)
            }
        }
    }

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

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
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
                        items(chatMessages.reversed()) { message ->
                            val isMyMessage = message.user_id == currentUser?.id
                            // --- 2. 이미지 클릭 콜백을 MessageBubble에 전달 ---
                            MessageBubble(
                                message = message,
                                isMyMessage = isMyMessage,
                                onImageClick = { imageUrl ->
                                    fullScreenImageUrl = imageUrl
                                }
                            )
                        }
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    if (selectedImageUri != null) {
                        // --- 수정된 부분: 이미지 미리보기 ---
                        Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "Selected Image",
                                modifier = Modifier
                                    .height(150.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            // --- X 버튼 추가 ---
                            IconButton(
                                onClick = { selectedImageUri = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove selected image",
                                    tint = Color.White
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
                            },
                            enabled = (messageText.isNotBlank() || selectedImageUri != null) && !uiState.isChatLoading
                        ) {
                            Icon(imageVector = Icons.Default.Send, contentDescription = "Send")
                        }
                    }
                }
            }
            // --- 3. 전체 화면 이미지 표시 로직 추가 ---
            if (fullScreenImageUrl != null) {
                FullScreenImage(
                    imageUrl = fullScreenImageUrl!!,
                    onDismiss = { fullScreenImageUrl = null }
                )
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    isMyMessage: Boolean,
    onImageClick: (String) -> Unit // --- 이미지 클릭 이벤트를 처리할 콜백 추가 ---
) {
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

                message.image_url?.let { imageUrl ->
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Chat image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onImageClick(imageUrl) }, // --- 이미지 클릭 시 콜백 호출 ---
                        contentScale = ContentScale.Crop,
                        onSuccess = { Log.d("이미지 성공", imageUrl) },
                        onError = { Log.d("이미지 에러", it.toString()) },
                        onLoading = { Log.d("이미지 로딩", it.toString()) },
                    )
                    if (message.message.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

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

// --- 4. 전체 화면 이미지를 표시하는 Composable 추가 ---
@Composable
fun FullScreenImage(imageUrl: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false) // 전체 화면으로 확장
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onDismiss() }, // 배경 클릭 시 닫기
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Full screen image",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentScale = ContentScale.Fit, // 이미지가 잘리지 않도록 설정
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        }
    }
}