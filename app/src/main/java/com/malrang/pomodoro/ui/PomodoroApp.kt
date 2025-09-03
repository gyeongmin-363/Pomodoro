package com.malrang.pomodoro.ui

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.malrang.pomodoro.dataclass.ui.Screen
import com.malrang.pomodoro.ui.screen.CollectionScreen
import com.malrang.pomodoro.ui.screen.LoginScreen
import com.malrang.pomodoro.ui.screen.MainScreen
import com.malrang.pomodoro.ui.screen.PermissionScreen
import com.malrang.pomodoro.ui.screen.SettingsScreen
import com.malrang.pomodoro.ui.screen.StatsScreen
import com.malrang.pomodoro.ui.screen.UserScreen
import com.malrang.pomodoro.ui.screen.WhitelistScreen
import com.malrang.pomodoro.viewmodel.AuthViewModel
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
fun PomodoroApp(vm: PomodoroViewModel = viewModel(), authVm : AuthViewModel = viewModel()) {
    // 1. 두 ViewModel의 상태를 모두 관찰합니다.
    val appState by vm.uiState.collectAsState()
    val authState by authVm.uiState.collectAsState()
    val context = LocalContext.current

    // 2. 인증 상태가 'Authenticated'로 변경될 때만 권한 확인 로직을 실행합니다.
    LaunchedEffect(authState) {
        if (authState is AuthViewModel.AuthState.Authenticated) {
            // 로그인 성공 시점에 권한을 확인하고 필요한 화면으로 이동시킵니다.
            vm.checkPermissionsAndNavigateIfNeeded(context)
        }
    }

    // ✅ MainScreen이 아닐 경우 뒤로가기 버튼을 누르면 Main으로 이동
    if (appState.currentScreen != Screen.Main && appState.currentScreen != Screen.Whitelist ) {
        BackPressMove {
            vm.showScreen(Screen.Main)
        }
    }


    // 3. 인증 상태에 따라 화면을 분기합니다.
    when (authState) {
        is AuthViewModel.AuthState.Authenticated -> {
            // ✅ 로그인이 완료되었을 때만 앱의 주 화면 로직을 따릅니다.
            when (appState.currentScreen) {
                Screen.Login -> LoginScreen(authVm)
                Screen.Main -> MainScreen(vm)
                Screen.Collection -> CollectionScreen(vm)
                Screen.Settings -> SettingsScreen(vm)
                Screen.Stats -> StatsScreen(vm)
                Screen.Whitelist -> WhitelistScreen(vm)
                Screen.Permission -> PermissionScreen(vm)
                Screen.StudyRoom -> UserScreen(vm)
                else -> LoginScreen(authVm)
            }
        }
        is AuthViewModel.AuthState.NotAuthenticated,
        is AuthViewModel.AuthState.Idle,
        is AuthViewModel.AuthState.Error -> {
            // ❌ 로그인이 안 된 상태이거나 오류가 발생하면 항상 로그인 화면을 보여줍니다.
            LoginScreen(viewModel = authVm)
        }

        is AuthViewModel.AuthState.Loading,
        is AuthViewModel.AuthState.WaitingForRedirect -> {
            // 로딩 중이거나 리디렉션을 기다리는 중일 때
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}