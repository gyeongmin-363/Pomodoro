package com.malrang.pomodoro.ui

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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.malrang.pomodoro.dataclass.ui.Screen
import com.malrang.pomodoro.ui.screen.account.AccountSettingsScreen
import com.malrang.pomodoro.ui.screen.login.LoginScreen
import com.malrang.pomodoro.ui.screen.main.MainScreen
import com.malrang.pomodoro.ui.screen.permission.PermissionScreen
import com.malrang.pomodoro.ui.screen.setting.SettingsScreen
import com.malrang.pomodoro.ui.screen.stats.StatsScreen
import com.malrang.pomodoro.ui.screen.whitelist.WhitelistScreen
import com.malrang.pomodoro.viewmodel.AuthViewModel
import com.malrang.pomodoro.viewmodel.PermissionViewModel
import com.malrang.pomodoro.viewmodel.SettingsViewModel
import com.malrang.pomodoro.viewmodel.StatsViewModel
import com.malrang.pomodoro.viewmodel.TimerViewModel

@Composable
fun PomodoroApp(
    timerViewModel: TimerViewModel,
    settingsViewModel: SettingsViewModel,
    permissionViewModel: PermissionViewModel,
    statsViewModel : StatsViewModel,
    authViewModel: AuthViewModel,
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val authState by authViewModel.authState.collectAsState()

    when (authState) {
        is AuthViewModel.AuthState.Authenticated -> {
            // ✅ 권한이 부여되었는지 확인합니다.
            val allPermissionsGranted = permissionViewModel.checkAndUpdatePermissions(context)
            // ✅ 권한 상태에 따라 시작 화면을 결정합니다.
            val startDestination = if (allPermissionsGranted) Screen.Main.name else Screen.Permission.name


            // ✅ 로그인이 된 상태이면 메인 앱 콘텐츠를 보여줍니다.
            NavHost(navController = navController, startDestination = startDestination) {
                composable(Screen.Main.name) {
                    MainScreen(
                        timerViewModel = timerViewModel,
                        settingsViewModel = settingsViewModel,
                        onNavigateTo = { screen -> navController.navigate(screen.name) }
                    )
                }
                composable(Screen.Stats.name) {
                    StatsScreen(
                        statsViewModel = statsViewModel,
                        onNavigateTo = { screen -> navController.navigate(screen.name) }
                    )
                }
                composable(Screen.Settings.name) {
                    SettingsScreen(
                        settingsViewModel = settingsViewModel,
                        onNavigateTo = { screen -> navController.navigate(screen.name) },
                        onSave = {
                            settingsViewModel.saveSettingsAndReset { newSettings ->
                                timerViewModel.reset(newSettings)
                                // 저장 및 리셋이 완료된 후 메인 화면으로 이동합니다.
                                navController.navigate(Screen.Main.name) {
                                    popUpTo(Screen.Settings.name) { inclusive = true }
                                }
                            }
                        }
                    )
                }
                composable(Screen.Permission.name) {
                    val permissionUiState by permissionViewModel.uiState.collectAsState()
                    PermissionScreen(
                        permissionUiState = permissionUiState,
                        onPermissionResult = { permissionViewModel.onPermissionRequestResult(context) },
                        onSetPermissionAttempted = permissionViewModel::setPermissionAttemptedInSession,
                        // 권한 설정 후 메인 화면으로 이동하고 이전 스택을 지웁니다.
                        onNavigateToMain = {
                            navController.navigate(Screen.Main.name) {
                                popUpTo(Screen.Permission.name) { inclusive = true }
                            }
                        }
                    )
                }
                composable(Screen.Whitelist.name) {
                    WhitelistScreen(
                        settingsViewModel = settingsViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.AccountSettings.name) {
                    AccountSettingsScreen(
                        authViewModel = authViewModel,
                        onNavigateTo = { navController.navigate(Screen.Main.name) }
                    )
                }

            }
        }
        is AuthViewModel.AuthState.NotAuthenticated,
        is AuthViewModel.AuthState.Idle,
        is AuthViewModel.AuthState.Error -> {
            // ❌ 로그인이 안 된 상태이거나 오류가 발생하면 항상 로그인 화면을 보여줍니다.
            LoginScreen(viewModel = authViewModel)
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