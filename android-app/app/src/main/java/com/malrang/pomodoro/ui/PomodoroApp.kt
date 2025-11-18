package com.malrang.pomodoro.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

// ✅ 수정됨: iconRes(Int) 하나만 사용하도록 변경
sealed class BottomNavItem(
    val screen: Screen,
    val title: String,
    @DrawableRes val icon: Int // 리소스 ID만 받음
) {
    // 업로드된 리소스 파일들을 매핑했습니다.
    object Background : BottomNavItem(Screen.Background, "배경", R.drawable.ic_wallpaper)
    object Settings : BottomNavItem(Screen.Settings, "설정", R.drawable.ic_settings)

    // '타이머'는 ic_play (또는 적절한 ic_timer가 있다면 교체) 사용
    object Home : BottomNavItem(Screen.Main, "타이머", R.drawable.ic_play)

    object Stats : BottomNavItem(Screen.Stats, "통계", R.drawable.ic_stats)

    // '계정'은 ic_user_attributes_24px 사용
    object Account : BottomNavItem(Screen.AccountSettings, "계정", R.drawable.ic_user_attributes_24px)
}

@Composable
fun PomodoroApp(
    timerViewModel: TimerViewModel,
    settingsViewModel: SettingsViewModel,
    permissionViewModel: PermissionViewModel,
    statsViewModel: StatsViewModel,
    authViewModel: AuthViewModel,
) {
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState()
    val permissionUiState by permissionViewModel.uiState.collectAsState()
    val allPermissionsGranted =
        permissionUiState.permissions.isNotEmpty() &&
                permissionUiState.permissions.all { it.isGranted }

    LaunchedEffect(authState) {
        if (authState is AuthViewModel.AuthState.Authenticated) {
            permissionViewModel.checkAndUpdatePermissions(context)
        }
    }

    when (authState) {
        is AuthViewModel.AuthState.Authenticated -> {
            val navController = rememberNavController()

            val navItems = listOf(
                BottomNavItem.Background,
                BottomNavItem.Settings,
                BottomNavItem.Home,
                BottomNavItem.Stats,
                BottomNavItem.Account
            )

            val startDestination = if (!allPermissionsGranted) Screen.Permission.name else Screen.Main.name

            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val currentRoute = currentDestination?.route

            val showBottomBar = currentRoute in navItems.map { it.screen.name }

            Scaffold(
                bottomBar = {
                    if (showBottomBar) {
                        NavigationBar {
                            navItems.forEach { item ->
                                val selected = currentDestination?.hierarchy?.any { it.route == item.screen.name } == true
                                NavigationBarItem(
                                    // ✅ 수정됨: painterResource를 사용하여 리소스 ID로 아이콘을 그림
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
                    startDestination = startDestination,
                    modifier = Modifier.padding(innerPadding)
                ) {
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
                    composable(Screen.Background.name) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("배경 설정 (기상 API 연동 예정)")
                        }
                    }
                }
            }
        }
        is AuthViewModel.AuthState.NotAuthenticated,
        is AuthViewModel.AuthState.Idle,
        is AuthViewModel.AuthState.Error -> {
            LoginScreen(viewModel = authViewModel)
        }
        is AuthViewModel.AuthState.Loading,
        is AuthViewModel.AuthState.WaitingForRedirect -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}