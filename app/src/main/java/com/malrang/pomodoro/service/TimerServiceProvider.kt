package com.malrang.pomodoro.service

import android.content.Context
import android.content.Intent
import com.malrang.pomodoro.TimerService

class TimerServiceProvider(private val context: Context) {
    fun start(timeLeft: Int) {
        Intent(context, TimerService::class.java).also { intent ->
            intent.action = "START"
            intent.putExtra("TIME_LEFT", timeLeft)
            context.startService(intent)
        }
    }

    fun pause() {
        Intent(context, TimerService::class.java).also { intent ->
            intent.action = "PAUSE"
            context.startService(intent)
        }
    }

    fun reset(timeLeft: Int) {
        Intent(context, TimerService::class.java).also { intent ->
            intent.action = "RESET"
            intent.putExtra("TIME_LEFT", timeLeft)
            context.startService(intent)
        }
    }
}