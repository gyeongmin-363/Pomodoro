package com.malrang.pomodoro

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.malrang.pomodoro.networkRepo.SupabaseProvider
import com.malrang.pomodoro.service.AppUsageMonitoringService
import com.malrang.pomodoro.service.TimerService
import com.malrang.pomodoro.service.WarningOverlayService
import com.malrang.pomodoro.ui.PomodoroApp
import com.malrang.pomodoro.ui.theme.PomodoroTheme
import com.malrang.pomodoro.viewmodel.AppViewModelFactory
import com.malrang.pomodoro.viewmodel.AuthVMFactory
import com.malrang.pomodoro.viewmodel.AuthViewModel
import com.malrang.pomodoro.viewmodel.PermissionViewModel
import com.malrang.pomodoro.viewmodel.SettingsViewModel
import com.malrang.pomodoro.viewmodel.StatsViewModel
import com.malrang.pomodoro.viewmodel.TimerViewModel
import io.github.jan.supabase.auth.handleDeeplinks

class MainActivity : ComponentActivity() {
    // 분리된 ViewModel들을 AppViewModelFactory를 사용해 초기화합니다.
    private val timerViewModel: TimerViewModel by viewModels { AppViewModelFactory(application) }
    private val settingsViewModel: SettingsViewModel by viewModels { AppViewModelFactory(application) }
    private val permissionViewModel: PermissionViewModel by viewModels { AppViewModelFactory(application) }
    private val statsViewModel: StatsViewModel by viewModels { AppViewModelFactory(application) }
    private val authViewModel: AuthViewModel by viewModels { AuthVMFactory(SupabaseProvider.client) }


    // 👇 [추가] 데이터 업데이트를 수신할 BroadcastReceiver
    private val dataUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == TimerService.ACTION_DATA_UPDATED) {
                // 통계 정보도 새로고침
                statsViewModel.loadDailyStats()
            }
        }
    }

    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == TimerService.ACTION_STATUS_UPDATE) {
                val timeLeft = intent.getIntExtra(TimerService.EXTRA_TIME_LEFT, 0)
                val isRunning = intent.getBooleanExtra(TimerService.EXTRA_IS_RUNNING, false)
                val currentModeName = intent.getStringExtra(TimerService.EXTRA_CURRENT_MODE)
                val totalSessions = intent.getIntExtra(TimerService.EXTRA_TOTAL_SESSIONS, 0)
                val currentMode = currentModeName?.let {
                    try {
                        com.malrang.pomodoro.dataclass.ui.Mode.valueOf(it)
                    } catch (e: IllegalArgumentException) {
                        com.malrang.pomodoro.dataclass.ui.Mode.STUDY
                    }
                } ?: com.malrang.pomodoro.dataclass.ui.Mode.STUDY
                // BroadcastReceiver가 TimerViewModel을 직접 업데이트하도록 수정합니다.
                timerViewModel.updateTimerStateFromService(timeLeft, isRunning, currentMode, totalSessions)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SupabaseProvider.client.handleDeeplinks(intent)

        val intentFilter = IntentFilter(TimerService.ACTION_DATA_UPDATED)
        registerReceiver(dataUpdateReceiver, intentFilter, RECEIVER_NOT_EXPORTED)

        enableEdgeToEdge()
        setContent {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            HideSystemBars()
            PomodoroTheme {
                Scaffold { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        // PomodoroApp에 모든 ViewModel을 전달합니다.
                        PomodoroApp(
                            timerViewModel = timerViewModel,
                            settingsViewModel = settingsViewModel,
                            permissionViewModel = permissionViewModel,
                            statsViewModel = statsViewModel,
                            authViewModel = authViewModel,
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // 앱이 화면에 보이기 시작하면 리시버를 등록합니다. (onStop과 짝을 이룸)
        val filter = IntentFilter(TimerService.ACTION_STATUS_UPDATE)
        ContextCompat.registerReceiver(this, updateReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onStop() {
        super.onStop()
        // [추가] 앱이 백그라운드로 전환될 때, 학습 중이면 앱 모니터링 서비스를 시작하는 로직
        val timerState = timerViewModel.uiState.value
        val settingsState = settingsViewModel.uiState.value
        if (timerState.isRunning && timerState.currentMode == com.malrang.pomodoro.dataclass.ui.Mode.STUDY) {
            // [수정] whitelistedApps -> blockedApps 로 변경
            startAppMonitoringService(settingsState.blockedApps, settingsState.settings.blockMode)
        }
        unregisterReceiver(updateReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(dataUpdateReceiver)

        if (TimerService.isServiceActive()) {
            var hasNotificationPermission = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    hasNotificationPermission = false
                }
            }
            if (!hasNotificationPermission) {
                val stopIntent = Intent(this, TimerService::class.java)
                stopService(stopIntent)
            }
        }
    }

    // [수정] 파라미터 이름 변경 (whitelist -> blockedApps)
    private fun startAppMonitoringService(
        blockedApps: Set<String>,
        blockMode: com.malrang.pomodoro.dataclass.ui.BlockMode
    ) {
        // 권한 확인 로직을 PermissionViewModel의 상태를 사용하도록 수정합니다.
        if (permissionViewModel.uiState.value.permissions.any { !it.isGranted }) return

        // [수정] AppUsageMonitoringService가 DataStore를 직접 구독하도록 변경되었으므로,
        // Intent에 목록을 넣을 필요가 없어졌습니다. (서비스 시작만 호출)
        val intent = Intent(this, AppUsageMonitoringService::class.java).apply {
            // 필요하다면 모드 정보 정도는 넘길 수 있으나, 서비스가 DataStore를 구독하므로 필수는 아닙니다.
            // 여기서는 명시적으로 시작 의도를 알리기 위해 남겨두거나 제거할 수 있습니다.
            // 기존 코드 호환성을 위해 Block Mode만 남기거나 제거해도 무방합니다.
            putExtra("BLOCK_MODE", blockMode.name)
        }
        startService(intent)
    }

    private fun stopAppMonitoringService() {
        stopService(Intent(this, AppUsageMonitoringService::class.java))
    }

    private fun stopWarningOverlay() {
        stopService(Intent(this, WarningOverlayService::class.java))
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // 앱이 이미 실행 중일 때에도 딥링크를 처리하고,
        SupabaseProvider.client.handleDeeplinks(intent)
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onResume() {
        super.onResume()

        // 앱이 포그라운드로 돌아올 때 서비스의 최신 상태를 요청하고, 동물 목록을 새로고침합니다.
        // onStart에서 등록한 리시버가 이 요청에 대한 응답을 받아서 처리해줍니다.
        timerViewModel.requestTimerStatus()

        // 방해 금지 모니터링이나 경고창은 앱을 보고 있을 땐 필요 없으므로 끕니다.
        stopAppMonitoringService()
        stopWarningOverlay()
    }

}

@SuppressLint("ComposableNaming")
@Composable
private fun Activity.HideSystemBars() {
    val view = LocalView.current
    val window = window
    LaunchedEffect(Unit) {
        WindowCompat.getInsetsController(window, view).apply {
            //상태 표시줄과 네비게이션 바를 모두 숨깁니다.
            hide(WindowInsetsCompat.Type.systemBars())

            //사용자가 화면을 스와이프했을 때만 시스템 바가 일시적으로 나타나도록 동작을 설정합니다.
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}