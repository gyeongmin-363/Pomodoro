package com.malrang.pomodoro.ui.screen.studyroom.dialog

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.malrang.pomodoro.ui.PixelArtConfirmDialog

/**
 * 작업을 재확인하는 공용 다이얼로그. PixelArtConfirmDialog를 사용합니다.
 */
@Composable
fun ConfirmationDialog(
    title: String,
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    PixelArtConfirmDialog(
        onDismissRequest = onDismiss,
        title = title,
        confirmText = "확인",
        onConfirm = onConfirm,
        confirmButtonEnabled = true
    ) {
        Text(
            text = text,
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        )
    }
}