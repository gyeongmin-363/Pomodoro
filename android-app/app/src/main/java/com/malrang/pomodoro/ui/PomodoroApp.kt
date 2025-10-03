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
import com.malrang.pomodoro.dataclass.ui.BlockMode
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
import com.malrang.pomodoro.viewmodel.MainViewModel
import com.malrang.pomodoro.viewmodel.PermissionViewModel
import com.malrang.pomodoro.viewmodel.SettingsViewModel
import com.malrang.pomodoro.viewmodel.StatsViewModel
import com.malrang.pomodoro.viewmodel.StudyRoomViewModel
import com.malrang.pomodoro.viewmodel.TimerViewModel

@Composable
fun PomodoroApp(
    mainViewModel: MainViewModel,
    timerViewModel: TimerViewModel,
    settingsViewModel: SettingsViewModel,
    permissionViewModel: PermissionViewModel,
    statsViewModel : StatsViewModel,
    authViewModel: AuthViewModel,
    studyRoomViewModel: StudyRoomViewModel,
    startAppMonitoring: (Set<String>, BlockMode) -> Unit,
    stopAppMonitoring: () -> Unit,
    stopWarningOverlay: () -> Unit
) {
    val context = LocalContext.current
    val navController = rememberNavController()

    // 각 ViewModel의 상태를 수집합니다.
    val authState by authViewModel.authState.collectAsState()

    // MainViewModel의 내비게이션 이벤트를 구독하여 화면을 전환합니다.
    LaunchedEffect(navController, mainViewModel) {
        mainViewModel.navigationEvents.collect { screen ->
            navController.navigate(screen.name)
        }
    }

    // ✅ [수정된 부분] 앱 시작 시 권한을 확인하고 필요하면 권한 화면으로 이동합니다.
    // 이 로직은 이제 UI 레이어의 책임입니다.
    LaunchedEffect(Unit) {
        val allGranted = permissionViewModel.checkAndupdatePermissions(context)
        if (!allGranted) {
            mainViewModel.navigateTo(Screen.Permission)
        }
    }

    when (authState) {
        is AuthViewModel.AuthState.Authenticated -> {
            // ✅ 로그인이 된 상태이면 메인 앱 콘텐츠를 보여줍니다.
            NavHost(navController = navController, startDestination = Screen.Main.name) {
                composable(Screen.Main.name) {
                    MainScreen(
                        mainViewModel = mainViewModel,
                        timerViewModel = timerViewModel,
                        settingsViewModel = settingsViewModel
                    )
                }
                composable(Screen.Stats.name) {
                    StatsScreen(
                        statsViewModel = statsViewModel,
                        mainViewModel = mainViewModel
                    )
                }
                composable(Screen.Collection.name) { CollectionScreen() }
                composable(Screen.Settings.name) {
                    SettingsScreen(
                        settingsViewModel = settingsViewModel,
                        mainViewModel = mainViewModel,
                        onSave = {
                            settingsViewModel.saveSettingsAndReset { newSettings ->
                                timerViewModel.reset(newSettings)
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
                        onNavigateToMain = { mainViewModel.navigateTo(Screen.Main) }
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
                        onNavigateTo = mainViewModel::navigateTo
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
                        onNavigateBack = { mainViewModel.navigateTo(Screen.Main) },
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