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

    // [추가] 동기화를 위한 레포지토리 직접 생성 (ViewModel을 거치지 않고 전역 동기화를 수행하기 위함)
    private lateinit var supabaseRepo: SupabaseRepository
    private lateinit var localRepo: PomodoroRepository

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

        // 레포지토리 초기화
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
                        // PomodoroApp에 모든 ViewModel을 전달합니다.
                        PomodoroApp(
                            timerViewModel = timerViewModel,
                            settingsViewModel = settingsViewModel,
                            permissionViewModel = permissionViewModel,
                            statsViewModel = statsViewModel,
                            authViewModel = authViewModel,
                            onSyncClick = { performSync() }
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
        // [추가] 앱 실행 시(Foreground 진입 시) 자동 동기화 시도
        performSync(silent = true)
    }

    // [수정] 통합 동기화 로직 (삭제 동기화 로직 추가)
    private fun performSync(silent: Boolean = false) {
        val userId = SupabaseProvider.client.auth.currentUserOrNull()?.id ?: return

        lifecycleScope.launch {
            try {
                if (!silent) Toast.makeText(this@MainActivity, "동기화 중...", Toast.LENGTH_SHORT).show()

                // 1. 서버에서 데이터 가져오기 (Pull)
                val remoteStats = supabaseRepo.getDailyStats(userId)
                val remotePresets = supabaseRepo.getWorkPresets(userId)

                // 2. 로컬 DB에 병합

                // (1) 통계 병합 (기존 유지)
                if (remoteStats.isNotEmpty()) {
                    val currentStats = localRepo.loadDailyStats().toMutableMap()
                    remoteStats.forEach { stat ->
                        currentStats[stat.date] = stat
                    }
                    localRepo.saveDailyStats(currentStats)
                }

                // (2) 프리셋 동기화 [중요 수정]
                // 서버 목록을 기준으로 로컬 목록을 갱신합니다.
                // 서버에 없는 로컬 아이템은 '삭제된 것'으로 간주하고 제거합니다.
                // *주의: 오프라인에서 생성하고 아직 동기화 못한 데이터가 있다면 삭제될 수 있습니다.
                if (remotePresets.isNotEmpty()) {
                    val mergedPresets = remotePresets.toMutableList()

                    // ViewModel 및 로컬 DB 업데이트
                    // (SettingsViewModel의 uiState를 갱신하면, saveSettingsAndReset 등의 로직이 없어도
                    //  다음 로직이나 UI에서 반영됩니다. 하지만 DB 저장을 위해 명시적으로 호출 권장)

                    // 여기서는 ViewModel의 상태를 강제로 업데이트하는 방식을 사용합니다.
                    // (실제로는 LocalRepo에 저장하는 로직이 ViewModel 내부에 있으므로,
                    //  ViewModel에 setPresets 같은 함수를 만들어 호출하는 것이 가장 깔끔합니다.)
                    // 임시로 settingsViewModel 내부의 값을 갱신하는 로직을 수행한다고 가정하거나,
                    // 로컬 레포지토리에 직접 저장합니다.

                    localRepo.insertNewWorkPresets(mergedPresets) // 덮어쓰기 (OnConflictStrategy.REPLACE 가정)

                    // 서버에 없는 건 삭제 (현재 로컬에만 있는 ID 찾기)
                    val localPresets = localRepo.loadWorkPresets()
                    val remoteIds = remotePresets.map { it.id }.toSet()
                    val toDelete = localPresets.filter { it.id !in remoteIds }

                    toDelete.forEach {
                        localRepo.deleteWorkPreset(it.id)
                    }

                    // UI 갱신을 위해 ViewModel 데이터 다시 로드 요청 (또는 직접 업데이트)
                    settingsViewModel.refreshPresets() // *ViewModel에 이 함수 추가 필요 (하단 참조)
                }


                // 3. 로컬 데이터를 서버로 백업 (Push)
                // [수정] Pull 직후이므로, 로컬 데이터가 최신 서버 데이터와 일치해졌습니다.
                // 굳이 바로 다시 올릴 필요는 없으나, 병합 과정에서 누락된 게 있을 수 있으니 유지하거나,
                // '삭제 동기화'를 위해 Pull 위주로만 작동하게 할 수도 있습니다.
                // 여기서는 안전하게 '현재 유효한 목록'을 다시 서버에 확정 짓습니다.

                val currentPresets = localRepo.loadWorkPresets() // 갱신된 로컬 데이터 로드
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