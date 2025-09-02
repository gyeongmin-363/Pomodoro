package com.malrang.pomodoro.networkRepo

import com.malrang.pomodoro.networkRepo.SupabaseProvider.client
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest

class StudyRoomRepository(
    private val postgrest: Postgrest,
){
    suspend fun insertUser(name: String) {
        client.postgrest["users"].insert(
            mapOf("name" to name)
        )
    }

    suspend fun getUsers() : List<User>{
        return postgrest["users"]
            .select().decodeList<User>()
    }

}