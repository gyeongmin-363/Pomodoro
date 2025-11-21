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

    // [추가] 회고 내용
    val retrospect: String? = null,

    // [동기화 필수] 데이터가 마지막으로 수정된 시간 (Unix Timestamp)
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain() = DailyStat(
        date = date,
        studyTimeByWork = studyTimeByWork,
        breakTimeByWork = breakTimeByWork,
        retrospect = retrospect
    )
}

fun DailyStat.toEntity() = DailyStatEntity(
    date = date,
    studyTimeByWork = studyTimeByWork ?: emptyMap(),
    breakTimeByWork = breakTimeByWork ?: emptyMap(),
    retrospect = retrospect,
    updatedAt = System.currentTimeMillis() // 저장 시점의 시간으로 갱신
)

// [WorkPreset] 작업 프리셋 엔티티
@Entity(tableName = "work_presets")
data class WorkPresetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val settings: Settings,

    // [동기화 필수] 마지막 수정 시간
    val updatedAt: Long = System.currentTimeMillis(),

    // [동기화 필수] 삭제 여부 (Soft Delete)
    // true면 삭제된 것으로 간주하며, 로컬 조회 시 제외하고 서버 동기화 시에는 삭제 상태를 전파함
    val isDeleted: Boolean = false
) {
    fun toDomain() = WorkPreset(id, name, settings)
}

fun WorkPreset.toEntity() = WorkPresetEntity(
    id = id,
    name = name,
    settings = settings,
    updatedAt = System.currentTimeMillis(),
    isDeleted = false // 도메인 객체를 저장할 때는 기본적으로 활성 상태로 간주
)