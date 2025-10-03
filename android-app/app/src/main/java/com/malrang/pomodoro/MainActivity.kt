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
import com.malrang.pomodoro.dataclass.ui.Mode
import com.malrang.pomodoro.networkRepo.SupabaseProvider
import com.malrang.pomodoro.service.AppUsageMonitoringService
import com.malrang.pomodoro.service.TimerService
import com.malrang.pomodoro.service.WarningOverlayService
import com.malrang.pomodoro.ui.PomodoroApp
import com.malrang.pomodoro.ui.theme.PomodoroTheme
import com.malrang.pomodoro.viewmodel.AuthVMFactory
import com.malrang.pomodoro.viewmodel.AuthViewModel
import com.malrang.pomodoro.viewmodel.PomodoroVMFactory
import com.malrang.pomodoro.viewmodel.PomodoroViewModel
import com.malrang.pomodoro.viewmodel.StudyRoomVMFactory
import com.malrang.pomodoro.viewmodel.StudyRoomViewModel
import io.github.jan.supabase.auth.handleDeeplinks

class MainActivity : ComponentActivity() {

    private val vm: PomodoroViewModel by viewModels { PomodoroVMFactory(application) }
    private val authVm: AuthViewModel by viewModels { AuthVMFactory(SupabaseProvider.client) }
    private val roomVm: StudyRoomViewModel by viewModels { StudyRoomVMFactory() }

    private val timerUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // [수정] 여러 Action을 처리하기 위해 if문에서 when문으로 변경
            when (intent?.action) {
                TimerService.TIMER_TICK -> {
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
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ 딥링크 처리 추가
        SupabaseProvider.client.handleDeeplinks(intent)

        enableEdgeToEdge()
        setContent {
            EnterFullScreen()
            PomodoroTheme {
                Scaffold {
                    Box(modifier = Modifier.padding(it)) {
                        PomodoroApp(vm, authVm, roomVm)
//                        BackPressExit()
                    }
                }
            }
        }
    }

    @Composable
    fun EnterFullScreen() {
        val view = LocalView.current
        val window = (view.context as? Activity)?.window ?: return

        // LaunchedEffect를 사용하여 Composable이 화면에 처음 나타날 때 한 번만 실행되도록 합니다.
        LaunchedEffect(Unit) {
            val insetsController = WindowCompat.getInsetsController(window, view)

            // 1. 상태 표시줄과 네비게이션 바를 모두 숨깁니다.
            insetsController.hide(WindowInsetsCompat.Type.systemBars())

            // 2. 사용자가 화면을 스와이프했을 때만 시스템 바가 일시적으로 나타나도록 동작을 설정합니다.
            //    잠시 후 자동으로 다시 사라집니다.
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // 앱이 이미 실행 중일 때에도 딥링크를 처리하고,
        SupabaseProvider.client.handleDeeplinks(intent)
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onResume() {
        super.onResume()
        // [수정] BroadcastReceiver가 두 가지 Action을 모두 수신하도록 필터에 추가
        val timerFilter = IntentFilter().apply {
            addAction(TimerService.TIMER_TICK)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                timerUpdateReceiver,
                timerFilter,
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(timerUpdateReceiver, timerFilter)
        }

        // [수정] 앱이 포그라운드로 돌아올 때 서비스의 최신 상태를 요청하고, 동물 목록을 새로고침합니다.
        vm.requestTimerStatus()

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
