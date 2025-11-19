package com.malrang.pomodoro.ui.screen.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.malrang.pomodoro.dataclass.ui.WorkPreset

// WorkPresetsManager는 SettingsScreen 내부 로직으로 통합되었으므로 제거해도 되지만,
// WorkPresetItem을 사용하기 위해 파일은 유지합니다.

@Composable
fun WorkPresetItem(
    preset: WorkPreset,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onItemClick: () -> Unit, // ✅ 추가: 아이템(행) 클릭 시 동작 (상세 설정 이동)
    onRename: () -> Unit,
    onEditSettings: () -> Unit,
    onDelete: () -> Unit
) {
    val contentColor = MaterialTheme.colorScheme.onSurface
    val radioColors = RadioButtonDefaults.colors()
    val editIconTint = MaterialTheme.colorScheme.secondary
    val settingsIconTint = MaterialTheme.colorScheme.tertiary
    val deleteIconTint = MaterialTheme.colorScheme.error

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick) // ✅ 수정: 선택이 아닌 상세 설정 진입으로 변경
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 라디오 버튼: Work 선택(활성화) 역할
        RadioButton(
            selected = isSelected,
            onClick = onSelect,
            colors = radioColors
        )

        // 텍스트: 클릭 시 상세 설정 이동 (Row의 clickable을 따름)
        Text(
            text = preset.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            color = contentColor
        )

        // 이름 변경 버튼
        IconButton(onClick = onRename) {
            Icon(Icons.Default.Edit, contentDescription = "이름 변경", tint = editIconTint)
        }

        // 설정 변경 버튼 (명시적으로 누를 경우)
        IconButton(onClick = onEditSettings) {
            Icon(Icons.Default.Settings, contentDescription = "설정 변경", tint = settingsIconTint)
        }

        // 삭제 버튼
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "삭제", tint = deleteIconTint)
        }
    }
}