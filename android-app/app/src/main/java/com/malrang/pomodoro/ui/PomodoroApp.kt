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
import com.malrang.pomodoro.ui.screen.collection.CollectionScreen
import com.malrang.pomodoro.ui.screen.login.LoginScreen
import com.malrang.pomodoro.ui.screen.main.MainScreen
import com.malrang.pomodoro.ui.screen.permission.PermissionScreen
import com.malrang.pomodoro.ui.screen.setting.SettingsScreen
import com.malrang.pomodoro.ui.screen.stats.StatsScreen
import com.malrang.pomodoro.ui.screen.studyroom.ChallengeScreen
import com.malrang.pomodoro.ui.screen.studyroom.ChatScreen
import com.malrang.pomodoro.ui.screen.studyroom.DeleteStudyRoomScreen
import com.malrang.pomodoro.ui.screen.studyroom.StudyRoomDetailScreen
import com.malrang.pomodoro.ui.screen.whitelist.WhitelistScreen
import com.malrang.pomodoro.viewmodel.AuthViewModel
import com.malrang.pomodoro.viewmodel.PermissionViewModel
import com.malrang.pomodoro.viewmodel.SettingsViewModel
import com.malrang.pomodoro.viewmodel.StatsViewModel
import com.malrang.pomodoro.viewmodel.StudyRoomViewModel
import com.malrang.pomodoro.viewmodel.TimerViewModel

@Composable
fun PomodoroApp(
    timerViewModel: TimerViewModel,
    settingsViewModel: SettingsViewModel,
    permissionViewModel: PermissionViewModel,
    statsViewModel : StatsViewModel,
    authViewModel: AuthViewModel,
    studyRoomViewModel: StudyRoomViewModel
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val authState by authViewModel.authState.collectAsState()

    // ✅ [추가] StudyRoomViewModel의 내비게이션 이벤트를 처리하는 부분
    LaunchedEffect(navController, studyRoomViewModel) {
        studyRoomViewModel.navigationEvents.collect { route ->
            navController.navigate(route)
        }
    }

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
                composable(Screen.Collection.name) { CollectionScreen() }
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
                // 딥링크 처리
                composable(
                    route = "${Screen.StudyRoom.name}?inviteId={inviteId}",
                    arguments = listOf(navArgument("inviteId") {
                        type = NavType.StringType
                        nullable = true
                    }),
                    deepLinks = listOf(
                        // App Links (https://) 하지만 작동은 안함
                        navDeepLink {
                            action = Intent.ACTION_VIEW
                            uriPattern = "https://pixbbo.netlify.app/study-room/{inviteId}"
                        },
                        // Custom Scheme (pixbbo://)
                        navDeepLink {
                            action = Intent.ACTION_VIEW
                            uriPattern = "pixbbo://study-room/{inviteId}"
                        }
                    )
                ) { backStackEntry ->
                    val inviteId = backStackEntry.arguments?.getString("inviteId")
                    ChallengeScreen(
                        authVM = authViewModel,
                        roomVM = studyRoomViewModel,
                        inviteStudyRoomId = inviteId, // ✅ 여기서 uuid 주입됨
                        onNavigateBack = { navController.navigate(Screen.Main.name) },
                        onNavigateToDelete = { navController.navigate(Screen.DeleteStudyRoom.name) }
                    )
                }
                // 챌린지룸 상세 화면을 위한 composable 경로
                composable(
                    route = "studyRoomDetail/{roomId}",
                    arguments = listOf(navArgument("roomId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val roomId = backStackEntry.arguments?.getString("roomId")
                    StudyRoomDetailScreen(
                        roomId = roomId,
                        roomVm = studyRoomViewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToChat = { studyRoomId ->
                            navController.navigate("chat/$studyRoomId")
                        }
                    )
                }
                composable(
                    route = "chat/{studyRoomId}",
                    arguments = listOf(navArgument("studyRoomId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val studyRoomId = backStackEntry.arguments?.getString("studyRoomId") ?: ""
                    ChatScreen(
                        studyRoomId = studyRoomId,
                        studyRoomViewModel = studyRoomViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.DeleteStudyRoom.name) {
                    DeleteStudyRoomScreen(
                        roomVM = studyRoomViewModel,
                        onNavigateBack = { navController.popBackStack() }
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