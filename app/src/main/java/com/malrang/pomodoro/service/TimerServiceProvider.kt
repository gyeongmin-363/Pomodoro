package com.malrang.pomodoro.service

import android.content.Context
import android.content.Intent
import com.malrang.pomodoro.dataclass.ui.Mode
import com.malrang.pomodoro.dataclass.ui.Settings
import java.io.Serializable

/**
 * ViewModel과 TimerService 간의 통신을 담당하는 클래스.
 * Service에 명령을 보내는 Intent 생성을 캡슐화합니다.
 */
class TimerServiceProvider(private val context: Context) {

    /**
     * 타이머 서비스를 시작합니다.
     * @param timeLeft 초기 시간 (초)
     * @param settings 현재 설정 객체
     * @param currentMode 현재 타이머 모드 (STUDY, BREAK)
     * @param totalSessions 완료한 총 세션 수
     */
    fun start(timeLeft: Int, settings: Settings, currentMode: Mode, totalSessions: Int) {
        val intent = Intent(context, TimerService::class.java).apply {
            action = "START"
            putExtra("TIME_LEFT", timeLeft)
            // --- 변경: 서비스에 필요한 모든 정보를 전달 ---
            putExtra("SETTINGS", settings as Serializable)
            putExtra("CURRENT_MODE", currentMode as Serializable)
            putExtra("TOTAL_SESSIONS", totalSessions)
        }
        context.startService(intent)
    }

    fun pause() {
        val intent = Intent(context, TimerService::class.java).apply {
            action = "PAUSE"
        }
        context.startService(intent)
    }

    fun reset(timeLeft: Int) {
        val intent = Intent(context, TimerService::class.java).apply {
            action = "RESET"
            putExtra("TIME_LEFT", timeLeft)
        }
        context.startService(intent)
    }

    fun requestStatus() {
        val intent = Intent(context, TimerService::class.java).apply {
            action = "REQUEST_STATUS"
        }
        context.startService(intent)
    }

    /**
     * ✅ 새로 추가된 메서드: 세션 건너뛰기
     * Service에서 DB 저장 없이 다음 모드로 강제 전환합니다.
     */
    fun skip(currentMode: Mode, totalSessions: Int) {
        val intent = Intent(context, TimerService::class.java).apply {
            action = "SKIP"
            putExtra("CURRENT_MODE", currentMode as Serializable)
            putExtra("TOTAL_SESSIONS", totalSessions)
        }
        context.startService(intent)
    }
}