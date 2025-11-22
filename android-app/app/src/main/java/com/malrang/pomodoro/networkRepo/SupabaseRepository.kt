package com.malrang.pomodoro.networkRepo

import com.malrang.pomodoro.dataclass.ui.DailyStat
import com.malrang.pomodoro.dataclass.ui.Settings
import com.malrang.pomodoro.dataclass.ui.WorkPreset
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SupabaseRepository(
    private val postgrest: Postgrest,
    private val storage: Storage,
) {

    // --- Daily Stats (통계) ---

    suspend fun upsertDailyStat(userId: String, stat: DailyStat) {
        withContext(Dispatchers.IO) {
            // DB 테이블 구조에 맞춰 DTO로 변환하거나 직접 매핑 (여기서는 예시로 직접 매핑 가정)
            // 실제 구현 시 data class에 @Serializable이 붙어있어야 합니다.
            val dto = DailyStatDto(
                user_id = userId,
                date = stat.date,
                total_study_time = stat.totalStudyTimeInMinutes,
                study_time_by_work = stat.studyTimeByWork,
                break_time_by_work = stat.breakTimeByWork,
                checklist = stat.checklist,
                retrospect = stat.retrospect
            )
            postgrest["daily_stats"].upsert(dto) {
                onConflict = "user_id, date" // PK가 user_id와 date 복합키라고 가정
            }
        }
    }

    suspend fun getDailyStats(userId: String): List<DailyStat> {
        return withContext(Dispatchers.IO) {
            val result = postgrest["daily_stats"].select {
                filter { eq("user_id", userId) }
            }.decodeList<DailyStatDto>()

            result.map { dto ->
                DailyStat(
                    date = dto.date,
                    studyTimeByWork = dto.study_time_by_work,
                    breakTimeByWork = dto.break_time_by_work,
                    checklist = dto.checklist ?: emptyMap(),
                    retrospect = dto.retrospect
                )
            }
        }
    }

    // --- Settings (설정) ---

    suspend fun upsertSettings(userId: String, settings: Settings) {
        withContext(Dispatchers.IO) {
            val dto = SettingsDto(userId, settings)
            postgrest["user_settings"].upsert(dto) {
                onConflict = "user_id"
            }
        }
    }

    suspend fun getSettings(userId: String): Settings? {
        return withContext(Dispatchers.IO) {
            postgrest["user_settings"].select {
                filter { eq("user_id", userId) }
            }.decodeSingleOrNull<SettingsDto>()?.settings
        }
    }

    // --- Work Presets (프리셋) ---

    suspend fun upsertWorkPresets(userId: String, presets: List<WorkPreset>) {
        withContext(Dispatchers.IO) {
            val dtos = presets.map { WorkPresetDto(it.id, userId, it.name, it.settings) }
            if (dtos.isNotEmpty()) {
                postgrest["work_presets"].upsert(dtos) {
                    onConflict = "id"
                }
            }
        }
    }

    suspend fun getWorkPresets(userId: String): List<WorkPreset> {
        return withContext(Dispatchers.IO) {
            postgrest["work_presets"].select {
                filter { eq("user_id", userId) }
            }.decodeList<WorkPresetDto>().map {
                WorkPreset(id = it.id, name = it.name, settings = it.settings)
            }
        }
    }
}