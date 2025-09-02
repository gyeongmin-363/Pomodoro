package com.malrang.pomodoro.networkRepo


import com.malrang.pomodoro.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.JacksonSerializer
import io.ktor.client.engine.okhttp.OkHttp

object SupabaseProvider {
    val client by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            // 직렬화기
            defaultSerializer = JacksonSerializer()
            // ✅ Ktor 엔진 명시
            httpEngine = OkHttp.create()
            // 모듈 설치
            install(Postgrest)
        }
    }
}