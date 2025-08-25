package com.malrang.pomodoro.service

import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import kotlinx.coroutines.*

class AppUsageMonitoringService : Service() {

    private var job: Job? = null
    private lateinit var usageStatsManager: UsageStatsManager
    private val launcherPackageNames = mutableSetOf<String>()
    private var isOverlayShown = false
    private var isTemporaryPassActive = false

    private val tempPassReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.malrang.pomodoro.ACTION_TEMP_PASS") {
                isTemporaryPassActive = true
                isOverlayShown = false // 오버레이가 닫혔으므로 상태 동기화
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        fetchLauncherPackageNames()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(tempPassReceiver, IntentFilter("com.malrang.pomodoro.ACTION_TEMP_PASS"), RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(tempPassReceiver, IntentFilter("com.malrang.pomodoro.ACTION_TEMP_PASS"))
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val whitelistedApps = intent?.getStringArrayExtra("WHITELISTED_APPS")?.toSet() ?: emptySet()
        val ownPackageName = packageName

        job?.cancel()
        job = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                val foregroundApp = getForegroundApp()

                val isForbiddenApp = foregroundApp != null &&
                        foregroundApp != ownPackageName &&
                        !whitelistedApps.contains(foregroundApp) &&
                        !launcherPackageNames.contains(foregroundApp)

                // ✅ 로직 수정: 안전한 앱일 때와 금지된 앱일 때의 역할을 명확히 분리
                if (isForbiddenApp) {
                    // 금지된 앱을 사용 중인 경우
                    // 임시 허용 상태가 아니고, 오버레이가 아직 표시되지 않았다면 오버레이를 띄웁니다.
                    if (!isTemporaryPassActive && !isOverlayShown) {
                        startWarningOverlay()
                        isOverlayShown = true
                    }
                } else {
                    // 안전한 앱을 사용 중인 경우 (우리 앱, 홈 화면, 화이트리스트 앱 등)
                    // 다음을 위해 임시 허용 상태를 리셋합니다.
                    isTemporaryPassActive = false
                    // ✅ 중요: 여기서 더 이상 오버레이를 끄지 않습니다!
                }
                delay(1000)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
        unregisterReceiver(tempPassReceiver)
        stopWarningOverlay()
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

    private fun getForegroundApp(): String? {
        var foregroundApp: String? = null
        val time = System.currentTimeMillis()
        val usageEvents = usageStatsManager.queryEvents(time - 1000 * 2, time)
        val event = UsageEvents.Event()
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                foregroundApp = event.packageName
            }
        }
        return foregroundApp
    }

    private fun startWarningOverlay() {
        startService(Intent(this, WarningOverlayService::class.java))
    }

    private fun stopWarningOverlay() {
        stopService(Intent(this, WarningOverlayService::class.java))
    }

    override fun onBind(intent: Intent?): IBinder? = null
}