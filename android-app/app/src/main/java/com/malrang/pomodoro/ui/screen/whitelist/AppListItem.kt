package com.malrang.pomodoro.ui.screen.whitelist

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// 1. Material 3 Switch를 import합니다.
import androidx.compose.material3.Switch
import coil3.compose.rememberAsyncImagePainter

// 2. 기존 PixelSwitch import를 삭제합니다.
// import com.malrang.pomodoro.ui.screen.whitelist.PixelSwitch

@Composable
fun AppListItem(
    appName: String,
    appIcon: Drawable,
    isBlocked: Boolean,
    onBlockToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        onClick = { onBlockToggle(!isBlocked) }, // 행 전체 클릭 가능
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp), // 여백 추가
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberAsyncImagePainter(model = appIcon),
                contentDescription = "$appName icon",
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = appName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1.0f)
            )
            Spacer(modifier = Modifier.width(16.dp))

            // 3. PixelSwitch를 Material 3 Switch로 교체합니다.
            Switch(
                checked = isBlocked,
                onCheckedChange = onBlockToggle
            )
        }
    }
}