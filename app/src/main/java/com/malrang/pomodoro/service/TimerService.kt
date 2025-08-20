package com.malrang.pomodoro.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.malrang.pomodoro.MainActivity
import com.malrang.pomodoro.R
import com.malrang.pomodoro.dataclass.ui.Mode
import com.malrang.pomodoro.dataclass.ui.Settings
import com.malrang.pomodoro.localRepo.SoundPlayer
import com.malrang.pomodoro.localRepo.VibratorHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TimerService : Service() {

    private var job: Job? = null
    private var timeLeft: Int = 0
    private var isRunning: Boolean = false

    // --- 추가: 서비스가 직접 상태를 관리하기 위한 변수 ---
    private var settings: Settings? = null
    private var currentMode: Mode = Mode.STUDY
    private var totalSessions: Int = 0
    private lateinit var soundPlayer: SoundPlayer
    private lateinit var vibratorHelper: VibratorHelper


    override fun onCreate() {
        super.onCreate()
        isServiceActive = true
        createNotificationChannel()
        // --- 추가: 서비스 내에서 SoundPlayer와 VibratorHelper 인스턴스 생성 ---
        soundPlayer = SoundPlayer(this)
        vibratorHelper = VibratorHelper(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START" -> {
                if (!isRunning) {
                    timeLeft = intent.getIntExtra("TIME_LEFT", 0)
                    // --- 추가: Intent로부터 전달받은 상태 정보 저장 ---
                    settings = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getSerializableExtra("SETTINGS", Settings::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getSerializableExtra("SETTINGS") as? Settings
                    }
                    currentMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getSerializableExtra("CURRENT_MODE", Mode::class.java) ?: Mode.STUDY
                    } else {
                        @Suppress("DEPRECATION")
                        (intent.getSerializableExtra("CURRENT_MODE") as? Mode) ?: Mode.STUDY
                    }
                    totalSessions = intent.getIntExtra("TOTAL_SESSIONS", 0)

                    startTimer()
                    isRunning = true
                }
            }
            "PAUSE" -> {
                pauseTimer()
                isRunning = false
            }
            "RESET" -> {
                resetTimer()
                isRunning = false
                timeLeft = intent.getIntExtra("TIME_LEFT", 0)
                updateNotification()
                sendBroadcast(Intent(TIMER_TICK).apply {
                    putExtra("TIME_LEFT", timeLeft)
                    putExtra("IS_RUNNING", isRunning)
                })
            }
            "REQUEST_STATUS" -> {
                sendBroadcast(Intent(TIMER_TICK).apply {
                    putExtra("TIME_LEFT", timeLeft)
                    putExtra("IS_RUNNING", isRunning)
                })
            }
        }
        return START_STICKY
    }

    /**
     * 타이머 시작 및 세션 완료 로직 전체를 담당하도록 수정
     */
    private fun startTimer() {
        job?.cancel() // 기존 코루틴이 있다면 취소
        job = CoroutineScope(Dispatchers.Main).launch {
            while (timeLeft > 0) {
                delay(1000)
                timeLeft--
                updateNotification()
                sendBroadcast(Intent(TIMER_TICK).apply {
                    putExtra("TIME_LEFT", timeLeft)
                    putExtra("IS_RUNNING", true)
                })
            }

            // --- 여기부터 세션 완료 처리 로직 ---

            val currentSettings = settings ?: return@launch // 설정값이 없으면 중단

            // 1. 소리 및 진동 재생
            if (currentSettings.soundEnabled) {
                soundPlayer.playSound()
            }
            if (currentSettings.vibrationEnabled) {
                vibratorHelper.vibrate()
            }

            // 2. ViewModel에 세션 완료 알림 (UI 업데이트 및 동물 획득 처리용)
            sendBroadcast(Intent(TIMER_FINISHED))

            // 3. 다음 세션 상태 계산
            var nextMode = Mode.STUDY
            var nextTime = 0
            var newTotalSessions = totalSessions

            if (currentMode == Mode.STUDY) {
                newTotalSessions++
                val isLongBreakTime = newTotalSessions > 0 && newTotalSessions % currentSettings.longBreakInterval == 0
                nextMode = if (isLongBreakTime) Mode.LONG_BREAK else Mode.SHORT_BREAK
                nextTime = if (isLongBreakTime) currentSettings.longBreakTime else currentSettings.shortBreakTime
            } else { // 휴식 모드였을 경우
                nextMode = Mode.STUDY
                nextTime = currentSettings.studyTime
            }

            // 4. 자동 시작 설정 확인
            if (currentSettings.autoStart) {
                // 서비스의 상태를 다음 세션으로 업데이트
                timeLeft = nextTime * 60
                currentMode = nextMode
                totalSessions = newTotalSessions
                isRunning = true

                // 새로운 타이머 시작 (재귀 호출 대신 새로운 코루틴 시작)
                startTimer()
            } else {
                // 자동 시작이 아닐 경우 서비스 종료
                isRunning = false
                stopSelf()
            }
        }
        startForeground(NOTIFICATION_ID, createNotification())
    }

    private fun pauseTimer() {
        job?.cancel()
        updateNotification()
        sendBroadcast(Intent(TIMER_TICK).apply {
            putExtra("TIME_LEFT", timeLeft)
            putExtra("IS_RUNNING", false)
        })
    }

    private fun resetTimer() {
        job?.cancel()
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "pomodoro_timer",
            "Pomodoro Timer",
            NotificationManager.IMPORTANCE_LOW
        )
        channel.setShowBadge(false)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val minutes = timeLeft / 60
        val seconds = timeLeft % 60
        val timeFormatted = String.format("%02d:%02d", minutes, seconds)

        return NotificationCompat.Builder(this, "pomodoro_timer")
            .setContentTitle("뽀모도로 타이머")
            .setContentText("남은 시간: $timeFormatted")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(isRunning)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
        isRunning = false
        isServiceActive = false
    }

    companion object {
        private const val NOTIFICATION_ID = 2022
        const val TIMER_TICK = "com.malrang.pomodoro.TIMER_TICK"
        const val TIMER_FINISHED = "com.malrang.pomodoro.TIMER_FINISHED"
        private var isServiceActive = false
        fun isServiceActive(): Boolean = isServiceActive
    }
}