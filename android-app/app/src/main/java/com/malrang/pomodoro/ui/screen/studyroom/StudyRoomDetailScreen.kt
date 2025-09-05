package com.malrang.pomodoro.ui.screen.studyroom

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.malrang.pomodoro.viewmodel.StudyRoomViewModel


/**
 * 스터디룸 상세 정보를 표시하는 화면 Composable
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyRoomDetailScreen(
    roomId: String?,
    roomVm: StudyRoomViewModel,
    onNavigateBack: () -> Unit
) {
    // ✅ [추가] 공유 인텐트를 위해 LocalContext를 가져옵니다.
    val context = LocalContext.current

    // 화면이 처음 구성될 때 roomId를 사용하여 스터디룸 정보를 불러옵니다.
    LaunchedEffect(roomId) {
        if (roomId != null) {
            roomVm.loadStudyRoomById(roomId)
            roomVm.loadStudyRoomMembers(roomId)
        }
    }

    val uiState by roomVm.studyRoomUiState.collectAsState()
    val room = uiState.currentStudyRoom
    val members = uiState.currentRoomMembers

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(room?.name ?: "스터디룸 로딩 중...") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                // ✅ [추가] 공유 버튼을 포함하는 actions 입니다.
                actions = {
                    IconButton(onClick = {
                        val shareUrl = "https://pixbbo.netlify.app/study-room/$roomId"
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, "[뽀모도로 스터디] '${room?.name}' 스터디룸에 참여해보세요!\n$shareUrl")
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "스터디룸 공유")
                        context.startActivity(shareIntent)
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "스터디룸 공유")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (room == null) {
            // 로딩 상태 표시
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // 스터디룸 정보 및 멤버 목록 표시
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                Text(
                    text = room.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = room.inform ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "멤버 목록 (${members.size}명)",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(members) { member ->
                        Text(
                            text = "- ${member.nickname}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                // TODO: 여기에 스터디룸 관련 추가 UI(채팅, 현황 등)를 구현할 수 있습니다.
            }
        }
    }
}