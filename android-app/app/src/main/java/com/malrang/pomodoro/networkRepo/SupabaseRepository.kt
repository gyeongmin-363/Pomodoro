package com.malrang.pomodoro.networkRepo

import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.base.Functions
import io.github.jan.supabase.postgrest.Postgrest
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
    suspend fun incrementUserCoins(userId: String, amount: Int) {

    }

}