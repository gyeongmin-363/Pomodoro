package com.malrang.pomodoro.networkRepo


import com.malrang.pomodoro.BuildConfig
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.ExternalAuthAction
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.serializer.JacksonSerializer
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.serialization.json.Json

object SupabaseProvider {
    val client by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            // 직렬화기
            defaultSerializer = KotlinXSerializer(Json {
                ignoreUnknownKeys = true // ✅ DB에 있는데 DTO에 없는 필드(created_at 등) 무시 (에러 방지)
                encodeDefaults = true    // ✅ 기본값이 있는 필드도 JSON에 포함
                isLenient = true         // ✅ 형식이 조금 어긋나도 유연하게 파싱
            })
            // ✅ Ktor 엔진 명시
            httpEngine = OkHttp.create()
            // 모듈 설치
//            install(Postgrest)
            install(Auth){
                host = "auth"
                scheme = "pixbbo"
                // 안드로이드에서 CustomTabs 사용 예:
                defaultExternalAuthAction = ExternalAuthAction.CustomTabs()
                // 필요시 flowType = FlowType.PKCE
            }
//            install(Realtime)
            install(Storage)
        }
    }
}