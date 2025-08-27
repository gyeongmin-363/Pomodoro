package com.malrang.pomodoro

import android.Manifest
import android.annotation.SuppressLint
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
import androidx.core.net.toUri

class MainActivity : ComponentActivity() {

    private val vm: PomodoroViewModel by viewModels { PomodoroVMFactory(application) }

    private val timerUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == TimerService.TIMER_TICK) {
                val timeLeft = intent.getIntExtra("TIME_LEFT", 0)
                val isRunning = intent.getBooleanExtra("IS_RUNNING", false)

                // [설명] 티라미수(API 33) 이상에서는 getSerializableExtra의 두 번째 인자로 클래스 타입을 넘겨야 합니다.
                // 하위 버전과의 호환성을 위해 분기 처리했으며, 기존 코드가 올바르게 작성되었습니다.
                val currentMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getSerializableExtra("CURRENT_MODE", Mode::class.java)
                } else {
                    // [설명] 하위 버전에서는 @Suppress("DEPRECATION")을 사용하여 이전 방식을 사용합니다.
                    @Suppress("DEPRECATION")
                    intent.getSerializableExtra("CURRENT_MODE") as? Mode
                } ?: Mode.STUDY

                val totalSessions = intent.getIntExtra("TOTAL_SESSIONS", 0)
                vm.updateTimerStateFromService(timeLeft, isRunning, currentMode, totalSessions)
            }
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        checkPermissionsAndNavigate()
    }

    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        checkPermissionsAndNavigate()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PomodoroTheme {
                Scaffold {
                    Box(modifier = Modifier.padding(it)) {
                        PomodoroApp(vm, onRequestPermission = ::requestNextPermission)
                        BackPressExit()
                    }
                }
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onResume() {
        super.onResume()
        val timerFilter = IntentFilter(TimerService.TIMER_TICK)

        // [설명] 티라미수(API 33) 이상에서는 리시버 등록 시 데이터 수신 가능 여부(exported)를 명시해야 합니다.
        // RECEIVER_NOT_EXPORTED는 앱 내부에서만 브로드캐스트를 수신하겠다는 의미입니다.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                timerUpdateReceiver,
                timerFilter,
                Context.RECEIVER_NOT_EXPORTED // 티라미수 이상에서 추가된 플래그
            )
        } else {
            // [설명] 하위 버전에서는 해당 플래그 없이, 이전 방식으로 리시버를 등록합니다.
            @Suppress("DEPRECATION")
            registerReceiver(timerUpdateReceiver, timerFilter)
        }

        checkPermissionsAndNavigate()
        stopAppMonitoringService()
        stopWarningOverlay()
    }

    override fun onStop() {
        super.onStop()
        val state = vm.uiState.value
        if (state.isRunning && state.currentMode == Mode.STUDY) {
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
            // [설명] POST_NOTIFICATIONS 권한은 티라미수(API 33) 이상에서만 필요합니다.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    hasNotificationPermission = false
                }
            }
            // [설명] 티라미수 버전에서 알림 권한이 없다면 서비스를 종료합니다.
            // 하위 버전에서는 hasNotificationPermission이 항상 true이므로, 이 조건에 걸리지 않고 서비스가 유지됩니다.
            // 이는 올바른 동작입니다. (하위 버전은 앱 설치 시 알림 권한을 자동으로 획득)
            if (!hasNotificationPermission) {
                val stopIntent = Intent(this, TimerService::class.java)
                stopService(stopIntent)
            }
        }
    }

    private fun checkPermissionsAndNavigate() {
        val allGranted = vm.checkAndupdatePermissions(this)
        if (!allGranted) {
            vm.showScreen(Screen.Permission)
        } else {
            if (vm.uiState.value.currentScreen == Screen.Permission) {
                vm.showScreen(Screen.Main)
            }
        }
    }

    private fun requestNextPermission() {
        val nextPermission = vm.uiState.value.permissions.firstOrNull { !it.isGranted }
        when (nextPermission?.type) {
            PermissionType.NOTIFICATION -> {
                // [설명] POST_NOTIFICATIONS 권한 요청은 티라미수(API 33) 이상에서만 필요합니다.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                // [설명] 하위 버전에서는 이 권한이 존재하지 않으므로, 아무런 동작을 하지 않는 것이 맞습니다.
                // 따라서 'else' 블록이 없는 것이 올바른 구현입니다.
            }
            PermissionType.OVERLAY -> {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    "package:$packageName".toUri()
                )
                settingsLauncher.launch(intent)
            }
            PermissionType.USAGE_STATS -> {
                settingsLauncher.launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
            else -> {
                checkPermissionsAndNavigate()
            }
        }
    }

    private fun startAppMonitoringService(
        whitelist: Set<String>,
        blockMode: com.malrang.pomodoro.dataclass.ui.BlockMode
    ) {
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