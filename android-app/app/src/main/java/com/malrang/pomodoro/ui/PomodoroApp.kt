package com.malrang.pomodoro.ui

import android.content.Intent
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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.malrang.pomodoro.dataclass.ui.Screen
import com.malrang.pomodoro.ui.screen.account.AccountSettingsScreen
import com.malrang.pomodoro.ui.screen.groups.GroupsList
import com.malrang.pomodoro.ui.screen.login.LoginScreen
import com.malrang.pomodoro.ui.screen.main.MainScreen
import com.malrang.pomodoro.ui.screen.nickname.NicknameSetupScreen
import com.malrang.pomodoro.ui.screen.permission.PermissionScreen
import com.malrang.pomodoro.ui.screen.setting.SettingsScreen
import com.malrang.pomodoro.ui.screen.stats.StatsScreen
import com.malrang.pomodoro.ui.screen.whitelist.WhitelistScreen
import com.malrang.pomodoro.viewmodel.AuthViewModel
import com.malrang.pomodoro.viewmodel.PermissionViewModel
import com.malrang.pomodoro.viewmodel.SettingsViewModel
import com.malrang.pomodoro.viewmodel.StatsViewModel
import com.malrang.pomodoro.viewmodel.TimerViewModel
import com.malrang.pomodoro.viewmodel.UserViewModel

@Composable
fun PomodoroApp(
    timerViewModel: TimerViewModel,
    settingsViewModel: SettingsViewModel,
    permissionViewModel: PermissionViewModel,
    statsViewModel : StatsViewModel,
    authViewModel: AuthViewModel,
    userViewModel: UserViewModel,
) {
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState()
    val userState by userViewModel.userState.collectAsState() // userState를 여기서 수집

    when (authState) {
        is AuthViewModel.AuthState.Authenticated -> {
            // ✅ 인증 상태가 확인되면 사용자 프로필을 가져옵니다.
            // 이 LaunchedEffect는 'Authenticated' 상태가 될 때 한 번만 실행됩니다.
            LaunchedEffect(Unit) {
                userViewModel.fetchUserProfile()
            }

            // ✅ 사용자 프로필을 로딩하는 동안 로딩 인디케이터를 표시합니다.
            if (userState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val navController = rememberNavController()
                // ✅ 권한 확인
                val allPermissionsGranted = permissionViewModel.checkAndUpdatePermissions(context)

                // ✅ 권한과 닉네임 설정 여부에 따라 시작 화면을 결정합니다.
                val startDestination = when {
                    !allPermissionsGranted -> Screen.Permission.name
                    !userState.isNicknameSet -> Screen.NicknameSetup.name
                    else -> Screen.Main.name
                }

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
                            // ✅ 권한 설정 후 닉네임 설정 여부에 따라 분기합니다.
                            onNavigateTo = {
                                val destination = if (userState.isNicknameSet) {
                                    Screen.Main.name
                                } else {
                                    Screen.NicknameSetup.name
                                }
                                navController.navigate(destination) {
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
                    composable(Screen.NicknameSetup.name) {
                        NicknameSetupScreen(
                            userViewModel = userViewModel,
                            onNavigateToMain = {
                                navController.navigate(Screen.Main.name) {
                                    popUpTo(Screen.NicknameSetup.name) { inclusive = true }
                                }
                            }
                        )
                    }
                    // 딥링크 처리
                    composable(
                        route = "${Screen.GroupsList.name}?inviteId={inviteId}",
                        arguments = listOf(navArgument("inviteId") {
                            type = NavType.StringType
                            nullable = true
                        }),
                        deepLinks = listOf(
                            // App Links (https://) 하지만 작동은 안함
                            navDeepLink {
                                action = Intent.ACTION_VIEW
                                uriPattern = "https://pixbbo.netlify.app/groups/{inviteId}"
                            },
                            // Custom Scheme (pixbbo://)
                            navDeepLink {
                                action = Intent.ACTION_VIEW
                                uriPattern = "pixbbo://groups/{inviteId}"
                            }
                        )
                    ) { backStackEntry ->
                        val inviteId = backStackEntry.arguments?.getString("inviteId")
                        GroupsList(
//                            roomVM = studyRoomViewModel,
//                            inviteStudyRoomId = inviteId, // ✅ 여기서 uuid 주입됨
//                            onNavigateBack = { navController.navigate(Screen.Main.name) },
//                            onNavigateToDelete = { navController.navigate(Screen.DeleteStudyRoom.name) }
                        )
                    }
                    // 챌린지룸 상세 화면을 위한 composable 경로
                    composable(
                        route = "${Screen.Group.name}/{groupId}",
                        arguments = listOf(navArgument("groupId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val groupId = backStackEntry.arguments?.getString("groupId")
//                        StudyRoomDetailScreen(
//                            roomId = roomId,
//                            roomVm = studyRoomViewModel,
//                            onNavigateBack = { navController.popBackStack() },
//                            onNavigateToChat = { studyRoomId ->
//                                navController.navigate("chat/$studyRoomId")
//                            }
//                        )
                    }
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