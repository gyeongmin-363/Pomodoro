package com.malrang.pomodoro.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.malrang.pomodoro.localRepo.PomodoroRepository
import com.malrang.pomodoro.networkRepo.SupabaseProvider
import com.malrang.pomodoro.networkRepo.SupabaseRepository
import com.malrang.pomodoro.service.TimerServiceProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage

/**
 * 앱의 주요 뷰모델들을 생성하는 팩토리입니다.
 * 공통 의존성을 공유하여 메모리 효율성을 높입니다.
 */
class AppViewModelFactory(private val app: Application) : ViewModelProvider.Factory {

    // Repository 인스턴스를 lazy로 생성하여 싱글톤처럼 관리
    private val pomodoroRepository by lazy { PomodoroRepository(app) }
    private val timerServiceProvider by lazy { TimerServiceProvider(app) }

    // [수정] SupabaseRepository 추가 (StatsViewModel 등에서 사용)
    private val supabaseRepository by lazy {
        SupabaseRepository(SupabaseProvider.client.postgrest, SupabaseProvider.client.storage)
    }

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
                // [수정] StatsViewModel에 SupabaseRepository 주입
                StatsViewModel(pomodoroRepository, supabaseRepository)
            }
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

// [수정] Application 컨텍스트를 받아 로컬 DB 접근이 가능하도록 변경
class AuthVMFactory(
    private val app: Application,
    private val supabase: SupabaseClient
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            val repository = PomodoroRepository(app)
            val supabaseRepository = SupabaseRepository(supabase.postgrest, supabase.storage)

            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(supabase, repository, supabaseRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
    }
}