package com.malrang.pomodoro.ui.screen.stats.daliyDetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.malrang.pomodoro.dataclass.ui.DailyStat
import com.malrang.pomodoro.viewmodel.StatsViewModel

@Composable
fun ChecklistTab(dailyStat: DailyStat, viewModel: StatsViewModel) {
    var newTaskText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 네오 스타일 입력 필드
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newTaskText,
                onValueChange = { newTaskText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("할 일을 입력하세요") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (newTaskText.isNotBlank()) {
                        viewModel.addChecklistItem(dailyStat.date, newTaskText)
                        newTaskText = ""
                        focusManager.clearFocus()
                    }
                })
            )

            Spacer(modifier = Modifier.width(8.dp))

            // 추가 버튼 (하드 섀도우)
            IconButton(
                onClick = {
                    if (newTaskText.isNotBlank()) {
                        viewModel.addChecklistItem(dailyStat.date, newTaskText)
                        newTaskText = ""
                        focusManager.clearFocus()
                    }
                },
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                    .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.Default.Add, contentDescription = "추가", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (dailyStat.checklist.isEmpty()) {
                EmptyStateMessage("등록된 체크리스트가 없습니다.")
            } else {
                dailyStat.checklist.forEach { (task, isDone) ->
                    ChecklistItemRow(
                        task = task,
                        isDone = isDone,
                        onToggle = { viewModel.toggleChecklistItem(dailyStat.date, task) },
                        onDelete = { viewModel.deleteChecklistItem(dailyStat.date, task) },
                        onModify = { old, new ->
                            viewModel.modifyChecklistItem(dailyStat.date, old, new)
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}