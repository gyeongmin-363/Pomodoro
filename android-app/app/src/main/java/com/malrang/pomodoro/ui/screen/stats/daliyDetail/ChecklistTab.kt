package com.malrang.pomodoro.ui.screen.stats.daliyDetail


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
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
        // 입력 필드 개선
        OutlinedTextField(
            value = newTaskText,
            onValueChange = { newTaskText = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("할 일을 입력하세요") },
            trailingIcon = {
                IconButton(
                    onClick = {
                        if (newTaskText.isNotBlank()) {
                            viewModel.addChecklistItem(dailyStat.date, newTaskText)
                            newTaskText = ""
                            focusManager.clearFocus()
                        }
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "추가", tint = MaterialTheme.colorScheme.primary)
                }
            },
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

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                            viewModel.modifyChecklistItem(
                                dailyStat.date,
                                old,
                                new
                            )
                        }
                    )
                }
            }
        }
    }
}
