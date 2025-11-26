package com.malrang.pomodoro.ui.screen.stats.daliyDetail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChecklistItemRow(
    task: String,
    isDone: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onModify: (String, String) -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            when (it) {
                SwipeToDismissBoxValue.StartToEnd, SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    true
                }
                else -> false
            }
        }
    )

    var showMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editContextText by remember { mutableStateOf(task) }

    // 다이얼로그 스타일링 (표준 다이얼로그지만 컨텐츠에 집중)
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("할 일 수정", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = editContextText,
                    onValueChange = { editContextText = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = { onModify(task, editContextText); showEditDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("수정")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("취소", color = MaterialTheme.colorScheme.onSurface) }
            }
        )
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val alignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.error) // NeoRed
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                Icon(Icons.Default.Delete, contentDescription = "삭제", tint = MaterialTheme.colorScheme.onError)
            }
        },
        content = {
            // Shadow Box Item
            Box(modifier = Modifier.fillMaxWidth()) {
                // Shadow
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .offset(x = 3.dp, y = 3.dp)
                        .background(MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                )

                // Content
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isDone) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(12.dp)
                        )
                        .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        .combinedClickable(
                            onClick = onToggle,
                            onLongClick = { showMenu = true }
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Custom Checkbox Style
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                if (isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                RoundedCornerShape(6.dp)
                            )
                            .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDone) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Text(
                        text = task,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isDone) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                        fontWeight = if (isDone) FontWeight.Normal else FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // 메뉴
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface)
                            .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                    ) {
                        DropdownMenuItem(
                            text = { Text("수정") },
                            onClick = { showMenu = false; editContextText = task; showEditDialog = true },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("삭제") },
                            onClick = { showMenu = false; onDelete() },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                        )
                    }
                }
            }
        }
    )
}