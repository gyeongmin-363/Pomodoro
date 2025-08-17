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
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malrang.pomodoro.viewmodel.PomodoroViewModel

/**
 * 앱의 설정 화면을 표시하는 컴포저블 함수입니다.
 * 타이머 시간, 알림음, 진동 설정을 변경할 수 있습니다.
 *
 * @param viewModel [PomodoroViewModel]의 인스턴스입니다.
 */
@Composable
fun SettingsScreen(viewModel: PomodoroViewModel) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1B4B))
            .padding(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("⚙️ 설정", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Button(onClick = { viewModel.showScreen(com.malrang.pomodoro.data.Screen.Main) }) { Text("← 돌아가기") }
        }

        Spacer(Modifier.height(24.dp))

        Text("타이머 설정", fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))

        Text("공부 시간: ${state.settings.studyTime}분")
        Slider(
            value = state.settings.studyTime.toFloat(),
            onValueChange = { viewModel.updateStudyTime(it.toInt()) },
            valueRange = 1f..60f
        )

        Text("휴식 시간: ${state.settings.breakTime}분")
        Slider(
            value = state.settings.breakTime.toFloat(),
            onValueChange = { viewModel.updateBreakTime(it.toInt()) },
            valueRange = 1f..15f
        )

        Spacer(Modifier.height(24.dp))

        Text("알림 설정", fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = state.settings.soundEnabled, onCheckedChange = { viewModel.toggleSound(it) })
            Text("알림음 사용")
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = state.settings.vibrationEnabled, onCheckedChange = { viewModel.toggleVibration(it) })
            Text("진동 사용")
        }
    }
}
