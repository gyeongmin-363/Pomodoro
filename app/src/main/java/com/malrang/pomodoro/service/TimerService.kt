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
import com.malrang.pomodoro.dataclass.animalInfo.Animal
import com.malrang.pomodoro.dataclass.animalInfo.AnimalsTable
import com.malrang.pomodoro.dataclass.animalInfo.Rarity
import com.malrang.pomodoro.dataclass.sprite.AnimalSprite
import com.malrang.pomodoro.dataclass.sprite.SpriteData
import com.malrang.pomodoro.dataclass.sprite.SpriteMap
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
import java.util.UUID
import kotlin.random.Random

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

    // --- ▼▼▼ 추가된 부분 ▼▼▼ ---
    private lateinit var wakeLock: PowerManager.WakeLock
    // --- ▲▲▲ 추가된 부분 ▲▲▲ ---


    override fun onCreate() {
        super.onCreate()
        isServiceActive = true
        createNotificationChannel()
        soundPlayer = SoundPlayer(this)
        vibratorHelper = VibratorHelper(this)
        repo = PomodoroRepository(this)

        // --- ▼▼▼ 추가된 부분 ▼▼▼ ---
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Pomodoro::TimerWakeLock")
        // --- ▲▲▲ 추가된 부분 ▲▲▲ ---
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
                isRunning = false
                pauseTimer()
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
                // --- ▼▼▼ 추가된 부분 ▼▼▼ ---
                if (wakeLock.isHeld) {
                    wakeLock.release()
                }
                // --- ▲▲▲ 추가된 부분 ▲▲▲ ---
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
                // --- ▼▼▼ 추가된 부분 ▼▼▼ ---
                if (wakeLock.isHeld) {
                    wakeLock.release()
                }
                // --- ▲▲▲ 추가된 부분 ▲▲▲ ---

                val newSettings = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getSerializableExtra("SETTINGS", Settings::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getSerializableExtra("SETTINGS") as? Settings
                }

                if (newSettings != null) {
                    settings = newSettings
                }

                currentMode = Mode.STUDY
                totalSessions = 0
                timeLeft = settings?.studyTime?.times(60) ?: (25 * 60)

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
        // --- ▼▼▼ 추가된 부분 ▼▼▼ ---
        // 타이머를 시작할 때 WakeLock을 얻습니다.
        // 1시간 30분(90분) 타임아웃을 설정하여 무한정 유지되지 않도록 합니다.
        wakeLock.acquire(90*60*1000L /*90 minutes*/)
        // --- ▲▲▲ 추가된 부분 ▲▲▲ ---
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
            // --- ▼▼▼ 추가/수정된 부분 ▼▼▼ ---
            // 타이머가 끝나면 WakeLock을 해제합니다.
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
            // --- ▲▲▲ 추가/수정된 부분 ▲▲▲ ---

            val finishedMode = currentMode
            val currentSettings = settings ?: return@launch

            if (currentSettings.soundEnabled) { soundPlayer.playSound() }
            if (currentSettings.vibrationEnabled) { vibratorHelper.vibrate() }

            sendBroadcast(Intent(TIMER_FINISHED))

            if (finishedMode == Mode.STUDY) {
                handleStudySessionCompletion()
            }

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
                isRunning = true
                startTimer()
            } else {
                isRunning = false
                pauseTimer()
            }
        }
        startForeground(NOTIFICATION_ID, createNotification())
    }

    private fun pauseTimer() {
        job?.cancel()
        // --- ▼▼▼ 추가된 부분 ▼▼▼ ---
        // 타이머가 일시정지되면 WakeLock을 해제합니다.
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
        // --- ▲▲▲ 추가된 부분 ▲▲▲ ---
        updateNotification()
        sendBroadcast(Intent(TIMER_TICK).apply {
            putExtra("TIME_LEFT", timeLeft)
            putExtra("IS_RUNNING", false)
            putExtra("CURRENT_MODE", currentMode as java.io.Serializable)
            putExtra("TOTAL_SESSIONS", totalSessions)
        })
    }

    private fun handleStudySessionCompletion() {
        CoroutineScope(Dispatchers.IO).launch {
            val animal = getRandomAnimal()
            val sprite = makeSprite(animal)
            val updatedSeenIds = repo.loadSeenIds() + animal.id
            val updatedSprites = repo.loadActiveSprites() + sprite
            repo.saveSeenIds(updatedSeenIds)
            repo.saveActiveSprites(updatedSprites)
        }
    }

    private fun getRandomAnimal(): Animal {
        val roll = Random.nextInt(100)
        val rarity = when {
            roll < 60 -> Rarity.COMMON
            roll < 85 -> Rarity.RARE
            roll < 97 -> Rarity.EPIC
            else -> Rarity.LEGENDARY
        }
        return AnimalsTable.randomByRarity(rarity)
    }

    private fun makeSprite(animal: Animal): AnimalSprite {
        val spriteData = SpriteMap.map[animal]
            ?: SpriteData(idleRes = R.drawable.classical_idle, jumpRes = R.drawable.classical_jump)
        return AnimalSprite(
            id = UUID.randomUUID().toString(),
            animalId = animal.id,
            idleSheetRes = spriteData.idleRes,
            idleCols = spriteData.idleCols,
            idleRows = spriteData.idleRows,
            jumpSheetRes = spriteData.jumpRes,
            jumpCols = spriteData.jumpCols,
            jumpRows = spriteData.jumpRows,
            x = Random.nextInt(0, 600).toFloat(),
            y = Random.nextInt(0, 1000).toFloat(),
            vx = listOf(-70f, 70f).random(),
            vy = listOf(-50f, 50f).random(),
            sizeDp = 48f
        )
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
            .setOngoing(isRunning)
            .build()
    }


    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
        isRunning = false
        isServiceActive = false
        // --- ▼▼▼ 추가된 부분 ▼▼▼ ---
        // 서비스가 소멸될 때 WakeLock을 해제합니다.
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
        // --- ▲▲▲ 추가된 부분 ▲▲▲ ---
    }

    companion object {
        private const val NOTIFICATION_ID = 2022
        const val TIMER_TICK = "com.malrang.pomodoro.TIMER_TICK"
        const val TIMER_FINISHED = "com.malrang.pomodoro.TIMER_FINISHED"
        private var isServiceActive = false
        fun isServiceActive(): Boolean = isServiceActive
    }
}