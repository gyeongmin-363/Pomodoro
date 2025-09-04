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
import com.malrang.pomodoro.dataclass.ui.BlockMode
import kotlinx.coroutines.*

class AppUsageMonitoringService : Service() {

    private var job: Job? = null
    private lateinit var usageStatsManager: UsageStatsManager
    private val launcherPackageNames = mutableSetOf<String>()
    private var isOverlayShown = false
    private var isTemporaryPassActive = false
    private var currentBlockMode: BlockMode = BlockMode.PARTIAL // 기본값 설정

    // '앱 계속 사용' 버튼 클릭 시 신호를 받는 리시버
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
        // 브로드캐스트 리시버 등록
        registerReceiver(tempPassReceiver, IntentFilter("com.malrang.pomodoro.ACTION_TEMP_PASS"), RECEIVER_NOT_EXPORTED)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val whitelistedApps = intent?.getStringArrayExtra("WHITELISTED_APPS")?.toSet() ?: emptySet()
        val ownPackageName = packageName

        // MainActivity로부터 현재 설정된 차단 모드를 받아옴
        val blockModeString = intent?.getStringExtra("BLOCK_MODE")
        currentBlockMode = runCatching {
            BlockMode.valueOf(blockModeString ?: "")
        }.getOrElse { BlockMode.PARTIAL }


        job?.cancel()
        job = CoroutineScope(Dispatchers.IO).launch {
            // 차단 모드가 '없음'이면 감시할 필요가 없으므로 코루틴 종료
            if (currentBlockMode == BlockMode.NONE) {
                stopSelf() // 서비스 자체를 종료
                return@launch
            }

            while (isActive) {
                val foregroundApp = getForegroundApp()

                val isForbiddenApp = foregroundApp != null &&
                        foregroundApp != ownPackageName &&
                        !whitelistedApps.contains(foregroundApp) &&
                        !launcherPackageNames.contains(foregroundApp)

                if (isForbiddenApp) {
                    // 금지된 앱 사용 중 + 임시 허용 상태 아님 + 오버레이 아직 안 뜸
                    if (!isTemporaryPassActive && !isOverlayShown) {
                        startWarningOverlay()
                        isOverlayShown = true
                    }
                } else {
                    // 안전한 앱 사용 중 (뽀모도로, 화이트리스트, 홈 화면 등)
                    // 다음을 위해 임시 허용 상태를 초기화
                    isTemporaryPassActive = false
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
            setPackage("com.malrang.pomodoro")

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
        // WarningOverlayService에 현재 차단 모드를 전달
        val intent = Intent(this, WarningOverlayService::class.java).apply {
            putExtra("BLOCK_MODE", currentBlockMode.name)
            setPackage("com.malrang.pomodoro")

        }
        startService(intent)
    }

    private fun stopWarningOverlay() {
        stopService(Intent(this, WarningOverlayService::class.java))
    }

    override fun onBind(intent: Intent?): IBinder? = null
}