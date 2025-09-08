package com.malrang.pomodoro.ui.screen.studyroom.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.malrang.pomodoro.networkRepo.StudyRoomMember
import io.github.jan.supabase.realtime.Column
import java.time.LocalDate
import java.time.format.DateTimeFormatter


// ✅ 픽셀아트 스타일로 재구성된 CompletionStatusDialog Composable
@Composable
fun CompletionStatusDialog(
    date: LocalDate,
    completers: List<StudyRoomMember>,
    onDismiss: () -> Unit
) {
    val dialogTitle = date.format(DateTimeFormatter.ofPattern("M월 d일")) + " 완료 멤버"

    // 픽셀아트 컨셉의 색상 팔레트
    val pixelDarkGreen = Color(0xFF33691E) // 어두운 녹색
    val pixelLightGreen = Color(0xFF8BC34A) // 밝은 녹색
    val pixelBrown = Color(0xFF795548)     // 갈색
    val pixelBorder = Color(0xFF212121)    // 진한 테두리 색
    val pixelText = Color(0xFFE0E0E0)      // 밝은 텍스트 색

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false) // 기본 다이얼로그 폭 사용 안함
    ) {
        Column(
            modifier = Modifier
                .width(300.dp) // 다이얼로그 폭 고정
                .clip(RoundedCornerShape(0.dp)) // 각진 모서리
                .background(pixelDarkGreen) // 기본 배경
                .border(4.dp, pixelBorder) // 진한 테두리
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 제목 (상단 강조)
            Text(
                text = dialogTitle,
                color = pixelLightGreen,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // 구분선
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(pixelBrown)
                    .padding(bottom = 12.dp)
            )

            // 멤버 목록
            if (completers.isEmpty()) {
                Text(
                    text = "이날 챌린지를 완료한 멤버가 없습니다.",
                    color = pixelText,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (completers.size > 5) 180.dp else (completers.size * 30).dp) // 최대 높이 설정
                        .clip(RoundedCornerShape(0.dp)) // 각진 모서리
                        .background(Color.Black.copy(alpha = 0.3f)) // 목록 배경
                        .border(2.dp, pixelBrown) // 목록 테두리
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(completers) { member ->
                        Text(
                            text = "🐾 ${member.nickname}", // 발바닥 아이콘 추가
                            color = pixelText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 닫기 버튼
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = pixelLightGreen,
                    contentColor = pixelDarkGreen
                ),
                shape = RoundedCornerShape(0.dp), // 각진 버튼
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .border(2.dp, pixelBorder) // 버튼 테두리
            ) {
                Text(
                    text = "닫기",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}