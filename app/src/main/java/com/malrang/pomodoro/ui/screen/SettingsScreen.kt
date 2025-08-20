package com.malrang.pomodoro.ui.screen

import android.widget.Toast // Toast 임포트 추가
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
import androidx.compose.ui.platform.LocalContext // LocalContext 임포트 추가
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malrang.pomodoro.dataclass.ui.Screen
import com.malrang.pomodoro.viewmodel.PomodoroViewModel

/**
 * 앱의 설정 화면을 표시하는 컴포저블 함수입니다.
 * 타이머 시간, 알림음, 진동 설정을 변경할 수 있습니다.
 *
 * @param viewModel [PomodoroViewModel]의 인턴스입니다.
 */
@Composable
fun SettingsScreen(viewModel: PomodoroViewModel) {
    val state by viewModel.uiState.collectAsState()
    val settings = state.settings
    // --- 추가: Toast 메시지를 위해 Context 가져오기 ---
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
            Text("⚙️ 설정", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            // --- 핵심 수정 사항: 뒤로가기 버튼 클릭 로직 변경 ---
            IconButton(onClick = {
                if (state.isTimerStartedOnce) {
                    Toast.makeText(context, "변경사항은 리셋 이후 적용됩니다", Toast.LENGTH_SHORT).show()
                }
                viewModel.showScreen(Screen.Main)
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "돌아가기",
                    tint = Color.White
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Text("타이머 설정", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Color.White)
        Spacer(Modifier.height(8.dp))

        Text("공부 시간: ${settings.studyTime}분", color = Color.White)
        Slider(
            value = settings.studyTime.toFloat(),
            onValueChange = { viewModel.updateStudyTime(it.toInt()) },
            valueRange = 1f..60f
        )

        Text("짧은 휴식 시간: ${settings.shortBreakTime}분", color = Color.White)
        Slider(
            value = settings.shortBreakTime.toFloat(),
            onValueChange = { viewModel.updateShortBreakTime(it.toInt()) },
            valueRange = 0f..60f
        )

        Text("긴 휴식 시간: ${settings.longBreakTime}분", color = Color.White)
        Slider(
            value = settings.longBreakTime.toFloat(),
            onValueChange = { viewModel.updateLongBreakTime(it.toInt()) },
            valueRange = 0f..60f
        )

        Text("긴 휴식 간격: ${settings.longBreakInterval}회 마다", color = Color.White)
        Slider(
            value = settings.longBreakInterval.toFloat(),
            onValueChange = { viewModel.updateLongBreakInterval(it.toInt()) },
            valueRange = 1f..12f,
            steps = 10 // 1부터 12까지 설정 가능
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