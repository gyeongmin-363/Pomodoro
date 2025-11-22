package com.malrang.pomodoro.service

import android.content.Context
import android.content.Intent
import com.malrang.pomodoro.dataclass.ui.Settings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class TimerServiceProvider(private val context: Context) {

    fun start(settings: Settings, timeLeft: Int, currentModeName: String, totalSessions: Int) {
        val intent = Intent(context, TimerService::class.java).apply {
            action = "START"
            // [수정] 객체를 JSON 문자열로 변환하여 전달 (Serializable 캐스팅 제거)
            val settingsJson = Json.encodeToString(settings)
            putExtra(TimerService.EXTRA_SETTINGS, settingsJson)

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
            // [수정] 객체를 JSON 문자열로 변환하여 전달
            val settingsJson = Json.encodeToString(settings)
            putExtra(TimerService.EXTRA_SETTINGS, settingsJson)
        }
        context.startService(intent)
    }

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