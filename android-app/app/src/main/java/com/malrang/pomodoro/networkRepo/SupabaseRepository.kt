package com.malrang.pomodoro.networkRepo

import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.base.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SupabaseRepository(
    private val postgrest: Postgrest,
    private val storage: Storage,
) {
    /**
     * 특정 ID의 유저 정보를 가져옵니다.
     * @param userId 가져올 유저의 ID
     * @return User 객체 (없으면 null)
     */
    suspend fun getUserProfile(userId: String): User? {
        return postgrest["users"]
            .select {
                filter {
                    eq("id", userId)
                }
            }
            .decodeSingleOrNull<User>()
    }

    /**
     * 특정 유저의 코인을 증가시킵니다. (Edge Function 호출)
     */
    suspend fun incrementUserCoins(userId: String, coinIncrement: Int) {
        postgrest.rpc(
            "increment_user_coin",
            mapOf(
                "user_id_input" to userId,
                "coin_increment" to coinIncrement
            )
        )
    }

    /**
     * 사용자의 닉네임을 업데이트합니다.
     * @param userId 닉네임을 업데이트할 유저의 ID
     * @param nickname 새로운 닉네임
     */
    suspend fun updateNickname(userId: String, nickname: String) {
        withContext(Dispatchers.IO) {
            postgrest["users"]
                .update({
                    set("nickname", nickname)
                }) {
                    filter {
                        eq("id", userId)
                    }
                }
        }
    }

    /**
     * 닉네임이 사용 가능한지 확인합니다.
     * @param nickname 확인할 닉네임
     * @return 사용 가능하면 true, 아니면 false
     */
    suspend fun isNicknameAvailable(nickname: String): Boolean {
        return withContext(Dispatchers.IO) {
            val result = postgrest["users"].select {
                filter { eq("nickname", nickname) }
            }.decodeList<User>()   // User는 users 테이블 매핑 데이터 클래스

            result.isEmpty()   // 비어 있으면 사용 가능
        }
    }

}