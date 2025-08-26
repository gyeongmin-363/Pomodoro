package com.malrang.pomodoro

import android.Manifest
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.malrang.pomodoro.dataclass.ui.Mode
import com.malrang.pomodoro.dataclass.ui.PermissionType
import com.malrang.pomodoro.dataclass.ui.Screen
import com.malrang.pomodoro.localRepo.PomodoroRepository
import com.malrang.pomodoro.service.AppUsageMonitoringService
import com.malrang.pomodoro.service.TimerService
import com.malrang.pomodoro.service.TimerServiceProvider
import com.malrang.pomodoro.service.WarningOverlayService
import com.malrang.pomodoro.ui.PomodoroApp
import com.malrang.pomodoro.ui.theme.PomodoroTheme
import com.malrang.pomodoro.viewmodel.PomodoroViewModel
import com.malrang.withpet.BackPressExit

class MainActivity : ComponentActivity() {

    private val vm: PomodoroViewModel by viewModels { PomodoroVMFactory(application) }

    private val timerUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == TimerService.TIMER_TICK) {
                val timeLeft = intent.getIntExtra("TIME_LEFT", 0)
                val isRunning = intent.getBooleanExtra("IS_RUNNING", false)
                val currentMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getSerializableExtra("CURRENT_MODE", Mode::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getSerializableExtra("CURRENT_MODE") as? Mode
                } ?: Mode.STUDY
                val totalSessions = intent.getIntExtra("TOTAL_SESSIONS", 0)
                vm.updateTimerStateFromService(timeLeft, isRunning, currentMode, totalSessions)
            }
        }
    }

    // ✅ 권한 요청 런처들을 통합 관리합니다.
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // 권한 요청 후 상태를 다시 확인합니다.
        checkPermissionsAndNavigate()
    }

    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // 설정 화면에서 돌아온 후 상태를 다시 확인합니다.
        checkPermissionsAndNavigate()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PomodoroTheme {
                Scaffold {
                    Box(modifier = Modifier.padding(it)){
                        // ✅ 권한 요청 콜백을 PomodoroApp에 전달합니다.
                        PomodoroApp(vm, onRequestPermission = ::requestNextPermission)
                        BackPressExit()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val timerFilter = IntentFilter(TimerService.TIMER_TICK)
        registerReceiver(timerUpdateReceiver, timerFilter)

        // ✅ 화면으로 돌아올 때마다 권한을 확인하고 적절한 화면으로 안내합니다.
        checkPermissionsAndNavigate()

        // ✅ 화면으로 돌아올 때마다 모든 감시와 경고를 중지시킵니다.
        stopAppMonitoringService()
        stopWarningOverlay()
    }

    override fun onStop() {
        super.onStop()
        val state = vm.uiState.value
        // ✅ 공부 중일 때만 감시 서비스를 시작합니다.
        if (state.isRunning && state.currentMode == Mode.STUDY) {
            // ✅ 현재 설정된 차단 모드와 화이트리스트를 함께 전달합니다.
            startAppMonitoringService(state.whitelistedApps, state.settings.blockMode)
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(timerUpdateReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (TimerService.isServiceActive()) {
            var hasNotificationPermission = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    hasNotificationPermission = false
                }
            }
            if (!hasNotificationPermission) {
                val stopIntent = Intent(this, TimerService::class.java)
                stopService(stopIntent)
            }
        }
    }

    // ✅ 권한 확인 및 화면 전환 로직
    private fun checkPermissionsAndNavigate() {
        val allGranted = vm.checkAndupdatePermissions(this)
        if (!allGranted) {
            vm.showScreen(Screen.Permission)
        } else {
            // 모든 권한이 있으면 메인 화면으로 (이미 메인 화면이라면 변경 없음)
            if(vm.uiState.value.currentScreen == Screen.Permission) {
                vm.showScreen(Screen.Main)
            }
        }
    }

    // ✅ 필요한 다음 권한을 요청하는 함수
    private fun requestNextPermission() {
        val nextPermission = vm.uiState.value.permissions.firstOrNull { !it.isGranted }
        when (nextPermission?.type) {
            PermissionType.NOTIFICATION -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            PermissionType.OVERLAY -> {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                settingsLauncher.launch(intent)
            }
            PermissionType.USAGE_STATS -> {
                settingsLauncher.launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
            else -> {
                // 요청할 권한이 없거나 모든 권한이 허용된 경우
                checkPermissionsAndNavigate()
            }
        }
    }

    private fun startAppMonitoringService(whitelist: Set<String>, blockMode: com.malrang.pomodoro.dataclass.ui.BlockMode) {
        // ✅ 권한 상태를 ViewModel에서 다시 확인합니다.
        if (vm.uiState.value.permissions.any { !it.isGranted }) return

        val intent = Intent(this, AppUsageMonitoringService::class.java).apply {
            putExtra("WHITELISTED_APPS", whitelist.toTypedArray())
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
}


class PomodoroVMFactory(private val app: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PomodoroViewModel::class.java)) {
            val localDatastoreRepo = PomodoroRepository(app)
            val timerServiceProvider = TimerServiceProvider(app)

            @Suppress("UNCHECKED_CAST")
            return PomodoroViewModel(
                repo = localDatastoreRepo,
                timerService = timerServiceProvider
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}