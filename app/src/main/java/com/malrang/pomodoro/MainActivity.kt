package com.malrang.pomodoro

import android.Manifest
import android.app.Application
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.malrang.pomodoro.dataclass.ui.Mode
import com.malrang.pomodoro.localRepo.PomodoroRepository
import com.malrang.pomodoro.service.TimerService
import com.malrang.pomodoro.service.TimerServiceProvider
import com.malrang.pomodoro.ui.PomodoroApp
import com.malrang.pomodoro.ui.theme.PomodoroTheme
import com.malrang.pomodoro.viewmodel.PomodoroViewModel
import com.malrang.withpet.BackPressExit

/**
 * 앱의 메인 액티비티입니다.
 * 앱의 진입점 역할을 하며, [PomodoroApp] 컴포저블을 표시합니다.
 */
class MainActivity : ComponentActivity() {

    private lateinit var vm: PomodoroViewModel

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

                if (::vm.isInitialized) {
                    vm.updateTimerStateFromService(timeLeft, isRunning, currentMode, totalSessions)
                }
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // 권한 요청 결과 처리 (필요 시)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        askNotificationPermission()
        setContent {
            PomodoroTheme {
                vm = viewModel(factory = PomodoroVMFactory(application))
                PomodoroApp(vm)
                BackPressExit()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // IntentFilter에 TIMER_TICK 액션만 등록합니다.
        val filter = IntentFilter(TimerService.TIMER_TICK)
        registerReceiver(timerUpdateReceiver, filter)
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

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
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