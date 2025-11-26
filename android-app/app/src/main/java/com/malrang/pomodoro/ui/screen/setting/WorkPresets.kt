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
    isDeleteEnabled: Boolean = true, // [수정] 삭제 가능 여부 파라미터 추가
    onSelect: () -> Unit,
    onRename: () -> Unit,
    onEditSettings: () -> Unit,
    onDelete: () -> Unit
) {
    // 선택된 아이템: PrimaryContainer + Primary 테두리
    // 미선택 아이템: SurfaceContainer (배경과 은은하게 구분됨) + 테두리 없음
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceContainer // Surface보다 살짝 밝거나 어두운 톤

    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            // 기존의 horizontal padding 제거 (부모 컨테이너가 16dp 가짐)
            .height(72.dp) // 높이를 약간 주어 클릭 영역 확보 및 디자인 안정감
            .border(width = if (isSelected) 2.dp else 0.dp, color = borderColor, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = !isSelected, onClick = onSelect),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // 플랫한 디자인 유지
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp), // 내부 패딩
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = preset.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
                modifier = Modifier.weight(1f)
            )

            // 액션 버튼 그룹
            Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                IconButton(onClick = onRename, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "이름 변경",
                        modifier = Modifier.size(18.dp),
                        tint = if(isSelected) contentColor.copy(alpha = 0.7f) else MaterialTheme.colorScheme.secondary
                    )
                }
                IconButton(onClick = onEditSettings, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "설정",
                        modifier = Modifier.size(18.dp),
                        tint = if(isSelected) contentColor.copy(alpha = 0.7f) else MaterialTheme.colorScheme.tertiary
                    )
                }

                // [수정] 삭제 버튼에 enabled 속성 및 색상 처리 적용
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp), enabled = isDeleteEnabled) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "삭제",
                        modifier = Modifier.size(18.dp),
                        // 비활성화 시 흐린 색상 적용
                        tint = if (isDeleteEnabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
            }
        }
    }
}