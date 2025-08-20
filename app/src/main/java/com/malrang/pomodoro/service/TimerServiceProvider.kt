package com.malrang.pomodoro.service

import android.content.Context
import android.content.Intent

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

    /**
     * 실행 중인 TimerService에 현재 상태(남은 시간, 실행 여부)를 요청합니다.
     * 서비스는 이 요청을 받으면 즉시 현재 상태를 브로드캐스트합니다.
     */
    fun requestStatus() {
        val intent = Intent(context, TimerService::class.java).apply {
            // TimerService에 정의된 상수 Action을 사용합니다.
            action = "REQUEST_STATUS"
        }
        context.startService(intent)
    }
}