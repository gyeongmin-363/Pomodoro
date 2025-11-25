package com.malrang.pomodoro

import android.app.Application
import com.malrang.pomodoro.localRepo.PomodoroRepository
import com.malrang.pomodoro.networkRepo.SupabaseProvider
import com.malrang.pomodoro.networkRepo.SupabaseRepository
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage

class PomodoroApplication : Application() {
    // 앱 전체에서 공유할 유일한 리포지토리 인스턴스
    lateinit var pomodoroRepository: PomodoroRepository
    lateinit var supabaseRepository: SupabaseRepository

    override fun onCreate() {
        super.onCreate()

        // 1. Supabase Client 초기화 (기존 로직이 SupabaseProvider lazy에 있다면 접근 시 초기화됨)
        val supabase = SupabaseProvider.client

        // 2. 리포지토리 싱글톤 생성
        pomodoroRepository = PomodoroRepository(this)
        supabaseRepository = SupabaseRepository(supabase.postgrest, supabase.storage)
    }
}