package com.malrang.pomodoro.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
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
import com.malrang.pomodoro.dataclass.ui.DailyStat
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
import java.time.LocalDate
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

    private lateinit var wakeLock: PowerManager.WakeLock


    override fun onCreate() {
        super.onCreate()
        isServiceActive = true
        createNotificationChannel()
        soundPlayer = SoundPlayer(this)
        vibratorHelper = VibratorHelper(this)
        repo = PomodoroRepository(this)

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Pomodoro::TimerWakeLock")
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
                if (wakeLock.isHeld) {
                    wakeLock.release()
                }

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
        wakeLock.acquire(90*60*1000L /*90 minutes*/)
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
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
        updateNotification()
        sendBroadcast(Intent(TIMER_TICK).apply {
            putExtra("TIME_LEFT", timeLeft)
            putExtra("IS_RUNNING", false)
            putExtra("CURRENT_MODE", currentMode as java.io.Serializable)
            putExtra("TOTAL_SESSIONS", totalSessions)
        })
    }

    private fun handleSessionCompletion(finishedMode: Mode) {
        CoroutineScope(Dispatchers.IO).launch {
            updateTodayStats(finishedMode)

            if (finishedMode == Mode.STUDY) {
                val animal = getRandomAnimal()
                val sprite = makeSprite(animal)
                val updatedSeenIds = repo.loadSeenIds() + animal.id
                val updatedSprites = repo.loadActiveSprites() + sprite
                repo.saveSeenIds(updatedSeenIds)
                repo.saveActiveSprites(updatedSprites)
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
            Mode.SHORT_BREAK -> {
                val newBreakTimeMap = (todayStat.breakTimeByWork ?: emptyMap()).toMutableMap()
                val currentWorkTime = newBreakTimeMap.getOrDefault(currentWorkName, 0)
                newBreakTimeMap[currentWorkName] = currentWorkTime + currentSettings.shortBreakTime
                todayStat.copy(breakTimeByWork = newBreakTimeMap)
            }
            Mode.LONG_BREAK -> {
                val newBreakTimeMap = (todayStat.breakTimeByWork ?: emptyMap()).toMutableMap()
                val currentWorkTime = newBreakTimeMap.getOrDefault(currentWorkName, 0)
                newBreakTimeMap[currentWorkName] = currentWorkTime + currentSettings.longBreakTime
                todayStat.copy(breakTimeByWork = newBreakTimeMap)
            }
        }
        currentStatsMap[today] = updatedStat
        repo.saveDailyStats(currentStatsMap)
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
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 2022
        const val TIMER_TICK = "com.malrang.pomodoro.TIMER_TICK"
        private var isServiceActive = false
        fun isServiceActive(): Boolean = isServiceActive
    }
}