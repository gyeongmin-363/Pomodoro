package com.malrang.pomodoro.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malrang.pomodoro.dataclass.ui.Screen
import com.malrang.pomodoro.dataclass.ui.Settings
import com.malrang.pomodoro.viewmodel.PomodoroViewModel

@Composable
fun SettingsScreen(viewModel: PomodoroViewModel) {
    // --- ▼▼▼ 수정된 부분 ▼▼▼ ---
    // 편집 중인 프리셋이 있으면 그것을, 없으면 현재 활성화된 프리셋의 설정을 가져옴
    val editingPreset by viewModel.editingWorkPreset.collectAsState()
    val state by viewModel.uiState.collectAsState()
    val settings = editingPreset?.settings ?: state.settings
    val title = editingPreset?.name ?: "기본 설정"
    // --- ▲▲▲ 수정된 부분 ▲▲▲ ---

    val context = LocalContext.current

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
            // --- ▼▼▼ 수정된 부분 (타이틀 변경) ▼▼▼ ---
            Text("⚙️ $title 설정", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            // --- ▲▲▲ 수정된 부분 ▲▲▲ ---
            IconButton(onClick = {
                if (state.isTimerStartedOnce && editingPreset == null) {
                    Toast.makeText(context, "변경사항은 리셋 이후 적용됩니다", Toast.LENGTH_SHORT).show()
                }
                // --- ▼▼▼ 수정된 부분 (편집 모드 종료) ▼▼▼ ---
                viewModel.stopEditingWorkPreset()
                viewModel.showScreen(Screen.Main)
                // --- ▲▲▲ 수정된 부분 ▲▲▲ ---
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "돌아가기",
                    tint = Color.White
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // 슬라이더와 체크박스는 ViewModel의 함수를 호출하므로 수정할 필요 없음
        // ViewModel 내부 로직이 알아서 편집 중인 프리셋을 수정해 줌

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
            valueRange = 1f..30f, // 1분부터 시작
        )

        Text("긴 휴식 시간: ${settings.longBreakTime}분", color = Color.White)
        Slider(
            value = settings.longBreakTime.toFloat(),
            onValueChange = { viewModel.updateLongBreakTime(it.toInt()) },
            valueRange = 1f..60f, // 1분부터 시작
        )

        Text("긴 휴식 간격: ${settings.longBreakInterval}회 마다", color = Color.White)
        Slider(
            value = settings.longBreakInterval.toFloat(),
            onValueChange = { viewModel.updateLongBreakInterval(it.toInt()) },
            valueRange = 2f..12f, // 2회부터
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
    }
}