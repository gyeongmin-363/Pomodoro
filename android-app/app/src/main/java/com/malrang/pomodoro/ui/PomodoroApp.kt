package com.malrang.pomodoro.ui

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.malrang.pomodoro.R
import com.malrang.pomodoro.dataclass.ui.Screen
import com.malrang.pomodoro.ui.screen.account.AccountSettingsScreen
import com.malrang.pomodoro.ui.screen.background.BackgroundScreen
import com.malrang.pomodoro.ui.screen.main.MainScreen
import com.malrang.pomodoro.ui.screen.permission.PermissionScreen
import com.malrang.pomodoro.ui.screen.setting.SettingsScreen
import com.malrang.pomodoro.ui.screen.stats.daliyDetail.DailyDetailScreen
import com.malrang.pomodoro.ui.screen.stats.month.StatsScreen
import com.malrang.pomodoro.ui.screen.whitelist.WhitelistScreen
import com.malrang.pomodoro.viewmodel.AuthViewModel
import com.malrang.pomodoro.viewmodel.BackgroundViewModel
import com.malrang.pomodoro.viewmodel.PermissionViewModel
import com.malrang.pomodoro.viewmodel.SettingsViewModel
import com.malrang.pomodoro.viewmodel.StatsViewModel
import com.malrang.pomodoro.viewmodel.TimerViewModel

sealed class BottomNavItem(
    val screen: Screen,
    val title: String,
    @DrawableRes val icon: Int
) {
    object Background : BottomNavItem(Screen.Background, "배경 설정", R.drawable.ic_wallpaper)
    object Settings : BottomNavItem(Screen.Settings, "프리셋", R.drawable.assignment_24px)
    object Home : BottomNavItem(Screen.Main, "타이머", R.drawable.ic_play)
    object Stats : BottomNavItem(Screen.Stats, "통계", R.drawable.ic_stats)
    object Account : BottomNavItem(Screen.AccountSettings, "계정", R.drawable.ic_user_attributes_24px)
}

