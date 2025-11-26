package com.malrang.pomodoro.ui.screen.setting

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.malrang.pomodoro.dataclass.ui.WorkPreset

@Composable
fun WorkPresetItem(
    preset: WorkPreset,
    isSelected: Boolean,
    isDeleteEnabled: Boolean = true,
    onSelect: () -> Unit,
    onRename: () -> Unit,
    onEditSettings: () -> Unit,
    onDelete: () -> Unit
) {
    // 선택되면 Primary(Blue) 컬러, 아니면 Surface(White)
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    // 선택된 아이템은 그림자를 더 깊게 줘서 튀어나와 보이게 하거나, 반대로 눌린 느낌을 줄 수 있음
    // 여기서는 일관성을 위해 똑같이 Hard Shadow 적용하되, 색상으로 구분

    val shape = RoundedCornerShape(12.dp)
    val shadowOffset = if (isSelected) 6.dp else 4.dp // 선택되면 좀 더 팝업된 느낌

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp) // 아이템 간 간격
            .height(80.dp)
            .clickable(enabled = !isSelected, onClick = onSelect)
    ) {
        // Shadow Layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = 4.dp, y = 4.dp)
                .background(MaterialTheme.colorScheme.outline, shape)
        )

        // Content Layer
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = shape,
            color = backgroundColor,
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
            shadowElevation = 0.dp // 기본 그림자 제거
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = preset.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    modifier = Modifier.weight(1f)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                    // 아이콘 버튼들도 틴트 색상 조정
                    val iconTint = if (isSelected) contentColor else MaterialTheme.colorScheme.onSurface

                    IconButton(onClick = onRename) {
                        Icon(Icons.Default.Edit, "이름 변경", tint = iconTint)
                    }
                    IconButton(onClick = onEditSettings) {
                        Icon(Icons.Default.Settings, "설정", tint = iconTint)
                    }
                    IconButton(onClick = onDelete, enabled = isDeleteEnabled) {
                        Icon(
                            Icons.Default.Delete,
                            "삭제",
                            tint = if (isDeleteEnabled) iconTint else iconTint.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }
}