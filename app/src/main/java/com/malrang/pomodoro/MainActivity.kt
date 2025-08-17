package com.malrang.pomodoro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.malrang.pomodoro.ui.PomodoroApp
import com.malrang.pomodoro.ui.theme.PomodoroTheme

/**
 * 앱의 메인 액티비티입니다.
 * 앱의 진입점 역할을 하며, [PomodoroApp] 컴포저블을 표시합니다.
 */
class MainActivity : ComponentActivity() {
    /**
     * 액티비티가 생성될 때 호출됩니다.
     * @param savedInstanceState 이전에 저장된 액티비티 상태입니다.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PomodoroTheme {
                PomodoroApp()
            }
        }
    }
}
