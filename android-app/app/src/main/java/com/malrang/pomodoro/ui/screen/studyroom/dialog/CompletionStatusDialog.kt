package com.malrang.pomodoro.ui.screen.studyroom.dialog

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malrang.pomodoro.networkRepo.StudyRoomMember
import com.malrang.pomodoro.ui.PixelArtConfirmDialog
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun CompletionStatusDialog(
    date: LocalDate,
    completers: List<StudyRoomMember>,
    onDismiss: () -> Unit
) {
    val dialogTitle = date.format(DateTimeFormatter.ofPattern("M월 d일")) + " 완료 멤버"

    // 재사용 가능한 PixelArtConfirmDialog를 사용하여 UI 구성
    PixelArtConfirmDialog(
        onDismissRequest = onDismiss,
        title = dialogTitle,
        confirmText = "닫기", // 확인 버튼의 텍스트를 "닫기"로 설정
        onConfirm = onDismiss      // 확인 버튼의 동작도 다이얼로그를 닫는 것으로 설정
    ) {
        // 다이얼로그의 본문 내용
        if (completers.isEmpty()) {
            Text(
                text = "이날 챌린지를 완료한 멤버가 없습니다.",
                color = Color.White, // 스타일에 맞게 텍스트 색상 변경
                fontSize = 14.sp,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            // 멤버 목록을 표시하는 LazyColumn
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp) // 목록의 최대 높이를 지정하여 다이얼로그가 너무 커지는 것을 방지
                    .border(1.dp, Color.White) // 전체적인 스타일에 맞게 테두리 추가
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                items(completers) { member ->
                    Text(
                        text = "🐾 ${member.nickname}",
                        color = Color.White, // 스타일에 맞게 텍스트 색상 변경
                        fontSize = 16.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}