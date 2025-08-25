package com.malrang.pomodoro

import android.Manifest
import android.app.AppOpsManager
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
import android.widget.Toast
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
import androidx.lifecycle.lifecycleScope
import com.malrang.pomodoro.dataclass.ui.Mode
import com.malrang.pomodoro.localRepo.PomodoroRepository
import com.malrang.pomodoro.service.AppUsageMonitoringService
import com.malrang.pomodoro.service.TimerService
import com.malrang.pomodoro.service.TimerServiceProvider
import com.malrang.pomodoro.ui.PomodoroApp
import com.malrang.pomodoro.ui.theme.PomodoroTheme
import com.malrang.pomodoro.viewmodel.PomodoroViewModel
import com.malrang.withpet.BackPressExit
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val vm: PomodoroViewModel by viewModels { PomodoroVMFactory(application) }

    // ... (timerUpdateReceiver, requestPermissionLauncher는 기존과 동일) ...
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
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        askNotificationPermission()

        setContent {
            PomodoroTheme {
                Scaffold {
                    Box(modifier = Modifier.padding(it)){
                        PomodoroApp(vm)
                        BackPressExit()
                    }
                }
            }
        }

        // ✅ 권한 확인
        if (!hasUsageStatsPermission()) {
            requestUsageStatsPermission()
        }
        if (!Settings.canDrawOverlays(this)) {
            requestOverlayPermission()
        }

        lifecycleScope.launch {
            vm.uiState
                .map { it.isRunning && it.currentMode == Mode.STUDY }
                .distinctUntilChanged()
                .collect { isStudying ->
                    if (isStudying) {
                        startAppMonitoringService(vm.uiState.value.whitelistedApps)
                    } else {
                        stopAppMonitoringService()
                    }
                }
        }
    }

    override fun onResume() {
        super.onResume()
        val timerFilter = IntentFilter(TimerService.TIMER_TICK)
        registerReceiver(timerUpdateReceiver, timerFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(timerUpdateReceiver)
    }

    // ... (onDestroy, askNotificationPermission, hasUsageStatsPermission, requestUsageStatsPermission는 기존과 동일) ...
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
    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }
    private fun requestUsageStatsPermission() {
        Toast.makeText(this, "공부 중 다른 앱 사용 감지를 위해 권한이 필요합니다.", Toast.LENGTH_LONG).show()
        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }

    // ✅ 다른 앱 위에 그리기 권한을 요청하는 함수
    private fun requestOverlayPermission() {
        Toast.makeText(this, "경고창을 표시하기 위해 다른 앱 위에 그리기 권한이 필요합니다.", Toast.LENGTH_LONG).show()
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    private fun startAppMonitoringService(whitelist: Set<String>) {
        if (!hasUsageStatsPermission() || !Settings.canDrawOverlays(this)) return
        val intent = Intent(this, AppUsageMonitoringService::class.java).apply {
            putExtra("WHITELISTED_APPS", whitelist.toTypedArray())
        }
        startService(intent)
    }

    private fun stopAppMonitoringService() {
        // ✅ 서비스를 중지할 때 STOP 액션을 보내 오버레이도 확실히 제거하도록 함
        val intent = Intent(this, AppUsageMonitoringService::class.java).apply {
            action = "STOP"
        }
        startService(intent)
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