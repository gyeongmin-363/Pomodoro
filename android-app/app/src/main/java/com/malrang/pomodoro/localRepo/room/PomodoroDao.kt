package com.malrang.pomodoro.localRepo.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PomodoroDao {
    // --- DailyStat 관련 ---
    @Query("SELECT * FROM daily_stats")
    suspend fun getAllDailyStats(): List<DailyStatEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyStats(stats: List<DailyStatEntity>)

    // --- WorkPreset 관련 ---

    // [변경] 삭제되지 않은(isDeleted = 0) 프리셋만 조회 (UI 표시용)
    @Query("SELECT * FROM work_presets WHERE isDeleted = 0")
    suspend fun getActiveWorkPresets(): List<WorkPresetEntity>

    // [동기화용] 삭제된 것을 포함해 모든 프리셋 조회 (서버 동기화용)
    @Query("SELECT * FROM work_presets")
    suspend fun getAllWorkPresetsIncludeDeleted(): List<WorkPresetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkPresets(presets: List<WorkPresetEntity>)

    // [변경] 실제 삭제(DELETE) 대신 isDeleted 플래그를 1(true)로 변경 (Soft Delete)
    @Query("UPDATE work_presets SET isDeleted = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteWorkPreset(id: String, timestamp: Long = System.currentTimeMillis())
}