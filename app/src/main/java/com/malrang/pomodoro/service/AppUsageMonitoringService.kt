package com.malrang.pomodoro.service

import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.malrang.pomodoro.MainActivity
import kotlinx.coroutines.*

class AppUsageMonitoringService : Service() {

    private var job: Job? = null
    private lateinit var usageStatsManager: UsageStatsManager
    private var whitelistedApps: Set<String> = emptySet()
    private lateinit var ownPackageName: String
    private var isOverlayShown = false

    override fun onCreate() {
        super.onCreate()
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        ownPackageName = packageName
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 서비스가 중지될 때 오버레이도 함께 제거되도록 함
        if (intent?.action == "STOP") {
            stopWarningOverlay()
            stopSelf()
            return START_NOT_STICKY
        }

        whitelistedApps = intent?.getStringArrayExtra("WHITELISTED_APPS")?.toSet() ?: emptySet()
        job?.cancel()
        job = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                val foregroundApp = getForegroundApp()
                if (foregroundApp != null && foregroundApp != ownPackageName && !whitelistedApps.contains(foregroundApp)) {
                    // 오버레이가 아직 표시되지 않았다면 표시
                    if (!isOverlayShown) {
                        startWarningOverlay()
                        isOverlayShown = true
                    }
                } else {
                    // 우리 앱으로 돌아오면 오버레이 제거
                    if (isOverlayShown) {
                        stopWarningOverlay()
                        isOverlayShown = false
                    }
                }
                delay(1000) // 1초마다 확인
            }
        }
        return START_STICKY
    }

    private fun getForegroundApp(): String? {
        val time = System.currentTimeMillis()
        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, time - 1000 * 5, time
        )
        return usageStats?.filter { it.lastTimeUsed > 0 }
            ?.maxByOrNull { it.lastTimeUsed }?.packageName
    }

    // ✅ WarningOverlayService를 시작하는 함수
    private fun startWarningOverlay() {
        startService(Intent(this, WarningOverlayService::class.java))
    }

    // ✅ WarningOverlayService를 중지하는 함수
    private fun stopWarningOverlay() {
        stopService(Intent(this, WarningOverlayService::class.java))
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
        // 서비스가 완전히 종료될 때 오버레이가 남아있지 않도록 확실히 제거
        stopWarningOverlay()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}