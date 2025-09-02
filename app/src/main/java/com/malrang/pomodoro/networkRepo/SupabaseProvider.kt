package com.malrang.pomodoro.networkRepo


import com.malrang.pomodoro.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.JacksonSerializer

object SupabaseProvider {
    val client by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            // Jackson serializer를 기본 직렬화기로 사용
            defaultSerializer = JacksonSerializer()
            // Postgrest (DB) 모듈 설치
            install(Postgrest)
        }
    }
}