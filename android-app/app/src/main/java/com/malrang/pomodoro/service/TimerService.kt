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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

class TimerService : Service() {

    private var job: Job? = null
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START" -> {
                if (!isRunning) {
                    // 설정 값을 인텐트에서 가져옵니다.
                    settings = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getSerializableExtra(EXTRA_SETTINGS, Settings::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getSerializableExtra(EXTRA_SETTINGS) as? Settings
                    }

                    settings?.let { s ->
                        // 타이머 상태를 설정하고 시작하는 로직을 코루틴으로 감쌉니다.
                        CoroutineScope(Dispatchers.IO).launch {
                            val intentTimeLeft = intent.getIntExtra(EXTRA_TIME_LEFT, 0)
                            val savedState = repo.loadTimerState() // 저장된 상태를 불러옵니다.

                            if (intentTimeLeft > 0) {
                                // 인텐트로 전달된 시간이 있으면 그 값을 사용합니다.
                                timeLeft = intentTimeLeft
                                currentMode = intent.getStringExtra(EXTRA_CURRENT_MODE)?.let { Mode.valueOf(it) } ?: Mode.STUDY
                                totalSessions = intent.getIntExtra(EXTRA_TOTAL_SESSIONS, 0)
                            } else if (savedState != null) {
                                // 전달된 시간이 없고 저장된 상태가 있으면 복원합니다.
                                timeLeft = savedState.timeLeft
                                currentMode = savedState.currentMode
                                totalSessions = savedState.totalSessions
                            } else {
                                // 전달된 시간도, 저장된 상태도 없으면 초기화합니다.
                                timeLeft = when (currentMode) {
                                    Mode.STUDY -> s.studyTime * 60
                                    Mode.SHORT_BREAK -> s.shortBreakTime * 60
                                    Mode.LONG_BREAK -> s.longBreakTime * 60
                                }
                                totalSessions = 0 // 세션도 초기화
                            }

                            // 타이머 시작은 메인 스레드에서 수행합니다.
                            launch(Dispatchers.Main) {
                                startTimer()
                            }
                        }
                    }
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
                if (wakeLock.isHeld) {
                    wakeLock.release()
                }
                val currentSettings = settings ?: return START_STICKY

                var nextMode = Mode.STUDY
                var nextTime = 0
                var newTotalSessions = totalSessions

                if (currentMode == Mode.STUDY) {
                    newTotalSessions++
                    val isLongBreakTime = newTotalSessions > 0 &&
                            newTotalSessions % currentSettings.longBreakInterval == 0
                    nextMode = if (isLongBreakTime) Mode.LONG_BREAK else Mode.SHORT_BREAK
                    nextTime = if (isLongBreakTime) currentSettings.longBreakTime else currentSettings.shortBreakTime
                } else {
                    nextMode = Mode.STUDY
                    nextTime = currentSettings.studyTime
                }

                currentMode = nextMode
                totalSessions = newTotalSessions
                timeLeft = nextTime * 60
                isRunning = false

                updateNotification()
                broadcastStatus()
            }
            "RESET" -> {
                job?.cancel()
                isRunning = false
                if (wakeLock.isHeld) {
                    wakeLock.release()
                }
                CoroutineScope(Dispatchers.IO).launch {
                    repo.clearTimerState()
                }
                val newSettings = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getSerializableExtra(EXTRA_SETTINGS, Settings::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getSerializableExtra(EXTRA_SETTINGS) as? Settings
                }

                if (newSettings != null) {
                    settings = newSettings
                }

                currentMode = Mode.STUDY
                totalSessions = 0
                timeLeft = settings?.studyTime?.times(60) ?: (25 * 60)

                broadcastStatus()
                updateNotification()
            }
            // [추가] 알림이 지워졌을 때 호출될 액션
            "STOP_SERVICE_ACTION" -> {
                stopSelf() // 서비스 종료
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
            while (timeLeft > 0) {
                delay(1000)
                timeLeft--
                updateNotification()
                broadcastStatus()
            }
            if (wakeLock.isHeld) {
                wakeLock.release()
            }

            val finishedMode = currentMode
            val currentSettings = settings ?: return@launch

            if (currentSettings.soundEnabled) { soundPlayer.playSound() }
            if (currentSettings.vibrationEnabled) { vibratorHelper.vibrate() }

            handleSessionCompletion(finishedMode)

            var nextMode = Mode.STUDY
            var nextTime = 0
            var newTotalSessions = totalSessions

            if (finishedMode == Mode.STUDY) {
                newTotalSessions++
                val isLongBreakTime = newTotalSessions > 0 && newTotalSessions % currentSettings.longBreakInterval == 0
                nextMode = if (isLongBreakTime) Mode.LONG_BREAK else Mode.SHORT_BREAK
                nextTime = if (isLongBreakTime) currentSettings.longBreakTime else currentSettings.shortBreakTime
            } else {
                nextMode = Mode.STUDY
                nextTime = currentSettings.studyTime
            }

            timeLeft = nextTime * 60
            currentMode = nextMode
            totalSessions = newTotalSessions

            if (currentSettings.autoStart) {
                startTimer()
            } else {
                pauseTimer()
            }
        }
        startForeground(NOTIFICATION_ID, createNotification())
    }

    private fun pauseTimer() {
        isRunning = false
        job?.cancel()
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
        CoroutineScope(Dispatchers.IO).launch {
            repo.saveTimerState(timeLeft, currentMode, totalSessions)
        }
        updateNotification()
        broadcastStatus()
    }

    private fun handleSessionCompletion(finishedMode: Mode) {
        CoroutineScope(Dispatchers.IO).launch {
            updateTodayStats(finishedMode)

            // 공부 세션이 끝났을 때만 코인을 지급합니다.
            if (finishedMode == Mode.STUDY) {
                // 현재 로그인된 사용자 ID 가져오기
                val userId = SupabaseProvider.client.auth.currentUserOrNull()?.id
                // 설정된 공부 시간을 보상으로 지급할 코인 양으로 사용
                val coinAmount = settings?.studyTime

                // 사용자 ID와 코인 양이 유효할 때만 지급
                if (userId != null && coinAmount != null && coinAmount > 0) {
                    supabaseRepo.incrementUserCoins(userId, coinAmount)
                }
            }
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

        // [추가] 알림이 지워졌을 때 서비스를 종료시키기 위한 PendingIntent 생성
        val stopServiceIntent = Intent(this, TimerService::class.java).apply {
            action = "STOP_SERVICE_ACTION"
        }
        val stopServicePendingIntent = PendingIntent.getService(
            this, 0, stopServiceIntent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )


        val modeText = when (currentMode) {
            Mode.STUDY -> "공부 시간"
            Mode.SHORT_BREAK -> "짧은 휴식"
            Mode.LONG_BREAK -> "긴 휴식"
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

        return NotificationCompat.Builder(this, "pomodoro_timer")
            .setContentTitle("뽀모도로 타이머: $modeText")
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(isRunning) // isRunning이 true일 때 (타이머 작동 중) 알림을 못 지우게 설정
            .setDeleteIntent(stopServicePendingIntent) // [추가] 알림을 지웠을 때 stopServicePendingIntent 실행
            .build()
    }


    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
        isRunning = false
        isServiceActive = false
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 2022
        // 서비스 상태 브로드캐스트용 상수
        const val ACTION_STATUS_UPDATE = "com.malrang.pomodoro.ACTION_STATUS_UPDATE"
        const val EXTRA_IS_RUNNING = "com.malrang.pomodoro.EXTRA_IS_RUNNING"

        // 데이터 전달용 상수
        const val EXTRA_TIME_LEFT = "com.malrang.pomodoro.EXTRA_TIME_LEFT"
        const val EXTRA_CURRENT_MODE = "com.malrang.pomodoro.EXTRA_CURRENT_MODE"
        const val EXTRA_TOTAL_SESSIONS = "com.malrang.pomodoro.EXTRA_TOTAL_SESSIONS"
        const val EXTRA_SETTINGS = "com.malrang.pomodoro.EXTRA_SETTINGS"

        private var isServiceActive = false
        fun isServiceActive(): Boolean = isServiceActive
    }

}