@Composable
fun PomodoroApp(
    timerViewModel: TimerViewModel,
    settingsViewModel: SettingsViewModel,
    permissionViewModel: PermissionViewModel,
    statsViewModel: StatsViewModel,
    authViewModel: AuthViewModel,
    backgroundViewModel: BackgroundViewModel
) {
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState()
    val permissionUiState by permissionViewModel.uiState.collectAsState()

    // 권한 목록 로딩 상태 확인 (깜빡임 방지)
    val isPermissionReady = permissionUiState.permissions.isNotEmpty()
    val allPermissionsGranted = isPermissionReady && permissionUiState.permissions.all { it.isGranted }

    LaunchedEffect(authState) {
        permissionViewModel.checkAndUpdatePermissions(context)
    }

    val isLoading = authState is AuthViewModel.AuthState.Loading ||
            authState is AuthViewModel.AuthState.Idle ||
            authState is AuthViewModel.AuthState.WaitingForRedirect ||
            !isPermissionReady

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        val navController = rememberNavController()

        // BottomNavItem 리스트 정의
        val navItems = listOf(
            BottomNavItem.Background,
            BottomNavItem.Settings,
            BottomNavItem.Home,
            BottomNavItem.Stats,
            BottomNavItem.Account
        )

        // 더블 클릭 종료 로직을 수행하는 Composable 함수
        // 각 화면(Composable) 내부에서 호출하여 NavHost의 뒤로가기보다 우선순위를 높임
        @Composable
        fun DoubleBackToExit() {
            var backPressedTime by remember { mutableLongStateOf(0L) }
            val activity = LocalActivity.current

            BackHandler {
                if (System.currentTimeMillis() - backPressedTime < 2000) {
                    activity?.finish()
                } else {
                    backPressedTime = System.currentTimeMillis()
                    Toast.makeText(context, "'뒤로' 버튼을 한번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val startDestination = if (!allPermissionsGranted) Screen.Permission.name else Screen.Main.name
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        val currentRoute = currentDestination?.route

        val showBottomBar = currentRoute in navItems.map { it.screen.name }
        val navBarContainerColor = Color.Transparent
        val isMainScreen = currentRoute == Screen.Main.name
        val scaffoldContainerColor = if (isMainScreen) Color.Transparent else MaterialTheme.colorScheme.background

        Scaffold(
            containerColor = scaffoldContainerColor,
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar(
                        containerColor = navBarContainerColor
                    ) {
                        navItems.forEach { item ->
                            val selected = currentDestination?.hierarchy?.any { it.route == item.screen.name } == true
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        painter = painterResource(id = item.icon),
                                        contentDescription = item.title
                                    )
                                },
                                label = { Text(item.title) },
                                selected = selected,
                                onClick = {
                                    navController.navigate(item.screen.name) {
                                        // 탭 간 이동 시 스택이 무한히 쌓이는 것을 방지하기 위해 startDestination까지 pop 합니다.
                                        // 단, 아래의 DoubleBackToExit 핸들러 덕분에 '뒤로가기' 시에는 이 스택 구조를 무시하고 종료 로직이 실행됩니다.
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = startDestination
            ) {
                composable(Screen.Main.name) {
                    DoubleBackToExit() // [수정] 타이머 화면에서도 더블 클릭 종료 적용
                    MainScreen(
                        timerViewModel = timerViewModel,
                        settingsViewModel = settingsViewModel,
                        backgroundViewModel = backgroundViewModel,
                        onNavigateTo = { screen -> navController.navigate(screen.name) },
                        paddingValues = innerPadding
                    )
                }
                composable(Screen.Stats.name) {
                    DoubleBackToExit() // [수정] 통계 화면에서 뒤로가기 시 타이머로 가지 않고 종료 로직 실행
                    Box(modifier = Modifier.padding(innerPadding)) {
                        StatsScreen(
                            statsViewModel = statsViewModel,
                            onNavigateToDetail = { date ->
                                navController.navigate("${Screen.DailyDetail.name}/${date}")
                            }
                        )
                    }
                }
                composable(Screen.Settings.name) {
                    DoubleBackToExit() // [수정] 프리셋 화면에서 뒤로가기 시 타이머로 가지 않고 종료 로직 실행
                    Box(modifier = Modifier.padding(innerPadding)) {
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
                            },
                            onPresetSelected = { newSettings ->
                                timerViewModel.reset(newSettings)
                            }
                        )
                    }
                }
                composable(Screen.Permission.name) {
                    // 권한 화면은 최초 진입점이므로 별도 종료 로직이 필요할 수 있으나,
                    // BottomNavItem이 아니므로 기본 동작(앱 종료)을 따르거나 필요 시 DoubleBackToExit() 추가 가능.
                    Box(modifier = Modifier.padding(innerPadding)) {
                        val permissionUiStateVal by permissionViewModel.uiState.collectAsState()
                        PermissionScreen(
                            permissionUiState = permissionUiStateVal,
                            onPermissionResult = { permissionViewModel.onPermissionRequestResult(context) },
                            onSetPermissionAttempted = permissionViewModel::setPermissionAttemptedInSession,
                            onNavigateTo = {
                                navController.navigate(Screen.Main.name) {
                                    popUpTo(Screen.Permission.name) { inclusive = true }
                                }
                            }
                        )
                    }
                }
                composable(Screen.Whitelist.name) {
                    // Whitelist는 BottomNavItem이 아님 -> 기본 뒤로가기(popBackStack) 동작 유지
                    Box(modifier = Modifier.padding(innerPadding)) {
                        WhitelistScreen(
                            settingsViewModel = settingsViewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
                composable(Screen.AccountSettings.name) {
                    DoubleBackToExit() // [수정] 계정 화면에서 뒤로가기 시 타이머로 가지 않고 종료 로직 실행
                    Box(modifier = Modifier.padding(innerPadding)) {
                        AccountSettingsScreen(
                            authViewModel = authViewModel,
                        )
                    }
                }
                composable(Screen.Background.name) {
                    DoubleBackToExit() // [수정] 배경 화면에서 뒤로가기 시 타이머로 가지 않고 종료 로직 실행
                    Box(modifier = Modifier.padding(innerPadding)) {
                        BackgroundScreen(backgroundViewModel = backgroundViewModel)
                    }
                }
                composable(
                    route = "${Screen.DailyDetail.name}/{dateString}"
                ) { backStackEntry ->
                    // 상세 화면은 BottomNavItem이 아님 -> 기본 뒤로가기 동작 유지
                    val dateString = backStackEntry.arguments?.getString("dateString")
                    DailyDetailScreen(
                        dateString = dateString,
                        statsViewModel = statsViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}