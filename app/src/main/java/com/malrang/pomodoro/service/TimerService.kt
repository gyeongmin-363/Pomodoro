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

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START" -> {
                timeLeft = intent.getIntExtra("TIME_LEFT", 0)
                startTimer()
            }
            "PAUSE" -> {
                pauseTimer()
            }
            "RESET" -> {
                resetTimer()
                timeLeft = intent.getIntExtra("TIME_LEFT", 0)
                updateNotification()
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
            }
            // 시간이 다 되면 서비스 종료
            stopSelf()
        }
        startForeground(NOTIFICATION_ID, createNotification())
    }

    private fun pauseTimer() {
        job?.cancel()
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
        ).apply {
            setShowBadge(false)
        }
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
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
    }

    companion object {
        private const val NOTIFICATION_ID = 2022
    }
}