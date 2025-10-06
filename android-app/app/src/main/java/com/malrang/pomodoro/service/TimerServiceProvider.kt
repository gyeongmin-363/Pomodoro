package com.malrang.pomodoro.service

import android.content.Context
import android.content.Intent
import com.malrang.pomodoro.dataclass.ui.Settings
import java.io.Serializable

class TimerServiceProvider(private val context: Context) {

    fun start(settings: Settings, timeLeft: Int, currentModeName: String, totalSessions: Int) {
        val intent = Intent(context, TimerService::class.java).apply {
            action = "START"
            putExtra(TimerService.EXTRA_SETTINGS, settings as Serializable)
            putExtra(TimerService.EXTRA_TIME_LEFT, timeLeft)
            putExtra(TimerService.EXTRA_CURRENT_MODE, currentModeName)
            putExtra(TimerService.EXTRA_TOTAL_SESSIONS, totalSessions)
        }
        context.startService(intent)
    }

    fun pause() {
        val intent = Intent(context, TimerService::class.java).apply {
            action = "PAUSE"
        }
        context.startService(intent)
    }

    fun reset(settings: Settings) {
        val intent = Intent(context, TimerService::class.java).apply {
            action = "RESET"
            putExtra(TimerService.EXTRA_SETTINGS, settings as Serializable)
        }
        context.startService(intent)
    }

    // ✅ [수정] 파라미터가 필요 없으므로 삭제합니다.
    fun skip() {
        val intent = Intent(context, TimerService::class.java).apply {
            action = "SKIP"
        }
        context.startService(intent)
    }

    fun requestStatus() {
        val intent = Intent(context, TimerService::class.java).apply {
            action = "REQUEST_STATUS"
        }
        context.startService(intent)
    }
}