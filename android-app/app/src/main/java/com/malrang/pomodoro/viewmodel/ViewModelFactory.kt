package com.malrang.pomodoro.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.malrang.pomodoro.localRepo.PomodoroRepository
import com.malrang.pomodoro.networkRepo.SupabaseRepository
import com.malrang.pomodoro.service.TimerServiceProvider
import io.github.jan.supabase.SupabaseClient

/**
 * 로컬 데이터와 타이머 서비스만 필요한 뷰모델을 생성하는 팩토리
 * [수정] PomodoroRepository를 생성자로 주입받음
 */
class AppViewModelFactory(
    private val app: Application,
    private val pomodoroRepository: PomodoroRepository // 외부 주입
) : ViewModelProvider.Factory {

    private val timerServiceProvider by lazy { TimerServiceProvider(app) }

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return when {
            modelClass.isAssignableFrom(TimerViewModel::class.java) -> {
                TimerViewModel(pomodoroRepository, timerServiceProvider)
            }
            modelClass.isAssignableFrom(PermissionViewModel::class.java) -> {
                PermissionViewModel(pomodoroRepository)
            }
            modelClass.isAssignableFrom(BackgroundViewModel::class.java) -> {
                BackgroundViewModel(pomodoroRepository)
            }
            else -> {
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        } as T
    }
}

/**
 * SupabaseRepository(네트워크)가 필요한 모든 뷰모델을 생성하는 팩토리
 * Auth, Settings, Stats ViewModel을 담당합니다.
 * [수정] Repository들을 생성자로 주입받음
 */
class AuthVMFactory(
    private val app: Application,
    private val supabase: SupabaseClient,
    private val pomodoroRepository: PomodoroRepository, // 외부 주입
    private val supabaseRepository: SupabaseRepository  // 외부 주입
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> {
                AuthViewModel(supabase, pomodoroRepository, supabaseRepository)
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(pomodoroRepository, supabaseRepository)
            }
            modelClass.isAssignableFrom(StatsViewModel::class.java) -> {
                StatsViewModel(pomodoroRepository, supabaseRepository)
            }
            else -> {
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        } as T
    }
}