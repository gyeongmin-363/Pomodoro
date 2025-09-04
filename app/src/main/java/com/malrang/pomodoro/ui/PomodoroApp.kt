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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.malrang.pomodoro.dataclass.ui.Screen
import com.malrang.pomodoro.ui.screen.CollectionScreen
import com.malrang.pomodoro.ui.screen.LoginScreen
import com.malrang.pomodoro.ui.screen.MainScreen
import com.malrang.pomodoro.ui.screen.PermissionScreen
import com.malrang.pomodoro.ui.screen.SettingsScreen
import com.malrang.pomodoro.ui.screen.StatsScreen
import com.malrang.pomodoro.ui.screen.StudyRoomDetailScreen
import com.malrang.pomodoro.ui.screen.UserScreen
import com.malrang.pomodoro.ui.screen.WhitelistScreen
import com.malrang.pomodoro.viewmodel.AuthViewModel
import com.malrang.pomodoro.viewmodel.PomodoroViewModel
import com.malrang.pomodoro.viewmodel.StudyRoomViewModel
import com.malrang.withpet.BackPressExit

/**
 * 앱의 메인 컴포저블 함수입니다.
 * 현재 UI 상태에 따라 적절한 화면을 표시하는 역할을 합니다.
 *
 * @param vm [PomodoroViewModel]의 인스턴스입니다.
+ * @param onRequestPermission 권한 설정 버튼 클릭 시 호출될 함수입니다.
 */
@Composable
fun PomodoroApp(
    vm: PomodoroViewModel = viewModel(),
    authVm: AuthViewModel = viewModel(),
    roomVm: StudyRoomViewModel = viewModel(),
) {
    // 1. 두 ViewModel의 상태를 모두 관찰합니다.
    val authState by authVm.uiState.collectAsState()
    val context = LocalContext.current

    val navController = rememberNavController()

    LaunchedEffect(Unit) {
        vm.navigationEvents.collect { screen ->
            navController.navigate(screen.name) {
                // 필요에 따라 popUpTo 같은 네비게이션 옵션을 설정할 수 있습니다.
                // 예: popUpTo(Screen.Main.name) { inclusive = true }
            }
        }
    }

    // ✅ [추가] StudyRoomViewModel의 네비게이션 이벤트를 처리합니다.
    LaunchedEffect(Unit) {
        roomVm.navigationEvents.collect { route ->
            navController.navigate(route)
        }
    }


    // 인증 상태가 'Authenticated'로 변경될 때만 권한 확인 로직을 실행합니다.
    LaunchedEffect(authState) {
        if (authState is AuthViewModel.AuthState.Authenticated) {
            // 로그인 성공 시점에 권한을 확인하고 필요한 화면으로 이동시킵니다.
            vm.checkPermissionsAndNavigateIfNeeded(context)
        }
    }

    // 3. 인증 상태에 따라 화면을 분기합니다.
    when (authState) {
        is AuthViewModel.AuthState.Authenticated -> {
            // ✅ 로그인이 완료되었을 때 NavHost를 통해 화면을 관리합니다.
            NavHost(navController = navController, startDestination = Screen.Main.name) {
                composable(Screen.Main.name) {
                    MainScreen(vm)
                    BackPressExit()
                }
                composable(Screen.Collection.name) { CollectionScreen(vm) }
                composable(Screen.Settings.name) { SettingsScreen(vm) }
                composable(Screen.Stats.name) { StatsScreen(vm) }
                composable(Screen.Whitelist.name) { WhitelistScreen(vm) }
                composable(Screen.Permission.name) { PermissionScreen(vm) }

                // ✅ [수정] 딥링크 처리를 위해 composable 정의를 수정합니다.
                composable(
                    route = "${Screen.StudyRoom.name}?inviteId={inviteId}",
                    arguments = listOf(navArgument("inviteId") {
                        type = NavType.StringType
                        nullable = true
                    }),
                    deepLinks = listOf(navDeepLink {
                        action = Intent.ACTION_VIEW
                        uriPattern = "https://pixbbo.netlify.app/study-room/{inviteId}"
                    })
                ) { backStackEntry ->
                    val inviteId = backStackEntry.arguments?.getString("inviteId")
                    UserScreen(
                        authVM = authVm,
                        roomVM = roomVm,
                        inviteStudyRoomId = inviteId, // 딥링크로 받은 ID를 전달합니다.
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                // ✅ [추가] 스터디룸 상세 화면을 위한 새로운 composable 경로
                composable(
                    route = "studyRoomDetail/{roomId}",
                    arguments = listOf(navArgument("roomId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val roomId = backStackEntry.arguments?.getString("roomId")
                    StudyRoomDetailScreen(
                        roomId = roomId,
                        roomVm = roomVm,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
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