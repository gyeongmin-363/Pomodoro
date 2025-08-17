package com.malrang.pomodoro.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.malrang.pomodoro.data.Screen
import com.malrang.pomodoro.ui.screen.AnimalScreen
import com.malrang.pomodoro.ui.screen.CollectionScreen
import com.malrang.pomodoro.ui.screen.MainScreen
import com.malrang.pomodoro.ui.screen.SettingsScreen
import com.malrang.pomodoro.viewmodel.PomodoroViewModel

/**
 * 앱의 메인 컴포저블 함수입니다.
 * 현재 UI 상태에 따라 적절한 화면을 표시하는 역할을 합니다.
 *
 * @param viewModel [PomodoroViewModel]의 인스턴스입니다.
 */
@Composable
fun PomodoroApp(viewModel: PomodoroViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    when (state.currentScreen) {
        Screen.Main -> MainScreen(viewModel)
        Screen.Animal -> AnimalScreen(viewModel)
        Screen.Collection -> CollectionScreen(viewModel)
        Screen.Settings -> SettingsScreen(viewModel)
    }
}
