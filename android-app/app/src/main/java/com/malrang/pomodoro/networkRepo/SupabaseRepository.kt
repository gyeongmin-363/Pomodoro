package com.malrang.pomodoro.networkRepo

import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SupabaseRepository(
    private val storage: Storage
) {

    companion object {
        private const val BUCKET_NAME = "backups"
        private const val FILE_NAME = "data.json"
    }

    /**
     * [백업 데이터 업로드]
     * 앱의 모든 데이터를 JSON 문자열로 변환한 뒤, Supabase Storage에 파일로 저장합니다.
     * 경로: backups/{userId}/data.json
     */
    suspend fun uploadBackup(userId: String, backupJson: String) {
        withContext(Dispatchers.IO) {
            val bucket = storage[BUCKET_NAME]
            val path = "$userId/$FILE_NAME"

            // [수정됨] upsert 옵션은 람다 블록 {} 안에서 설정해야 합니다.
            bucket.upload(path, backupJson.toByteArray()) {
                upsert = true
            }
        }
    }

    /**
     * [백업 데이터 다운로드]
     * Supabase Storage에서 사용자의 백업 파일(JSON)을 다운로드합니다.
     */
    suspend fun downloadBackup(userId: String): String {
        return withContext(Dispatchers.IO) {
            val bucket = storage[BUCKET_NAME]
            val path = "$userId/$FILE_NAME"

            // Private 버킷이므로 인증된 요청(downloadAuthenticated)을 사용합니다.
            bucket.downloadAuthenticated(path).decodeToString()
        }
    }
}