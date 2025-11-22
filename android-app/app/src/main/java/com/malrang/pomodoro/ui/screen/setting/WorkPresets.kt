package com.malrang.pomodoro.ui.screen.setting

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.malrang.pomodoro.dataclass.ui.WorkPreset

@Composable
fun WorkPresetItem(
    preset: WorkPreset,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onItemClick: () -> Unit,
    onRename: () -> Unit,
    onEditSettings: () -> Unit,
    onDelete: () -> Unit
) {
    // 선택된 아이템일 경우 강조 색상 사용
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    else MaterialTheme.colorScheme.surfaceContainerLow

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .border(width = if(isSelected) 2.dp else 0.dp, color = borderColor, shape = RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onItemClick),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if(isSelected) 4.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 라디오 버튼을 아이콘으로 대체하거나 유지
            RadioButton(
                selected = isSelected,
                onClick = onSelect,
                modifier = Modifier.size(20.dp)
            )

            Spacer(Modifier.width(12.dp))

            Text(
                text = preset.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            // 액션 버튼 그룹
            Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                IconButton(onClick = onRename, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "이름 변경", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
                }
                IconButton(onClick = onEditSettings, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Settings, contentDescription = "설정", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.tertiary)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "삭제", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}