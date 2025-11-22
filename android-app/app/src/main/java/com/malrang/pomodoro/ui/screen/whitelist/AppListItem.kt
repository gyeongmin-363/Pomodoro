package com.malrang.pomodoro.ui.screen.whitelist

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter

@Composable
fun AppListItem(
    appName: String,
    appIcon: Drawable,
    isBlocked: Boolean,
    onBlockToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // 차단 상태에 따라 텍스트 색상 변경
    val textColor = if (isBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onBlockToggle(!isBlocked) } // 행 전체 터치 가능
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 앱 아이콘
        Image(
            painter = rememberAsyncImagePainter(model = appIcon),
            contentDescription = "$appName icon",
            modifier = Modifier.size(42.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        // 앱 이름
        Text(
            text = appName,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isBlocked) FontWeight.Bold else FontWeight.Medium, // 차단 시 굵게
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // 스위치 (Material 3 스타일)
        Switch(
            checked = isBlocked,
            onCheckedChange = onBlockToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onErrorContainer,
                checkedTrackColor = MaterialTheme.colorScheme.errorContainer,
                checkedBorderColor = MaterialTheme.colorScheme.errorContainer,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                uncheckedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}