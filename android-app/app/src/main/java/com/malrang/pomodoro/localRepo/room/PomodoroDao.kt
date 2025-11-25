package com.malrang.pomodoro.localRepo.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface PomodoroDao {
    // --- DailyStat 관련 ---
    @Query("SELECT * FROM daily_stats")
    suspend fun getAllDailyStats(): List<DailyStatEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyStats(stats: List<DailyStatEntity>)

    // --- WorkPreset 관련 ---

    // 삭제되지 않은(isDeleted = 0) 프리셋만 조회 (UI 표시용)
    @Query("SELECT * FROM work_presets WHERE isDeleted = 0")
    suspend fun getActiveWorkPresets(): List<WorkPresetEntity>

    // [동기화용] 삭제된 것을 포함해 모든 프리셋 조회
    @Query("SELECT * FROM work_presets")
    suspend fun getAllWorkPresetsIncludeDeleted(): List<WorkPresetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorkPresets(presets: List<WorkPresetEntity>)

    // [추가] 기존 프리셋 업데이트용 메서드
    @Update
    suspend fun updateWorkPreset(preset: WorkPresetEntity)

    // Soft Delete
    @Query("UPDATE work_presets SET isDeleted = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteWorkPreset(id: String, timestamp: Long = System.currentTimeMillis())
}