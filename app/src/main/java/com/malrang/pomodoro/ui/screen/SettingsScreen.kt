package com.malrang.pomodoro.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.malrang.pomodoro.R
import com.malrang.pomodoro.dataclass.ui.Screen
import com.malrang.pomodoro.ui.PixelArtConfirmDialog
import com.malrang.pomodoro.viewmodel.PomodoroViewModel

@Composable
fun SettingsScreen(viewModel: PomodoroViewModel) {
    val editingPreset by viewModel.editingWorkPreset.collectAsState()
    val state by viewModel.uiState.collectAsState()
    val settings = editingPreset?.settings ?: state.settings
    val title = editingPreset?.name ?: "기본 설정"

    var showDialog by remember { mutableStateOf(false) }

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
        }

        Spacer(Modifier.height(24.dp))

        Text("타이머 설정", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Color.White)
        Spacer(Modifier.height(8.dp))

        // --- ▼▼▼ UI 개선: 슬라이더 및 체크박스 색상 통일 ▼▼▼ ---
        val sliderColors = SliderDefaults.colors(
            thumbColor = Color.White,
            activeTrackColor = Color(0xFF3F51B5),
            inactiveTrackColor = Color.Gray
        )

        Text("공부 시간: ${settings.studyTime}분", color = Color.White)
        Slider(
            value = settings.studyTime.toFloat(),
            onValueChange = { viewModel.updateStudyTime(it.toInt()) },
            valueRange = 1f..60f,
            colors = sliderColors
        )

        Text("짧은 휴식 시간: ${settings.shortBreakTime}분", color = Color.White)
        Slider(
            value = settings.shortBreakTime.toFloat(),
            onValueChange = { viewModel.updateShortBreakTime(it.toInt()) },
            valueRange = 1f..30f,
            colors = sliderColors
        )

        Text("긴 휴식 시간: ${settings.longBreakTime}분", color = Color.White)
        Slider(
            value = settings.longBreakTime.toFloat(),
            onValueChange = { viewModel.updateLongBreakTime(it.toInt()) },
            valueRange = 1f..60f,
            colors = sliderColors
        )

        Text("긴 휴식 간격: ${settings.longBreakInterval}회 마다", color = Color.White)
        Slider(
            value = settings.longBreakInterval.toFloat(),
            onValueChange = { viewModel.updateLongBreakInterval(it.toInt()) },
            valueRange = 2f..12f,
            steps = 9,
            colors = sliderColors
        )
        Spacer(Modifier.height(24.dp))

        Text("알림 설정", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Color.White)
        Spacer(Modifier.height(8.dp))

        val checkboxColors = CheckboxDefaults.colors(
            checkedColor = Color(0xFF3F51B5),
            uncheckedColor = Color.White,
            checkmarkColor = Color.White
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = settings.soundEnabled, onCheckedChange = { viewModel.toggleSound(it) }, colors = checkboxColors)
            Text("알림음 사용", color = Color.White)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = settings.vibrationEnabled, onCheckedChange = { viewModel.toggleVibration(it) }, colors = checkboxColors)
            Text("진동 사용", color = Color.White)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = settings.autoStart, onCheckedChange = { viewModel.toggleAutoStart(it) }, colors = checkboxColors)
            Text("자동 시작", color = Color.White)
        }
        // --- ▲▲▲ UI 개선 ▲▲▲ ---

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(
                onClick = {
                    viewModel.stopEditingWorkPreset()
                    viewModel.showScreen(Screen.Main)
                },
            ) {
                Icon(Icons.Default.Close, "취소", tint = Color.White)
            }

            Spacer(modifier = Modifier.width(16.dp))

            IconButton(onClick = { showDialog = true }) {
                Icon(painterResource(R.drawable.ic_save),"저장", tint = Color.White)
            }
        }
    }

    // --- ▼▼▼ 수정된 부분: 공용 다이얼로그 Composable 사용 ▼▼▼ ---
    if (showDialog) {
        PixelArtConfirmDialog(
            onDismissRequest = { showDialog = false },
            title = "저장하시겠습니까?",
            confirmText = "확인",
            onConfirm = {
                viewModel.reset()
                viewModel.stopEditingWorkPreset()
                viewModel.showScreen(Screen.Main)
                showDialog = false
            }
        ) {
            Text(
                text = "저장하면 타이머가 초기화됩니다.\n계속 진행할까요?",
                color = Color.LightGray
            )
        }
    }
    // --- ▲▲▲ 수정된 부분 ▲▲▲ ---
}