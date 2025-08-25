package com.malrang.pomodoro.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/**
 * 픽셀아트 스타일의 재사용 가능한 커스텀 확인 다이얼로그
 *
 * @param onDismissRequest 다이얼로그가 닫혀야 할 때 호출됩니다.
 * @param title 다이얼로그의 제목 텍스트입니다.
 * @param confirmText 확인 버튼의 텍스트입니다.
 * @param onConfirm 확인 버튼을 눌렀을 때 실행될 동작입니다.
 * @param confirmButtonEnabled 확인 버튼의 활성화 여부를 제어합니다.
 * @param content 다이얼로그의 본문 내용을 구성하는 Composable 람다입니다.
 */
@Composable
fun PixelArtConfirmDialog(
    onDismissRequest: () -> Unit,
    title: String,
    confirmText: String,
    onConfirm: () -> Unit,
    confirmButtonEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RectangleShape,
            color = Color(0xFF2D2A5A),
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, Color.White, RectangleShape)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))

                // 컨텐츠 영역: Text, TextField 등 자유롭게 구성 가능
                content()

                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // 취소 버튼
                    Button(
                        onClick = onDismissRequest,
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6B6B6B),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("취소")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    // 확인 버튼
                    Button(
                        onClick = onConfirm,
                        shape = RectangleShape,
                        enabled = confirmButtonEnabled, // 활성화 상태 적용
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3F51B5),
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFF494957),
                            disabledContentColor = Color(0xFF9E9E9E)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(confirmText)
                    }
                }
            }
        }
    }
}