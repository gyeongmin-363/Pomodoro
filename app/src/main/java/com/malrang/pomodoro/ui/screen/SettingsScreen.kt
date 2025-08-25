package com.malrang.pomodoro.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malrang.pomodoro.R
import com.malrang.pomodoro.dataclass.ui.Screen
import com.malrang.pomodoro.viewmodel.PomodoroViewModel

@Composable
fun SettingsScreen(viewModel: PomodoroViewModel) {
    val editingPreset by viewModel.editingWorkPreset.collectAsState()
    val state by viewModel.uiState.collectAsState()
    val settings = editingPreset?.settings ?: state.settings
    val title = editingPreset?.name ?: "기본 설정"

    // --- ▼▼▼ 추가된 부분 (Dialog 상태 관리) ▼▼▼ ---
    var showDialog by remember { mutableStateOf(false) }
    // --- ▲▲▲ 추가된 부분 ▲▲▲ ---

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1B4B))
            .padding(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("⚙️ $title 설정", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            // --- ▼▼▼ 삭제된 부분 (뒤로가기 아이콘 버튼) ▼▼▼ ---
            // IconButton(...) { ... }
            // --- ▲▲▲ 삭제된 부분 ▲▲▲ ---
        }

        Spacer(Modifier.height(24.dp))

        Text("타이머 설정", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Color.White)
        Spacer(Modifier.height(8.dp))

        Text("공부 시간: ${settings.studyTime}분", color = Color.White)
        Slider(
            value = settings.studyTime.toFloat(),
            onValueChange = { viewModel.updateStudyTime(it.toInt()) },
            valueRange = 1f..60f,
        )

        Text("짧은 휴식 시간: ${settings.shortBreakTime}분", color = Color.White)
        Slider(
            value = settings.shortBreakTime.toFloat(),
            onValueChange = { viewModel.updateShortBreakTime(it.toInt()) },
            valueRange = 1f..30f,
        )

        Text("긴 휴식 시간: ${settings.longBreakTime}분", color = Color.White)
        Slider(
            value = settings.longBreakTime.toFloat(),
            onValueChange = { viewModel.updateLongBreakTime(it.toInt()) },
            valueRange = 1f..60f,
        )

        Text("긴 휴식 간격: ${settings.longBreakInterval}회 마다", color = Color.White)
        Slider(
            value = settings.longBreakInterval.toFloat(),
            onValueChange = { viewModel.updateLongBreakInterval(it.toInt()) },
            valueRange = 2f..12f,
            steps = 9
        )
        Spacer(Modifier.height(24.dp))

        Text("알림 설정", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Color.White)
        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = settings.soundEnabled, onCheckedChange = { viewModel.toggleSound(it) })
            Text("알림음 사용", color = Color.White)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = settings.vibrationEnabled, onCheckedChange = { viewModel.toggleVibration(it) })
            Text("진동 사용", color = Color.White)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = settings.autoStart, onCheckedChange = { viewModel.toggleAutoStart(it) })
            Text("자동 시작", color = Color.White)
        }

        // --- ▼▼▼ 추가된 부분 (하단 버튼) ▼▼▼ ---
        Spacer(Modifier.weight(1f)) // 버튼을 하단으로 밀어내기 위한 Spacer

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(
                onClick = {
                    viewModel.stopEditingWorkPreset()
                    viewModel.showScreen(Screen.Main)
                },
//                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
            ) {
                Icon(Icons.Default.Close, "취소")
            }

            Spacer(modifier = Modifier.width(16.dp))

            IconButton(onClick = { showDialog = true }) {
                Icon(painterResource(R.drawable.ic_save),"저장")
            }
        }
        // --- ▲▲▲ 추가된 부분 ▲▲▲ ---
    }

    // --- ▼▼▼ 추가된 부분 (저장 확인 Dialog) ▼▼▼ ---
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("저장 확인") },
            text = { Text("저장 시, 타이머가 초기화됩니다. 계속하시겠습니까?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.reset()
                        viewModel.stopEditingWorkPreset()
                        viewModel.showScreen(Screen.Main)
                        showDialog = false
                    }
                ) {
                    Text("확인")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                ) {
                    Text("취소")
                }
            }
        )
    }
    // --- ▲▲▲ 추가된 부분 ▲▲▲ ---
}