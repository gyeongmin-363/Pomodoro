package com.malrang.pomodoro.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat


/**
 * 픽셀아트 스타일의 재사용 가능한 커스텀 확인 다이얼로그 (전체 화면 유지)
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
    // 1. DialogProperties를 설정하여 시스템 바 뒤로 UI를 그리고, 플랫폼 기본 너비를 사용하지 않도록 합니다.
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            decorFitsSystemWindows = false, // Edge-to-Edge 활성화
            usePlatformDefaultWidth = false // 좌우 여백 제거
        )
    ) {
        // 2. 다이얼로그의 Window 객체를 가져와 전체 화면으로 만듭니다.
        val view = LocalView.current
        val dialogWindow = (view.parent as? DialogWindowProvider)?.window

        if (dialogWindow != null) {
            LaunchedEffect(Unit) {
                val insetsController = WindowCompat.getInsetsController(dialogWindow, view)
                // 시스템 바(상태, 네비게이션 바) 숨기기
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                // 스와이프 시에만 시스템 바가 일시적으로 나타나도록 설정
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }

        // --- 기존 다이얼로그 UI 코드는 그대로 유지합니다. ---
        // 화면 전체를 덮도록 fillMaxSize()를 추가하고, 다이얼로그 UI는 Box 중앙에 배치합니다.
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RectangleShape,
                color = Color(0xFF2D2A5A),
                modifier = Modifier
                    .fillMaxWidth(0.8f) // 다이얼로그 너비를 화면의 80%로 설정 (조정 가능)
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

                    content()

                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
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
                        Button(
                            onClick = onConfirm,
                            shape = RectangleShape,
                            enabled = confirmButtonEnabled,
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
}