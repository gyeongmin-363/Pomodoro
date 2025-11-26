package com.malrang.pomodoro.ui.screen.stats.month

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties

@Composable
fun StatsFilterDropdown(
    currentFilter: String,
    options: List<String>,
    onFilterSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var dismissalTimestamp by remember { mutableLongStateOf(0L) }

    BackHandler(enabled = expanded) { expanded = false }

    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
        // 드롭다운 트리거 버튼 (Neo Style)
        Box(
            modifier = Modifier.clickable {
                if (System.currentTimeMillis() - dismissalTimestamp > 200) {
                    expanded = !expanded
                }
            }
        ) {
            // Shadow
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = 3.dp, y = 3.dp)
                    .background(MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            )
            // Content
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                    .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentFilter,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // 드롭다운 메뉴
        MaterialTheme(
            shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(8.dp))
        ) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                    dismissalTimestamp = System.currentTimeMillis()
                },
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .width(140.dp),
                properties = PopupProperties(focusable = false, dismissOnBackPress = true, dismissOnClickOutside = true)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                option,
                                fontWeight = FontWeight.Medium,
                                color = if(option == currentFilter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = { onFilterSelected(option); expanded = false }
                    )
                }
            }
        }
    }
}