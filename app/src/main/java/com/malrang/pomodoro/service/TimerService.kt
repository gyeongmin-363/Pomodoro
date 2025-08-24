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
import com.malrang.pomodoro.localRepo.PomodoroRepository
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

    private var settings: Settings? = null
    private var currentMode: Mode = Mode.STUDY
    private var totalSessions: Int = 0
    private lateinit var soundPlayer: SoundPlayer
    private lateinit var vibratorHelper: VibratorHelper


    override fun onCreate() {
        super.onCreate()
        isServiceActive = true
        createNotificationChannel()
        soundPlayer = SoundPlayer(this)
        vibratorHelper = VibratorHelper(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START" -> {
                if (!isRunning) {
                    timeLeft = intent.getIntExtra("TIME_LEFT", 0)
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

                    isRunning = true
                    startTimer()
                }
            }
            "PAUSE" -> {
                // --- 핵심 수정 사항: 상태를 먼저 변경하고 UI 업데이트 함수를 호출 ---
                isRunning = false // 1. 서비스의 상태를 '일시정지'로 먼저 변경
                pauseTimer()    // 2. 그 다음에 UI(알림) 업데이트 및 브로드캐스트 실행
            }
            "REQUEST_STATUS" -> {
                sendBroadcast(Intent(TIMER_TICK).apply {
                    putExtra("TIME_LEFT", timeLeft)
                    putExtra("IS_RUNNING", isRunning)
                    putExtra("CURRENT_MODE", currentMode as java.io.Serializable)
                    putExtra("TOTAL_SESSIONS", totalSessions)
                })
            }
            "SKIP" -> {
                job?.cancel()
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
                sendBroadcast(Intent(TIMER_TICK).apply {
                    putExtra("TIME_LEFT", timeLeft)
                    putExtra("IS_RUNNING", isRunning)
                    putExtra("CURRENT_MODE", currentMode as java.io.Serializable)
                    putExtra("TOTAL_SESSIONS", totalSessions)
                })
            }
            "RESET" -> {
                job?.cancel()
                isRunning = false

                // 1. Intent에서 Settings 객체를 가져옵니다.
                val newSettings = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getSerializableExtra("SETTINGS", Settings::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getSerializableExtra("SETTINGS") as? Settings
                }

                // 2. 서비스의 settings와 timeLeft를 업데이트합니다.
                if (newSettings != null) {
                    settings = newSettings
                }

                currentMode = Mode.STUDY
                totalSessions = 0
                // 이제 settings가 null이 아니므로 정확한 공부 시간으로 timeLeft를 설정할 수 있습니다.
                timeLeft = settings?.studyTime?.times(60) ?: (25 * 60) // (혹시 모를 경우를 대비해 기본값 25분 설정)


                // UI와 동기화: 일시정지 상태로 전달
                sendBroadcast(Intent(TIMER_TICK).apply {
                    putExtra("TIME_LEFT", timeLeft)
                    putExtra("IS_RUNNING", false)
                    putExtra("CURRENT_MODE", currentMode as java.io.Serializable)
                    putExtra("TOTAL_SESSIONS", totalSessions)
                })

                updateNotification()
            }

        }
        return START_STICKY
    }

    private fun startTimer() {
        job?.cancel()
        job = CoroutineScope(Dispatchers.Main).launch {
            while (timeLeft > 0) {
                delay(1000)
                timeLeft--
                updateNotification()
                sendBroadcast(Intent(TIMER_TICK).apply {
                    putExtra("TIME_LEFT", timeLeft)
                    putExtra("IS_RUNNING", true)
                    putExtra("CURRENT_MODE", currentMode as java.io.Serializable)
                    putExtra("TOTAL_SESSIONS", totalSessions)
                })
            }

            val currentSettings = settings ?: return@launch

            if (currentSettings.soundEnabled) {
                soundPlayer.playSound()
            }
            if (currentSettings.vibrationEnabled) {
                vibratorHelper.vibrate()
            }

            sendBroadcast(Intent(TIMER_FINISHED))

            var nextMode = Mode.STUDY
            var nextTime = 0
            var newTotalSessions = totalSessions

            if (currentMode == Mode.STUDY) {
                newTotalSessions++
                val isLongBreakTime = newTotalSessions > 0 && newTotalSessions % currentSettings.longBreakInterval == 0
                nextMode = if (isLongBreakTime) Mode.LONG_BREAK else Mode.SHORT_BREAK
                nextTime = if (isLongBreakTime) currentSettings.longBreakTime else currentSettings.shortBreakTime
            } else {
                nextMode = Mode.STUDY
                nextTime = currentSettings.studyTime
            }

            if (currentSettings.autoStart) {
                timeLeft = nextTime * 60
                currentMode = nextMode
                totalSessions = newTotalSessions
                isRunning = true
                startTimer()
            } else {
                // --- ▼▼▼ 여기가 수정된 부분입니다 ▼▼▼ ---
                // 다음 세션의 상태로 업데이트
                timeLeft = nextTime * 60
                currentMode = nextMode
                totalSessions = newTotalSessions
                // 타이머를 '일시정지' 상태로 변경
                isRunning = false
                // 일시정지 로직을 호출하여 알림을 업데이트하고 상태를 브로드캐스트
                pauseTimer()
                // --- ▲▲▲ 여기가 수정된 부분입니다 ▲▲▲ ---
            }
        }
        startForeground(NOTIFICATION_ID, createNotification())
    }

    private fun pauseTimer() {
        job?.cancel()
        updateNotification() // isRunning이 false로 바뀐 후에 호출되므로, 정확한 알림이 생성됨
        sendBroadcast(Intent(TIMER_TICK).apply {
            putExtra("TIME_LEFT", timeLeft)
            putExtra("IS_RUNNING", false)
            putExtra("CURRENT_MODE", currentMode as java.io.Serializable)
            putExtra("TOTAL_SESSIONS", totalSessions)
        })
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
            } else {
                "시간 종료"
            }
        }

        val sessionText = if (currentMode == Mode.STUDY) {
            " | 세션: ${totalSessions + 1}"
        } else {
            ""
        }

        val contentText = "$statusText$sessionText"

        return NotificationCompat.Builder(this, "pomodoro_timer")
            .setContentTitle("뽀모도로 타이머: $modeText")
            .setContentText(contentText)
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