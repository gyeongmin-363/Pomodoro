package com.malrang.pomodoro.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.malrang.pomodoro.dataclass.ui.Screen
import com.malrang.pomodoro.ui.screen.account.AccountSettingsScreen
import com.malrang.pomodoro.ui.screen.main.MainScreen
import com.malrang.pomodoro.ui.screen.permission.PermissionScreen
import com.malrang.pomodoro.ui.screen.setting.SettingsDetailScreen
import com.malrang.pomodoro.ui.screen.stats.month.StatsScreen
import com.malrang.pomodoro.ui.screen.whitelist.WhitelistScreen
import com.malrang.pomodoro.ui.screen.stats.daliyDetail.DailyDetailScreen
import com.malrang.pomodoro.ui.screen.setting.SettingsScreen
import com.malrang.pomodoro.viewmodel.AuthViewModel
import com.malrang.pomodoro.viewmodel.PermissionViewModel
import com.malrang.pomodoro.viewmodel.SettingsViewModel
import com.malrang.pomodoro.viewmodel.StatsViewModel
import com.malrang.pomodoro.viewmodel.TimerViewModel

sealed class BottomNavItem(
    val route: String,
    val emoji: String,
    val title: String
) {
    object Planner : BottomNavItem("planner", "📅", "플래너")
    object Focus : BottomNavItem(Screen.Main.name, "⏱️", "집중")
    object Social : BottomNavItem("social", "👥", "소셜")
    object Stats : BottomNavItem(Screen.Stats.name, "📊", "통계")
    // [수정] Settings 탭의 route를 AccountSettings로 변경
    object Settings : BottomNavItem(Screen.AccountSettings.name, "⚙️", "계정")
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

        val navItems = listOf(
            BottomNavItem.Planner,
            BottomNavItem.Focus,
            BottomNavItem.Social,
            BottomNavItem.Stats,
            BottomNavItem.Settings
        )

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

        val startDestination = if (!allPermissionsGranted) Screen.Permission.name else BottomNavItem.Focus.route
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        val currentRoute = currentDestination?.route

        val showBottomBar = currentRoute in navItems.map { it.route }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (showBottomBar) {
                    Column {
                        Divider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            thickness = 0.5.dp
                        )
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.background,
                            tonalElevation = 0.dp
                        ) {
                            navItems.forEach { item ->
                                val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true

                                NavigationBarItem(
                                    icon = { Text(text = item.emoji, fontSize = 24.sp) },
                                    label = {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = Color.Transparent,
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = startDestination
            ) {
                composable(BottomNavItem.Planner.route) {
                    DoubleBackToExit()
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                        Text("📅 플래너 화면 준비 중", style = MaterialTheme.typography.titleMedium)
                    }
                }

                composable(BottomNavItem.Focus.route) {
                    DoubleBackToExit()
                    MainScreen(
                        timerViewModel = timerViewModel,
                        settingsViewModel = settingsViewModel,
                        onNavigateTo = { screen -> navController.navigate(screen.name) },
                        paddingValues = innerPadding
                    )
                }

                composable(BottomNavItem.Social.route) {
                    DoubleBackToExit()
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                        Text("👥 소셜 공부방 화면 준비 중", style = MaterialTheme.typography.titleMedium)
                    }
                }

                composable(BottomNavItem.Stats.route) {
                    DoubleBackToExit()
                    Box(modifier = Modifier.padding(innerPadding)) {
                        StatsScreen(
                            statsViewModel = statsViewModel,
                            onNavigateToDetail = { date ->
                                navController.navigate("${Screen.DailyDetail.name}/${date}")
                            }
                        )
                    }
                }

                // [수정] 5번째 탭을 AccountSettingsScreen으로 연결
                composable(BottomNavItem.Settings.route) {
                    DoubleBackToExit()
                    Box(modifier = Modifier.padding(innerPadding)) {
                        AccountSettingsScreen(
                            authViewModel = authViewModel,
                        )
                    }
                }


                // [기존 SettingsScreen 로직 유지] 상세 설정 이동 등을 위해 필요할 수 있음
                composable(Screen.Settings.name) {
                    Box(modifier = Modifier.padding(innerPadding)) {
                        SettingsDetailScreen(
                            settingsViewModel = settingsViewModel,
                            onNavigateTo = { screen -> navController.navigate(screen.name) },
                            onSave = {
                                settingsViewModel.saveSettingsAndReset { newSettings ->
                                    timerViewModel.reset(newSettings)
                                    navController.navigate(BottomNavItem.Focus.route) {
                                        popUpTo(Screen.Settings.name) { inclusive = true }
                                    }
                                }
                            },
                            onCancel = {
                                navController.navigate(BottomNavItem.Focus.route) {
                                    popUpTo(Screen.Settings.name) { inclusive = true }
                                }
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
                                navController.navigate(BottomNavItem.Focus.route) {
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

                composable("${Screen.DailyDetail.name}/{dateString}") { backStackEntry ->
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