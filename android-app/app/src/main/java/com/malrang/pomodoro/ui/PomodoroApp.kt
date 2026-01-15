package com.malrang.pomodoro.ui

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
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
import com.malrang.pomodoro.ui.screen.setting.SettingsScreen
import com.malrang.pomodoro.ui.screen.stats.daliyDetail.DailyDetailScreen
import com.malrang.pomodoro.ui.screen.stats.month.StatsScreen
import com.malrang.pomodoro.ui.screen.whitelist.WhitelistScreen
import com.malrang.pomodoro.viewmodel.AuthViewModel
import com.malrang.pomodoro.viewmodel.PermissionViewModel
import com.malrang.pomodoro.viewmodel.SettingsViewModel
import com.malrang.pomodoro.viewmodel.StatsViewModel
import com.malrang.pomodoro.viewmodel.TimerViewModel

// [수정] BottomNavItem 정의: 아이콘 대신 이모지 사용, 순서 및 구성 변경
sealed class BottomNavItem(
    val route: String,
    val emoji: String,
    val title: String
) {
    object Planner : BottomNavItem("planner", "📅", "플래너") // (1) 할 일 관리 (신규)
    object Focus : BottomNavItem(Screen.Main.name, "⏱️", "집중")     // (2) 타이머 (기존 Main)
    object Social : BottomNavItem("social", "👥", "소셜")   // (3) 같이 공부 (신규)
    object Stats : BottomNavItem(Screen.Stats.name, "📊", "통계")     // (4) 통계
    object Settings : BottomNavItem(Screen.Settings.name, "⚙️", "설정") // (5) 설정
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

    // 권한 목록 로딩 상태 확인
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

        // [수정] 네비게이션 아이템 리스트 재정의 (순서 반영)
        val navItems = listOf(
            BottomNavItem.Planner,
            BottomNavItem.Focus,
            BottomNavItem.Social,
            BottomNavItem.Stats,
            BottomNavItem.Settings
        )

        // 더블 클릭 종료 로직
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
        val isMainScreen = currentRoute == BottomNavItem.Focus.route

        // 배경색 설정: 메인 화면일 때도 테마 배경색을 따르도록 하여 통일감 부여 (필요시 투명 처리)
        val scaffoldContainerColor = MaterialTheme.colorScheme.background

        Scaffold(
            containerColor = scaffoldContainerColor,
            bottomBar = {
                if (showBottomBar) {
                    // [수정] Notion Style 네비게이션 바 적용
                    Column {
                        // 상단 구분선 (아주 얇게)
                        Divider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            thickness = 0.5.dp
                        )
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.background, // 오프화이트 배경
                            tonalElevation = 0.dp // 그림자 제거 (Flat Design)
                        ) {
                            navItems.forEach { item ->
                                val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true

                                NavigationBarItem(
                                    icon = {
                                        // 아이콘 대신 이모지 텍스트 표시
                                        Text(text = item.emoji, fontSize = 24.sp)
                                    },
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
                                    // [수정] 선택 시 배경(Indicator) 제거 및 색상 조정
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = Color.Transparent, // 선택된 아이템 배경 투명
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
                // (1) Planner Screen (신규 플레이스홀더)
                composable(BottomNavItem.Planner.route) {
                    DoubleBackToExit()
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                        Text("📅 플래너 화면 준비 중", style = MaterialTheme.typography.titleMedium)
                    }
                }

                // (2) Focus Screen (기존 Main)
                composable(BottomNavItem.Focus.route) {
                    DoubleBackToExit()
                    MainScreen(
                        timerViewModel = timerViewModel,
                        settingsViewModel = settingsViewModel,
                        onNavigateTo = { screen -> navController.navigate(screen.name) },
                        paddingValues = innerPadding
                    )
                }

                // (3) Social Screen (신규 플레이스홀더)
                composable(BottomNavItem.Social.route) {
                    DoubleBackToExit()
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                        Text("👥 소셜 공부방 화면 준비 중", style = MaterialTheme.typography.titleMedium)
                    }
                }

                // (4) Stats Screen (기존 Stats)
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

                // (5) Settings Screen (기존 Settings)
                composable(BottomNavItem.Settings.route) {
                    DoubleBackToExit()
                    Box(modifier = Modifier.padding(innerPadding)) {
                        SettingsScreen(
                            settingsViewModel = settingsViewModel,
                            onNavigateTo = { screen -> navController.navigate(screen.name) },
                            onSave = {
                                settingsViewModel.saveSettingsAndReset { newSettings ->
                                    timerViewModel.reset(newSettings)
                                    navController.navigate(BottomNavItem.Focus.route) {
                                        popUpTo(BottomNavItem.Settings.route) { inclusive = true }
                                    }
                                }
                            },
                            onPresetSelected = { newSettings ->
                                timerViewModel.reset(newSettings)
                            }
                        )
                    }
                }

                // --- 기타 서브 화면들 ---

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

                composable(Screen.AccountSettings.name) {
                    // 계정 설정은 이제 BottomNav가 아니므로 서브 화면으로 처리하거나, Settings 내부로 통합 필요
                    // 현재는 별도 화면으로 유지
                    Box(modifier = Modifier.padding(innerPadding)) {
                        AccountSettingsScreen(
                            authViewModel = authViewModel,
                        )
                    }
                }


                composable(
                    route = "${Screen.DailyDetail.name}/{dateString}"
                ) { backStackEntry ->
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