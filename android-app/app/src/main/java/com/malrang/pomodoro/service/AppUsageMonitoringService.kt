package com.malrang.pomodoro.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.malrang.pomodoro.dataclass.ui.BlockMode
import com.malrang.pomodoro.localRepo.PomodoroRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AppUsageMonitoringService : AccessibilityService() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.IO)
    private lateinit var repo: PomodoroRepository

    private var currentBlockMode: BlockMode = BlockMode.NONE
    private var whitelistedApps: Set<String> = emptySet()
    private val launcherPackageNames = mutableSetOf<String>()

    private var isOverlayShown = false
    private var isTemporaryPassActive = false

    private val tempPassReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.malrang.pomodoro.ACTION_TEMP_PASS") {
                isTemporaryPassActive = true
                // 버튼을 눌러서 닫혔으므로 상태 동기화
                isOverlayShown = false
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("POMODORO_SVC", "접근성 서비스 연결됨")

        repo = PomodoroRepository(this)
        fetchLauncherPackageNames()

        registerReceiver(tempPassReceiver, IntentFilter("com.malrang.pomodoro.ACTION_TEMP_PASS"), Context.RECEIVER_NOT_EXPORTED)


        serviceScope.launch {
            repo.activeBlockModeFlow.collectLatest { mode ->
                currentBlockMode = mode
                if (mode == BlockMode.NONE) {
                    isTemporaryPassActive = false
                    stopWarningOverlay()
                }
            }
        }

        serviceScope.launch {
            repo.whitelistedAppsFlow.collectLatest { apps ->
                whitelistedApps = apps
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (currentBlockMode == BlockMode.NONE) return

        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString()
            if (packageName != null) {
                checkAndBlockApp(packageName)
            }
        }
    }

    private fun checkAndBlockApp(foregroundApp: String) {
        val ownPackageName = packageName

        val isForbiddenApp = foregroundApp != ownPackageName &&
                !whitelistedApps.contains(foregroundApp) &&
                !launcherPackageNames.contains(foregroundApp)

        if (isForbiddenApp) {
            // 차단해야 할 앱 감지됨
            if (!isTemporaryPassActive && !isOverlayShown) {
                Log.d("POMODORO_SVC", "차단 실행: $foregroundApp")
                startWarningOverlay()
                isOverlayShown = true
            }
        } else {
            // 안전한 앱 감지됨 (홈 화면, 화이트리스트 앱, OR **우리 앱**)

            // ✅ [버그 수정 핵심]
            // 감지된 앱이 '우리 앱(ownPackageName)'인 경우, 오버레이가 떠서 감지된 것일 수 있습니다.
            // 이 경우 stopWarningOverlay()를 호출하면 오버레이가 뜨자마자 닫히게 되므로,
            // 우리 앱일 때는 오버레이 종료 로직을 건너뜁니다.
            if (foregroundApp == ownPackageName) {
                return
            }

            // 그 외(홈 화면이나 다른 허용 앱)로 나갔을 때만 상태를 리셋하고 오버레이를 닫습니다.
            isTemporaryPassActive = false

            if (isOverlayShown) {
                isOverlayShown = false
                stopWarningOverlay()
            }
        }
    }

    private fun startWarningOverlay() {
        val intent = Intent(this, WarningOverlayService::class.java).apply {
            putExtra("BLOCK_MODE", currentBlockMode.name)
            setPackage("com.malrang.pomodoro")
        }
        startService(intent)
    }

    private fun stopWarningOverlay() {
        stopService(Intent(this, WarningOverlayService::class.java))
        isOverlayShown = false
    }

    private fun fetchLauncherPackageNames() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfoList = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        for (resolveInfo in resolveInfoList) {
            launcherPackageNames.add(resolveInfo.activityInfo.packageName)
        }
    }

    override fun onInterrupt() {
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        try {
            unregisterReceiver(tempPassReceiver)
        } catch (e: Exception) {
            Log.w("POMODORO_SVC", "tempPassReceiver 이미 해제되었거나 등록되지 않음", e)
        }
    }
}