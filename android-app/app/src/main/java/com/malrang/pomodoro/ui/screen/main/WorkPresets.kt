package com.malrang.pomodoro.ui.screen.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.malrang.pomodoro.dataclass.ui.WorkPreset
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun WorkPresetsManager(
    presets: List<WorkPreset>,
    currentPresetId: String?,
    onPresetSelected: (String) -> Unit,
    onAddPreset: () -> Unit,
    onDeletePreset: (WorkPreset) -> Unit,
    onRenamePreset: (WorkPreset) -> Unit,
    onEditSettings: (String) -> Unit,
    useGrassBackground: Boolean
) {
    val contentColor = if (useGrassBackground) Color.Black else Color.White

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(2.dp, contentColor),
        shape = RectangleShape,
        colors = CardDefaults.cardColors(
            containerColor = if (useGrassBackground) Color(0x99FFFFFF) else Color(0x992D2A5A)
        )
    ) {
        Column {
            LazyColumn(
                modifier = Modifier.heightIn(max = 200.dp)
            ) {
                items(presets) { preset ->
                    WorkPresetItem(
                        preset = preset,
                        isSelected = preset.id == currentPresetId,
                        onSelect = { onPresetSelected(preset.id) },
                        onRename = { onRenamePreset(preset) },
                        onEditSettings = { onEditSettings(preset.id) },
                        onDelete = { onDeletePreset(preset) },
                        useGrassBackground = useGrassBackground
                    )
                }
            }
            Divider(color = contentColor.copy(alpha = 0.5f))
            TextButton(
                onClick = onAddPreset,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = "Work 추가", tint = contentColor)
                Spacer(Modifier.width(4.dp))
                Text("새 Work 추가", color = contentColor)
            }
        }
    }
}

@Composable
fun WorkPresetItem(
    preset: WorkPreset,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onRename: () -> Unit,
    onEditSettings: () -> Unit,
    onDelete: () -> Unit,
    useGrassBackground: Boolean
) {
    val contentColor = if (useGrassBackground) Color.Black else Color.White
    val radioColors = if (useGrassBackground) {
        RadioButtonDefaults.colors(
            selectedColor = Color.Black,
            unselectedColor = Color.Gray
        )
    } else {
        RadioButtonDefaults.colors(
            selectedColor = Color.White,
            unselectedColor = Color.Gray
        )
    }
    val editIconTint = if (useGrassBackground) Color(0xFF0D47A1) else Color.Cyan
    val settingsIconTint = if (useGrassBackground) Color(0xFFF9A825) else Color.Yellow
    val deleteIconTint = if (useGrassBackground) Color(0xFFB71C1C) else Color(0xFFE91E63)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelect,
            colors = radioColors
        )
        Text(
            text = preset.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            color = contentColor
        )
        IconButton(onClick = onRename) {
            Icon(Icons.Default.Edit, contentDescription = "이름 변경", tint = editIconTint)
        }
        IconButton(onClick = onEditSettings) {
            Icon(Icons.Default.Settings, contentDescription = "설정 변경", tint = settingsIconTint)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "삭제", tint = deleteIconTint)
        }
    }
}

