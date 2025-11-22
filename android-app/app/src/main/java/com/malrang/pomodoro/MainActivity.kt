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
import android.widget.Toast
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
import androidx.lifecycle.lifecycleScope
import com.malrang.pomodoro.localRepo.PomodoroRepository
import com.malrang.pomodoro.networkRepo.SupabaseProvider
import com.malrang.pomodoro.networkRepo.SupabaseRepository
import com.malrang.pomodoro.service.AppUsageMonitoringService
import com.malrang.pomodoro.service.TimerService
import com.malrang.pomodoro.service.WarningOverlayService
import com.malrang.pomodoro.ui.PomodoroApp
import com.malrang.pomodoro.ui.theme.PomodoroTheme
import com.malrang.pomodoro.viewmodel.AppViewModelFactory
import com.malrang.pomodoro.viewmodel.AuthVMFactory
import com.malrang.pomodoro.viewmodel.AuthViewModel
import com.malrang.pomodoro.viewmodel.BackgroundViewModel // [추가]
import com.malrang.pomodoro.viewmodel.PermissionViewModel
import com.malrang.pomodoro.viewmodel.SettingsViewModel
import com.malrang.pomodoro.viewmodel.StatsViewModel
import com.malrang.pomodoro.viewmodel.TimerViewModel
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    // 분리된 ViewModel들을 AppViewModelFactory를 사용해 초기화합니다.
    private val timerViewModel: TimerViewModel by viewModels { AppViewModelFactory(application) }
    private val settingsViewModel: SettingsViewModel by viewModels { AppViewModelFactory(application) }
    private val permissionViewModel: PermissionViewModel by viewModels { AppViewModelFactory(application) }
    private val statsViewModel: StatsViewModel by viewModels { AppViewModelFactory(application) }
    private val authViewModel: AuthViewModel by viewModels { AuthVMFactory(SupabaseProvider.client) }
    // [추가] BackgroundViewModel 초기화
    private val backgroundViewModel: BackgroundViewModel by viewModels { AppViewModelFactory(application) }

    private lateinit var supabaseRepo: SupabaseRepository
    private lateinit var localRepo: PomodoroRepository

    private val dataUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == TimerService.ACTION_DATA_UPDATED) {
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

                timerViewModel.updateTimerStateFromService(timeLeft, isRunning, currentMode, totalSessions)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SupabaseProvider.client.handleDeeplinks(intent)

        supabaseRepo = SupabaseRepository(SupabaseProvider.client.postgrest, SupabaseProvider.client.storage)
        localRepo = PomodoroRepository(applicationContext)

        val intentFilter = IntentFilter(TimerService.ACTION_DATA_UPDATED)
        registerReceiver(dataUpdateReceiver, intentFilter, RECEIVER_NOT_EXPORTED)

        enableEdgeToEdge()
        setContent {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            HideSystemBars()
            PomodoroTheme {
                Scaffold { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        // [수정] backgroundViewModel 전달
                        PomodoroApp(
                            timerViewModel = timerViewModel,
                            settingsViewModel = settingsViewModel,
                            permissionViewModel = permissionViewModel,
                            statsViewModel = statsViewModel,
                            authViewModel = authViewModel,
                            backgroundViewModel = backgroundViewModel, // 전달
                            onSyncClick = { performSync() }
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(TimerService.ACTION_STATUS_UPDATE)
        ContextCompat.registerReceiver(this, updateReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        performSync(silent = true)
    }

    private fun performSync(silent: Boolean = false) {
        val userId = SupabaseProvider.client.auth.currentUserOrNull()?.id ?: return

        lifecycleScope.launch {
            try {
                if (!silent) Toast.makeText(this@MainActivity, "동기화 중...", Toast.LENGTH_SHORT).show()

                val remoteStats = supabaseRepo.getDailyStats(userId)
                val remotePresets = supabaseRepo.getWorkPresets(userId)

                if (remoteStats.isNotEmpty()) {
                    val currentStats = localRepo.loadDailyStats().toMutableMap()
                    remoteStats.forEach { stat ->
                        currentStats[stat.date] = stat
                    }
                    localRepo.saveDailyStats(currentStats)
                }

                if (remotePresets.isNotEmpty()) {
                    val mergedPresets = remotePresets.toMutableList()
                    localRepo.insertNewWorkPresets(mergedPresets)

                    val localPresets = localRepo.loadWorkPresets()
                    val remoteIds = remotePresets.map { it.id }.toSet()
                    val toDelete = localPresets.filter { it.id !in remoteIds }

                    toDelete.forEach {
                        localRepo.deleteWorkPreset(it.id)
                    }
                    settingsViewModel.refreshPresets()
                }

                val currentPresets = localRepo.loadWorkPresets()
                supabaseRepo.upsertWorkPresets(userId, currentPresets)

                val localStats = localRepo.loadDailyStats()
                localStats.values.forEach { stat ->
                    supabaseRepo.upsertDailyStat(userId, stat)
                }

                statsViewModel.loadDailyStats()
                if (!silent) Toast.makeText(this@MainActivity, "동기화 완료!", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                e.printStackTrace()
                if (!silent) Toast.makeText(this@MainActivity, "동기화 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        val timerState = timerViewModel.uiState.value
        val settingsState = settingsViewModel.uiState.value
        if (timerState.isRunning && timerState.currentMode == com.malrang.pomodoro.dataclass.ui.Mode.STUDY) {
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

    private fun startAppMonitoringService(
        blockedApps: Set<String>,
        blockMode: com.malrang.pomodoro.dataclass.ui.BlockMode
    ) {
        if (permissionViewModel.uiState.value.permissions.any { !it.isGranted }) return

        val intent = Intent(this, AppUsageMonitoringService::class.java).apply {
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
        SupabaseProvider.client.handleDeeplinks(intent)
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onResume() {
        super.onResume()
        timerViewModel.requestTimerStatus()
        stopAppMonitoringService()
        stopWarningOverlay()
    }

    @SuppressLint("ComposableNaming")
    @Composable
    private fun Activity.HideSystemBars() {
        val view = LocalView.current
        val window = window
        LaunchedEffect(Unit) {
            WindowCompat.getInsetsController(window, view).apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }
}