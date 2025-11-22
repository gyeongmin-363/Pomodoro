package com.malrang.pomodoro.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.malrang.pomodoro.MainActivity
import com.malrang.pomodoro.R
import com.malrang.pomodoro.dataclass.ui.BlockMode
import com.malrang.pomodoro.dataclass.ui.DailyStat
import com.malrang.pomodoro.dataclass.ui.Mode
import com.malrang.pomodoro.dataclass.ui.Settings
import com.malrang.pomodoro.localRepo.PomodoroRepository
import com.malrang.pomodoro.localRepo.SoundPlayer
import com.malrang.pomodoro.localRepo.VibratorHelper
import com.malrang.pomodoro.networkRepo.SupabaseProvider
import com.malrang.pomodoro.networkRepo.SupabaseRepository
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.ceil

class TimerService : Service() {

    private var job: Job? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var timeLeft: Int = 0
    private var isRunning: Boolean = false

    private var settings: Settings? = null
    private var currentMode: Mode = Mode.STUDY
    private var totalSessions: Int = 0
    private lateinit var soundPlayer: SoundPlayer
    private lateinit var vibratorHelper: VibratorHelper
    private lateinit var repo: PomodoroRepository
    private lateinit var supabaseRepo: SupabaseRepository
    private lateinit var wakeLock: PowerManager.WakeLock


    override fun onCreate() {
        super.onCreate()
        isServiceActive = true
        createNotificationChannel()
        soundPlayer = SoundPlayer(this)
        vibratorHelper = VibratorHelper(this)
        repo = PomodoroRepository(this)
        supabaseRepo = SupabaseRepository(SupabaseProvider.client.postgrest, SupabaseProvider.client.storage)

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Pomodoro::TimerWakeLock")
    }

