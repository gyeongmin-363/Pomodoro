package com.malrang.pomodoro.localRepo.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.malrang.pomodoro.dataclass.ui.DailyStat
import com.malrang.pomodoro.dataclass.ui.Settings
import com.malrang.pomodoro.dataclass.ui.WorkPreset

// [DailyStat] 일일 통계 엔티티
@Entity(tableName = "daily_stats")
data class DailyStatEntity(
    @PrimaryKey val date: String, // "yyyy-MM-dd"
    val studyTimeByWork: Map<String, Int>,
    val breakTimeByWork: Map<String, Int>,

    // [추가] 체크리스트
    val checklist: Map<String, Boolean> = emptyMap(),

    // 회고 내용
    val retrospect: String? = null,

    // [동기화 필수] 데이터가 마지막으로 수정된 시간 (Unix Timestamp)
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain() = DailyStat(
        date = date,
        studyTimeByWork = studyTimeByWork,
        breakTimeByWork = breakTimeByWork,
        checklist = checklist, // 매핑 추가
        retrospect = retrospect
    )
}

fun DailyStat.toEntity() = DailyStatEntity(
    date = date,
    studyTimeByWork = studyTimeByWork ?: emptyMap(),
    breakTimeByWork = breakTimeByWork ?: emptyMap(),
    checklist = checklist, // 매핑 추가
    retrospect = retrospect,
    updatedAt = System.currentTimeMillis() // 저장 시점의 시간으로 갱신
)

// ... (WorkPresetEntity는 변경 없음) ...
@Entity(tableName = "work_presets")
data class WorkPresetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val settings: Settings,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
) {
    fun toDomain() = WorkPreset(id, name, settings)
}

fun WorkPreset.toEntity() = WorkPresetEntity(
    id = id,
    name = name,
    settings = settings,
    updatedAt = System.currentTimeMillis(),
    isDeleted = false
)