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
    private var blockedApps: Set<String> = emptySet()
    private val launcherPackageNames = mutableSetOf<String>()

    private var isOverlayShown = false
    private var isTemporaryPassActive = false

    private val tempPassReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.malrang.pomodoro.ACTION_TEMP_PASS") {
                isTemporaryPassActive = true
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

        // [변경] 차단 목록 수집
        serviceScope.launch {
            repo.blockedAppsFlow.collectLatest { apps ->
                blockedApps = apps
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

        // [변경] 1. 런처(홈 화면)이거나 우리 앱이면 절대 차단하지 않음 (안전 장치)
        if (foregroundApp == ownPackageName || launcherPackageNames.contains(foregroundApp)) {
            return
        }

        // [변경] 2. 차단 목록에 포함되어 있는지 확인
        val isForbiddenApp = blockedApps.contains(foregroundApp)

        if (isForbiddenApp) {
            // 차단해야 할 앱 감지됨
            if (!isTemporaryPassActive && !isOverlayShown) {
                Log.d("POMODORO_SVC", "차단 실행: $foregroundApp")
                startWarningOverlay()
                isOverlayShown = true
            }
        } else {
            // 안전한 앱 (차단 목록에 없음)
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