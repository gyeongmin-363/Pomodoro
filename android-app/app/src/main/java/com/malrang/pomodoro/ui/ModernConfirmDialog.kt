package com.malrang.pomodoro.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * README의 모던한 디자인 가이드라인을 따르는 커스텀 확인 다이얼로그입니다.
 * Material 3 AlertDialog를 기반으로 둥근 모서리와 명확한 타이포그래피를 적용합니다.
 *
 * @param onDismissRequest 다이얼로그가 닫혀야 할 때 호출됩니다.
 * @param onConfirm 확인 버튼을 눌렀을 때 실행될 동작입니다.
 * @param title 다이얼로그의 제목 텍스트입니다.
 * @param text 다이얼로그의 본문 텍스트입니다. (content보다 간단한 텍스트용)
 * @param content 다이얼로그의 본문 내용을 구성하는 Composable 람다입니다. (text보다 복잡한 내용용)
 * @param confirmText 확인 버튼의 텍스트입니다. (기본값: "확인")
 * @param dismissText 취소 버튼의 텍스트입니다. (기본값: "취소")
 * @param confirmButtonEnabled 확인 버튼의 활성화 여부를 제어합니다. (기본값: true)
 */
@Composable
fun ModernConfirmDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    title: String,
    text: String? = null,
    content: @Composable (() -> Unit)? = null,
    confirmText: String = "확인",
    dismissText: String = "취소",
    confirmButtonEnabled: Boolean = true
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,

        // Material 3 표준 다이얼로그 모서리 반경 (28dp) 적용
        shape = RoundedCornerShape(28.dp),

        // SurfaceContainerHigh를 사용하여 입체감과 깊이를 부여
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,

        // 제목
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold, // 제목 강조
                color = MaterialTheme.colorScheme.onSurface
            )
        },

        // 본문
        text = {
            if (text != null) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (content != null) {
                content()
            }
        },

        // 확인 버튼 (Primary Color 사용으로 강조)
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = confirmButtonEnabled,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = confirmText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },

        // 취소 버튼 (중립적인 색상 사용)
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(
                    text = dismissText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    )
}