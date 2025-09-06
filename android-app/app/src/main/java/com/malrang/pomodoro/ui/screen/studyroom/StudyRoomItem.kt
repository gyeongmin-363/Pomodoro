package com.malrang.pomodoro.ui.screen.studyroom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.malrang.pomodoro.networkRepo.StudyRoom

@Composable
fun StudyRoomItem(room: StudyRoom, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp) // 아이템 간의 간격 조정
            .border(width = 2.dp, color = Color.White) // 바깥쪽 테두리
            .background(Color(0xFF5E606E)) // 어두운 회색 배경
            .clickable(onClick = onClick)
            .padding(16.dp) // 내부 여백
    ) {
        // 스터디룸 이름
        Text(
            text = room.name,
            fontWeight = FontWeight.Bold,
            color = Color.White // 텍스트 색상
        )
        Spacer(modifier = Modifier.height(8.dp))
        // 스터디룸 설명
        Text(
            text = room.inform ?: "",
            color = Color.LightGray // 텍스트 색상
        )
    }
}