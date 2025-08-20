package com.malrang.pomodoro.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.malrang.pomodoro.MainActivity
import com.malrang.pomodoro.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TimerService : Service() {

    private var job: Job? = null
    private var timeLeft: Int = 0
    private var isRunning: Boolean = false

    override fun onCreate() {
        super.onCreate()
        isServiceActive = true // 서비스 생성 시 활성 상태로 설정
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START" -> {
                if (!isRunning) {
                    timeLeft = intent.getIntExtra("TIME_LEFT", 0)
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
            // 상태 요청 액션 추가
            "REQUEST_STATUS" -> {
                // 현재 타이머 상태를 즉시 브로드캐스트
                sendBroadcast(Intent(TIMER_TICK).apply {
                    putExtra("TIME_LEFT", timeLeft)
                    putExtra("IS_RUNNING", isRunning)
                })
            }
        }
        return START_STICKY
    }

    private fun startTimer() {
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
            // 시간이 다 되면 서비스 종료
            sendBroadcast(Intent(TIMER_FINISHED))
            stopSelf()
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
        isServiceActive = false // 서비스 파괴 시 비활성 상태로 설정
    }

    companion object {
        private const val NOTIFICATION_ID = 2022
        const val TIMER_TICK = "com.malrang.pomodoro.TIMER_TICK"
        const val TIMER_FINISHED = "com.malrang.pomodoro.TIMER_FINISHED"

        // 서비스 활성 상태를 저장하는 static 변수
        private var isServiceActive = false
        fun isServiceActive(): Boolean = isServiceActive
    }
}