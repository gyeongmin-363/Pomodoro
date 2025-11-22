package com.malrang.pomodoro.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.malrang.pomodoro.localRepo.PomodoroRepository
import com.malrang.pomodoro.service.TimerServiceProvider
import io.github.jan.supabase.SupabaseClient
import kotlin.jvm.java

/**
 * 앱의 주요 뷰모델들을 생성하는 팩토리입니다.
 * 공통 의존성을 공유하여 메모리 효율성을 높입니다.
 */
class AppViewModelFactory(private val app: Application) : ViewModelProvider.Factory {

    // 여러 뷰모델에서 공유되는 의존성은 lazy를 통해 한 번만 생성되도록 합니다.
    private val pomodoroRepository by lazy { PomodoroRepository(app) }
    private val timerServiceProvider by lazy { TimerServiceProvider(app) }

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        val viewModel = when {
            modelClass.isAssignableFrom(TimerViewModel::class.java) -> {
                TimerViewModel(pomodoroRepository, timerServiceProvider)
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(pomodoroRepository)
            }
            modelClass.isAssignableFrom(PermissionViewModel::class.java) -> {
                PermissionViewModel(pomodoroRepository)
            }
            modelClass.isAssignableFrom(StatsViewModel::class.java) -> {
                StatsViewModel(pomodoroRepository)
            }
            // [추가] BackgroundViewModel 생성 로직
            modelClass.isAssignableFrom(BackgroundViewModel::class.java) -> {
                BackgroundViewModel(pomodoroRepository)
            }
            else -> {
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
        return viewModel as T
    }
}

class AuthVMFactory(
    private val supabase: SupabaseClient
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(supabase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
    }
}