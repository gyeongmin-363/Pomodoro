package com.malrang.pomodoro.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.malrang.pomodoro.localRepo.PomodoroRepository
import com.malrang.pomodoro.networkRepo.SupabaseRepository
import com.malrang.pomodoro.service.TimerServiceProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage

/**
 * 로컬 데이터와 타이머 서비스만 필요한 뷰모델을 생성하는 팩토리
 */
class AppViewModelFactory(private val app: Application) : ViewModelProvider.Factory {

    private val pomodoroRepository by lazy { PomodoroRepository(app) }
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
 * [수정됨] SupabaseRepository(네트워크)가 필요한 모든 뷰모델을 생성하는 팩토리
 * Auth, Settings, Stats ViewModel을 담당합니다.
 */
class AuthVMFactory(
    private val app: Application,
    private val supabase: SupabaseClient
) : ViewModelProvider.Factory {

    // 팩토리 내에서 리포지토리 싱글톤처럼 관리
    private val pomodoroRepository by lazy { PomodoroRepository(app) }
    private val supabaseRepository by lazy {
        SupabaseRepository(supabase.postgrest, supabase.storage)
    }

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> {
                AuthViewModel(supabase, pomodoroRepository, supabaseRepository)
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                // SettingsViewModel에 SupabaseRepository 주입
                SettingsViewModel(pomodoroRepository, supabaseRepository)
            }
            modelClass.isAssignableFrom(StatsViewModel::class.java) -> {
                // StatsViewModel에 SupabaseRepository 주입
                StatsViewModel(pomodoroRepository, supabaseRepository)
            }
            else -> {
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        } as T
    }
}