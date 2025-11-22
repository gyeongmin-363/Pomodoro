package com.malrang.pomodoro.networkRepo

import com.malrang.pomodoro.dataclass.ui.DailyStat
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
            val dto = DailyStatDto(
                user_id = userId,
                date = stat.date,
                total_study_time = stat.totalStudyTimeInMinutes,
                study_time_by_work = stat.studyTimeByWork,
                break_time_by_work = stat.breakTimeByWork,
                checklist = stat.checklist ?: emptyMap(),
                retrospect = stat.retrospect
            )
            postgrest["daily_stats"].upsert(dto) {
                onConflict = "user_id, date"
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

    // [추가] 프리셋 삭제 기능
    suspend fun deleteWorkPreset(userId: String, presetId: String) {
        withContext(Dispatchers.IO) {
            postgrest["work_presets"].delete {
                filter {
                    eq("user_id", userId)
                    eq("id", presetId)
                }
            }
        }
    }
}