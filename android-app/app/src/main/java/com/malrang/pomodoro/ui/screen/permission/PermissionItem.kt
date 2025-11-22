package com.malrang.pomodoro.ui.screen.permission

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.malrang.pomodoro.dataclass.ui.PermissionInfo
import com.malrang.pomodoro.dataclass.ui.PermissionType

@Composable
fun PermissionItem(
    permission: PermissionInfo,
    hasBeenAttempted: Boolean,
    isNextAction: Boolean = false // 현재 설정해야 할 아이템인지 여부
) {
    // 권한 타입에 따른 아이콘 매핑
    val icon = when (permission.type) {
        PermissionType.NOTIFICATION -> Icons.Default.Notifications
        PermissionType.OVERLAY -> Icons.Default.Notifications
        PermissionType.USAGE_STATS -> Icons.Default.Notifications
        else -> Icons.Default.Settings
    }

    // 상태에 따른 색상 및 테두리 설정
    val cardColors = when {
        permission.isGranted -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
        isNextAction -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
        else -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    }

    val borderColor = if (isNextAction && !permission.isGranted) MaterialTheme.colorScheme.primary else Color.Transparent
    val borderWidth = if (isNextAction && !permission.isGranted) 2.dp else 0.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(borderWidth, borderColor, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = cardColors,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isNextAction) 4.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 좌측: 아이콘
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (permission.isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // 중앙: 텍스트 정보
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = permission.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = permission.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 우측: 상태 표시 (체크 or 경고)
            if (permission.isGranted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "허용됨",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            } else if (hasBeenAttempted) {
                // 시도했으나 거부된 경우 경고 아이콘
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "설정 필요",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}