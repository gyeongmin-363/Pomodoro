package com.malrang.pomodoro.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.malrang.pomodoro.localRepo.PomodoroRepository
import com.malrang.pomodoro.networkRepo.StudyRoomRepository
import com.malrang.pomodoro.networkRepo.SupabaseProvider
import com.malrang.pomodoro.service.TimerServiceProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage


class PomodoroVMFactory(private val app: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PomodoroViewModel::class.java)) {
            val localDatastoreRepo = PomodoroRepository(app)
            val timerServiceProvider = TimerServiceProvider(app)

            @Suppress("UNCHECKED_CAST")
            return PomodoroViewModel(
                localRepo = localDatastoreRepo,
                timerService = timerServiceProvider,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
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

class StudyRoomVMFactory() : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StudyRoomViewModel::class.java)) {
            val studyRoomRepo = StudyRoomRepository(
                postgrest = SupabaseProvider.client.postgrest,
                storage = SupabaseProvider.client.storage
            )

            @Suppress("UNCHECKED_CAST")
            return StudyRoomViewModel(studyRoomRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
    }
}