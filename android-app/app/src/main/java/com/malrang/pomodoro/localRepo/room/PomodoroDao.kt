package com.malrang.pomodoro.localRepo.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface PomodoroDao {

    // --- DailyStat (통계) 관련 ---

    // [백업용] 모든 통계 조회
    @Query("SELECT * FROM daily_stats")
    suspend fun getAllDailyStats(): List<DailyStatEntity>

    // [복원/동기화용] 통계 목록 삽입 (충돌 시 교체)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyStats(stats: List<DailyStatEntity>)

    // [복원용] 모든 통계 삭제
    @Query("DELETE FROM daily_stats")
    suspend fun deleteAllDailyStats()


    // --- WorkPreset (프리셋) 관련 ---

    // [수정됨] isDeleted 필드가 사라졌으므로, 조건 없이 모든 프리셋을 가져옵니다.
    // 기존 getActiveWorkPresets -> getAllWorkPresets로 이름 변경 권장
    @Query("SELECT * FROM work_presets")
    suspend fun getAllWorkPresets(): List<WorkPresetEntity>

    // [복원용] 프리셋 목록 삽입 (충돌 시 교체)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkPresets(presets: List<WorkPresetEntity>)

    // [수정용] 단일 프리셋 업데이트
    @Update
    suspend fun updateWorkPreset(preset: WorkPresetEntity)

    // [수정됨] Soft Delete 제거 -> 실제 삭제(Hard Delete)로 변경
    // 기존 softDeleteWorkPreset -> deleteWorkPresetById로 변경
    @Query("DELETE FROM work_presets WHERE id = :id")
    suspend fun deleteWorkPresetById(id: String)

    // [복원용] 모든 프리셋 데이터 삭제
    @Query("DELETE FROM work_presets")
    suspend fun deleteAllWorkPresets()


    // --- 통합 복원 (Transaction) ---

    /**
     * [데이터 복원]
     * 기존 데이터를 모두 삭제하고, 백업된 데이터를 새로 삽입합니다.
     * @Transaction을 사용하여 작업 도중 실패 시 롤백(Rollback)을 보장합니다.
     */
    @Transaction
    suspend fun restoreAllData(stats: List<DailyStatEntity>, presets: List<WorkPresetEntity>) {
        // 1. 기존 데이터 클리어
        deleteAllDailyStats()
        deleteAllWorkPresets()

        // 2. 백업 데이터 삽입
        if (stats.isNotEmpty()) {
            insertDailyStats(stats)
        }
        if (presets.isNotEmpty()) {
            insertWorkPresets(presets)
        }
    }
}