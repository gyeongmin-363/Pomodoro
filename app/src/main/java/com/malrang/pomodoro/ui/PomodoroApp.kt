package com.malrang.pomodoro.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.malrang.pomodoro.dataclass.ui.Screen
import com.malrang.pomodoro.ui.screen.CollectionScreen
import com.malrang.pomodoro.ui.screen.MainScreen
import com.malrang.pomodoro.ui.screen.PermissionScreen
import com.malrang.pomodoro.ui.screen.SettingsScreen
import com.malrang.pomodoro.ui.screen.StatsScreen
import com.malrang.pomodoro.ui.screen.WhitelistScreen
import com.malrang.pomodoro.viewmodel.PomodoroViewModel
import com.malrang.withpet.BackPressMove

/**
 * 앱의 메인 컴포저블 함수입니다.
 * 현재 UI 상태에 따라 적절한 화면을 표시하는 역할을 합니다.
 *
 * @param vm [PomodoroViewModel]의 인스턴스입니다.
+ * @param onRequestPermission 권한 설정 버튼 클릭 시 호출될 함수입니다.
 */
@Composable
fun PomodoroApp(vm: PomodoroViewModel = viewModel(), onRequestPermission: () -> Unit) {
        val s by vm.uiState.collectAsState()

        // ✅ MainScreen이 아닐 경우 뒤로가기 버튼을 누르면 Main으로 이동
        if (s.currentScreen != Screen.Main && s.currentScreen != Screen.Whitelist ) {
            BackPressMove {
                vm.showScreen(Screen.Main)
            }
        }

        when (s.currentScreen) {
            Screen.Main -> MainScreen(vm)
            Screen.Collection -> CollectionScreen(vm)
            Screen.Settings -> SettingsScreen(vm)
            Screen.Stats -> StatsScreen(vm)
            Screen.Whitelist -> WhitelistScreen(vm)
            Screen.Permission -> PermissionScreen(vm)
            else -> MainScreen(vm)
        }
    }