    // 세션 전환 로직을 별도 함수로 분리하여 재사용성 및 일관성 확보
    private fun advanceToNextSession() {
        val currentSettings = settings ?: return

        if (currentMode == Mode.STUDY) {
            totalSessions++
            val isLongBreakTime = totalSessions > 0 && totalSessions % currentSettings.longBreakInterval == 0
            currentMode = if (isLongBreakTime) Mode.LONG_BREAK else Mode.SHORT_BREAK
            timeLeft = (if (isLongBreakTime) currentSettings.longBreakTime else currentSettings.shortBreakTime) * 60
        } else { // 현재 모드가 휴식 시간이면 다음은 공부 시간
            currentMode = Mode.STUDY
            timeLeft = currentSettings.studyTime * 60
        }
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val safeIntent = intent ?: return START_NOT_STICKY

        when (intent.action) {
            "START" -> {
                if (!isRunning) {
                    settings = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        safeIntent.getSerializableExtra(EXTRA_SETTINGS, Settings::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        safeIntent.getSerializableExtra(EXTRA_SETTINGS) as? Settings
                    }

                    settings?.let { s ->
                        // ViewModel이 전달한 상태를 우선적으로 사용
                        currentMode = safeIntent.getStringExtra(EXTRA_CURRENT_MODE)?.let { Mode.valueOf(it) } ?: Mode.STUDY
                        totalSessions = safeIntent.getIntExtra(EXTRA_TOTAL_SESSIONS, 0)
                        timeLeft = safeIntent.getIntExtra(EXTRA_TIME_LEFT, 0)

                        // 타이머 시작 시 남은 시간이 0이면, 현재 모드에 맞는 시간으로 재설정
                        if (timeLeft <= 0) {
                            timeLeft = when (currentMode) {
                                Mode.STUDY -> s.studyTime * 60
                                Mode.SHORT_BREAK -> s.shortBreakTime * 60
                                Mode.LONG_BREAK -> s.longBreakTime * 60
                            }
                        }
                        startTimer()
                    }
                }
            }
            // RESUME: 설정 변경 없이 현재 상태 그대로 재개 (알림창 Action용)
            "RESUME" -> {
                if (!isRunning && timeLeft > 0) {
                    startTimer()
                }
            }
            "PAUSE" -> {
                pauseTimer()
            }
            "REQUEST_STATUS" -> {
                broadcastStatus()
            }
            "SKIP" -> {
                job?.cancel()
                isRunning = false
                if (wakeLock.isHeld) { wakeLock.release() }

                advanceToNextSession() //  통합된 세션 전환 로직 호출

                // 스킵 후에는 항상 '일시정지' 상태이므로, 변경된 상태를 즉시 저장하고 UI에 알립니다.
                serviceScope.launch {
                    repo.saveTimerState(timeLeft, currentMode, totalSessions)
                }
                updateNotification()
                broadcastStatus()
            }
            "RESET" -> {
                job?.cancel()
                isRunning = false
                if (wakeLock.isHeld) { wakeLock.release() }
                serviceScope.launch { repo.clearTimerState() }

                val newSettings = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    safeIntent.getSerializableExtra(EXTRA_SETTINGS, Settings::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    safeIntent.getSerializableExtra(EXTRA_SETTINGS) as? Settings
                }
                if (newSettings != null) { settings = newSettings }

                currentMode = Mode.STUDY
                totalSessions = 0
                timeLeft = settings?.studyTime?.times(60) ?: (25 * 60)

                broadcastStatus()
                updateNotification()
            }
            "STOP_SERVICE_ACTION" -> {
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun broadcastStatus() {
        val intent = Intent(ACTION_STATUS_UPDATE).apply {
            putExtra(EXTRA_TIME_LEFT, timeLeft)
            putExtra(EXTRA_IS_RUNNING, isRunning)
            putExtra(EXTRA_CURRENT_MODE, currentMode.name)
            putExtra(EXTRA_TOTAL_SESSIONS, totalSessions)
            setPackage("com.malrang.pomodoro")
        }
        sendBroadcast(intent)
    }

    private fun startTimer() {
        isRunning = true

        job?.cancel()
        wakeLock.acquire(90*60*1000L /*90 minutes*/)
        job = CoroutineScope(Dispatchers.Main).launch {
            settings?.let {
                repo.saveActiveBlockMode(it.blockMode)
            }

            // [수정] Drift 방지를 위한 종료 예정 시간 계산
            // 현재 시간 + 남은 초 * 1000ms
            val endTime = System.currentTimeMillis() + (timeLeft * 1000L)

            // isRunning을 루프 조건에 포함하여 안전하게 제어
            while (timeLeft > 0 && isRunning) {
                // 정확도를 위해 0.5초마다 체크 (UI 부하는 아래 로직으로 방지)
                delay(500)

                // 종료 시간까지 남은 실제 밀리초 계산
                val remainingMillis = endTime - System.currentTimeMillis()

                // 남은 시간을 초 단위로 변환 (올림 처리하여 24.9초 -> 25초 표시)
                // 0 이하가 되면 루프 종료
                val newTimeLeft = if (remainingMillis > 0) {
                    ceil(remainingMillis / 1000.0).toInt()
                } else {
                    0
                }

                // 초 단위 숫자가 변경되었을 때만 UI 업데이트 및 브로드캐스트
                if (newTimeLeft != timeLeft) {
                    timeLeft = newTimeLeft
                    updateNotification()
                    broadcastStatus()
                }
            }

            if (wakeLock.isHeld) { wakeLock.release() }

            // 정상 종료된 경우 (timeLeft == 0)에만 세션 완료 처리
            if (timeLeft <= 0) {
                val finishedMode = currentMode
                val currentSettings = settings ?: return@launch

                if (currentSettings.soundEnabled) { soundPlayer.playSound() }
                if (currentSettings.vibrationEnabled) { vibratorHelper.vibrate() }

                handleSessionCompletion(finishedMode)

                advanceToNextSession() // 통합된 세션 전환 로직 호출

                // 다음 동작(자동시작/일시정지) 전에 UI에 변경된 상태를 먼저 알립니다.
                broadcastStatus()
                updateNotification()

                if (currentSettings.autoStart) {
                    startTimer()
                } else {
                    pauseTimer()
                }
            }
        }
        startForeground(NOTIFICATION_ID, createNotification())
    }

    private fun pauseTimer() {
        isRunning = false
        job?.cancel()
        if (wakeLock.isHeld) { wakeLock.release() }
        // 일시정지 할 때마다 현재 상태를 저장합니다.
        serviceScope.launch {
            repo.saveTimerState(timeLeft, currentMode, totalSessions)
        }
        // ✅ 일시정지 시 차단 해제
        serviceScope.launch {
            repo.saveTimerState(timeLeft, currentMode, totalSessions)
            repo.saveActiveBlockMode(BlockMode.NONE)
        }
        updateNotification()
        broadcastStatus()
    }

    private fun handleSessionCompletion(finishedMode: Mode) {
        serviceScope.launch {
            updateTodayStats(finishedMode)

            // 데이터 업데이트 신호 보내기
            val intent = Intent(ACTION_DATA_UPDATED).apply {
                setPackage("com.malrang.pomodoro")
            }
            sendBroadcast(intent)
        }
    }

    private suspend fun updateTodayStats(finishedMode: Mode) {
        val currentSettings = settings ?: return

        val today = LocalDate.now().toString()
        val currentStatsMap = repo.loadDailyStats().toMutableMap()
        val todayStat = currentStatsMap[today] ?: DailyStat(today)

        val currentWorkId = repo.loadCurrentWorkId()
        val workPresets = repo.loadWorkPresets()
        val currentWorkName = workPresets.find { it.id == currentWorkId }?.name ?: "알 수 없는 Work"

        val updatedStat = when (finishedMode) {
            Mode.STUDY -> {
                val newStudyTimeMap = (todayStat.studyTimeByWork ?: emptyMap()).toMutableMap()
                val currentWorkTime = newStudyTimeMap.getOrDefault(currentWorkName, 0)
                newStudyTimeMap[currentWorkName] = currentWorkTime + currentSettings.studyTime
                todayStat.copy(studyTimeByWork = newStudyTimeMap)
            }
            Mode.SHORT_BREAK, Mode.LONG_BREAK -> {
                val breakTime = if(finishedMode == Mode.SHORT_BREAK) currentSettings.shortBreakTime else currentSettings.longBreakTime
                val newBreakTimeMap = (todayStat.breakTimeByWork ?: emptyMap()).toMutableMap()
                val currentWorkTime = newBreakTimeMap.getOrDefault(currentWorkName, 0)
                newBreakTimeMap[currentWorkName] = currentWorkTime + breakTime
                todayStat.copy(breakTimeByWork = newBreakTimeMap)
            }
        }
        currentStatsMap[today] = updatedStat
        repo.saveDailyStats(currentStatsMap)
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel("pomodoro_timer", "Pomodoro Timer", NotificationManager.IMPORTANCE_LOW)
        channel.setShowBadge(false)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val stopServiceIntent = Intent(this, TimerService::class.java).apply {
            action = "STOP_SERVICE_ACTION"
        }
        val stopServicePendingIntent = PendingIntent.getService(
            this, 0, stopServiceIntent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action Intents 생성
        val pauseIntent = Intent(this, TimerService::class.java).apply { action = "PAUSE" }
        val pausePendingIntent = PendingIntent.getService(
            this, 1, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val resumeIntent = Intent(this, TimerService::class.java).apply { action = "RESUME" }
        val resumePendingIntent = PendingIntent.getService(
            this, 2, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val modeText = when (currentMode) {
            Mode.STUDY -> "운행 중"
            Mode.SHORT_BREAK -> "짧은 정차 중"
            Mode.LONG_BREAK -> "긴 정차 중"
        }

        val statusText = if (isRunning) {
            val minutes = timeLeft / 60
            val seconds = timeLeft % 60
            String.format("남은 시간: %02d:%02d", minutes, seconds)
        } else {
            if (timeLeft > 0) {
                val minutes = timeLeft / 60
                val seconds = timeLeft % 60
                String.format("남은 시간: %02d:%02d (일시정지)", minutes, seconds)
            } else "시간 종료"
        }

        val sessionText = if (currentMode == Mode.STUDY) " | 세션: ${totalSessions + 1}" else ""
        val contentText = "$statusText$sessionText"

        val builder = NotificationCompat.Builder(this, "pomodoro_timer")
            .setContentTitle("Focus Route: $modeText")
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(isRunning)
            .setDeleteIntent(stopServicePendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // 잠금 화면에서도 컨트롤 보이게 설정

        // [건너뛰기 제거됨] 상태에 따른 알림 Action 버튼
        if (isRunning) {
            // 실행 중일 때: [일시정지]만 표시
            builder.addAction(R.drawable.ic_pause, "일시정지", pausePendingIntent)
        } else {
            // 일시정지 중이고 시간이 남았을 때: [계속]만 표시
            if (timeLeft > 0) {
                builder.addAction(R.drawable.ic_play, "계속", resumePendingIntent)
            }
        }

        return builder.build()
    }


    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.launch {
            repo.saveActiveBlockMode(BlockMode.NONE)
        }
        job?.cancel()
        isRunning = false
        isServiceActive = false
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 2022
        const val ACTION_STATUS_UPDATE = "com.malrang.pomodoro.ACTION_STATUS_UPDATE"
        const val EXTRA_IS_RUNNING = "com.malrang.pomodoro.EXTRA_IS_RUNNING"
        const val EXTRA_TIME_LEFT = "com.malrang.pomodoro.EXTRA_TIME_LEFT"
        const val EXTRA_CURRENT_MODE = "com.malrang.pomodoro.EXTRA_CURRENT_MODE"
        const val EXTRA_TOTAL_SESSIONS = "com.malrang.pomodoro.EXTRA_TOTAL_SESSIONS"
        const val EXTRA_SETTINGS = "com.malrang.pomodoro.EXTRA_SETTINGS"

        //데이터 업데이트 후 신호
        const val ACTION_DATA_UPDATED = "com.malrang.pomodoro.ACTION_DATA_UPDATED"

        private var isServiceActive = false
        fun isServiceActive(): Boolean = isServiceActive
    }
}