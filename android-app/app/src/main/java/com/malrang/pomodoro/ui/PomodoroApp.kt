package com.malrang.pomodoro.ui

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
import com.malrang.pomodoro.ui.screen.stats.StatsScreen
import com.malrang.pomodoro.ui.screen.whitelist.WhitelistScreen
import com.malrang.pomodoro.viewmodel.AuthViewModel
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
    object Settings : BottomNavItem(Screen.Settings, "설정", R.drawable.ic_settings)
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
) {
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState()
    val permissionUiState by permissionViewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()

    val allPermissionsGranted =
        permissionUiState.permissions.isNotEmpty() &&
                permissionUiState.permissions.all { it.isGranted }

    LaunchedEffect(authState) {
        permissionViewModel.checkAndUpdatePermissions(context)
    }

    val isLoading = authState is AuthViewModel.AuthState.Loading ||
            authState is AuthViewModel.AuthState.Idle ||
            authState is AuthViewModel.AuthState.WaitingForRedirect

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
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

        // [수정] 네비게이션 바 배경은 항상 투명하게 설정 (배경 이미지가 비치도록)
        val navBarContainerColor = Color.Transparent

        // [수정] Scaffold 배경: 메인 화면은 투명(MainScreen이 그리기 위함), 그 외는 테마 기본값
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
            // [수정] NavHost에 전역 padding 제거
            NavHost(
                navController = navController,
                startDestination = startDestination
                // modifier = Modifier.padding(innerPadding) // 제거됨
            ) {
                composable(Screen.Main.name) {
                    // MainScreen에는 paddingValues를 전달하여 내부에서 처리 (배경은 무시, 콘텐츠는 적용)
                    MainScreen(
                        timerViewModel = timerViewModel,
                        settingsViewModel = settingsViewModel,
                        onNavigateTo = { screen -> navController.navigate(screen.name) },
                        paddingValues = innerPadding
                    )
                }
                // 다른 화면들은 padding을 적용하여 네비게이션 바와 겹치지 않도록 함
                composable(Screen.Stats.name) {
                    Box(modifier = Modifier.padding(innerPadding)) {
                        StatsScreen(
                            statsViewModel = statsViewModel,
                            onNavigateTo = { screen -> navController.navigate(screen.name) }
                        )
                    }
                }
                composable(Screen.Settings.name) {
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
                    Box(modifier = Modifier.padding(innerPadding)) {
                        WhitelistScreen(
                            settingsViewModel = settingsViewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
                composable(Screen.AccountSettings.name) {
                    Box(modifier = Modifier.padding(innerPadding)) {
                        AccountSettingsScreen(
                            authViewModel = authViewModel,
                            onNavigateTo = { navController.navigate(Screen.Main.name) }
                        )
                    }
                }
                composable(Screen.Background.name) {
                    Box(modifier = Modifier.padding(innerPadding)) {
                        BackgroundScreen(settingsViewModel = settingsViewModel)
                    }
                }
            }
        }
    }
}