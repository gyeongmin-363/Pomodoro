//package com.malrang.pomodoro.ui.screen
//
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.layout.width
//import androidx.compose.material3.Button
//import androidx.compose.material3.CircularProgressIndicator
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.collectAsState
//import androidx.compose.runtime.getValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Brush
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import com.malrang.pomodoro.data.Mode
//import com.malrang.pomodoro.data.Screen
//import com.malrang.pomodoro.viewmodel.PomodoroViewModel
//
///**
// * 앱의 메인 화면을 표시하는 컴포저블 함수입니다.
// * 타이머, 통계, 네비게이션 버튼을 포함합니다.
// *
// * @param viewModel [PomodoroViewModel]의 인스턴스입니다.
// */
//@Composable
//fun MainScreen(viewModel: PomodoroViewModel) {
//    val state by viewModel.uiState.collectAsState()
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(
//                Brush.verticalGradient(
//                    listOf(Color(0xFF312E81), Color(0xFF6D28D9), Color(0xFFDB2777))
//                )
//            )
//            .padding(16.dp),
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        Text("🐾 포모도로 동물원", fontSize = 28.sp, fontWeight = FontWeight.Bold)
//        Text("공부하고 동물 친구들을 만나보세요!", color = Color(0xFFDDD6FE))
//
//        Spacer(Modifier.height(16.dp))
//
//        // 원형 타이머
//        Box(contentAlignment = Alignment.Center) {
//            val totalTime = if (state.currentMode == Mode.STUDY) state.settings.studyTime * 60 else state.settings.breakTime * 60
//            val progress = 1f - state.timeLeft.toFloat() / totalTime
//
//            CircularProgressIndicator(
//                progress = progress,
//                strokeWidth = 12.dp,
//                modifier = Modifier.size(200.dp),
//                color = if (state.currentMode == Mode.STUDY) Color(0xFF10B981) else Color(0xFFF59E0B)
//            )
//            Text(
//                text = "%02d:%02d".format(state.timeLeft / 60, state.timeLeft % 60),
//                fontSize = 32.sp,
//                fontWeight = FontWeight.Bold
//            )
//        }
//
//        Spacer(Modifier.height(16.dp))
//
//        Row {
//            if (!state.isRunning) {
//                Button(onClick = { viewModel.startTimer() }) { Text("시작") }
//            } else {
//                Button(onClick = { viewModel.pauseTimer() }) { Text("일시정지") }
//            }
//            Spacer(Modifier.width(8.dp))
//            Button(onClick = { viewModel.resetTimer() }) { Text("리셋") }
//        }
//
//        Spacer(Modifier.height(24.dp))
//
//        // 통계
//        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
//            Column(horizontalAlignment = Alignment.CenterHorizontally) {
//                Text("${state.collectedAnimals.size}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Green)
//                Text("수집한 동물", color = Color.LightGray)
//            }
//            Column(horizontalAlignment = Alignment.CenterHorizontally) {
//                Text("${state.totalSessions}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Cyan)
//                Text("완료한 세션", color = Color.LightGray)
//            }
//        }
//
//        Spacer(Modifier.height(24.dp))
//
//        // 네비게이션
//        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
//            Button(onClick = { viewModel.showScreen(Screen.Collection) }) { Text("📚 동물 도감") }
//            Button(onClick = { viewModel.showScreen(Screen.Settings) }) { Text("⚙️ 설정") }
//        }
//    }
//}
