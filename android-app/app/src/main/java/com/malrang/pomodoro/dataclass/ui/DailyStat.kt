package com.malrang.pomodoro.dataclass.ui

import com.malrang.pomodoro.localRepo.room.DailyStatEntity
import kotlinx.serialization.Serializable

// 일별 기록
@Serializable
data class DailyStat(
    val date: String,          // "yyyy-MM-dd"
    val studyTimeByWork: Map<String, Int>? = emptyMap(),
    val breakTimeByWork: Map<String, Int>? = emptyMap(),
    val checklist: Map<String, Boolean> = emptyMap(),
    val retrospect: String? = null
) {
    val totalStudyTimeInMinutes: Int
        get() = studyTimeByWork?.values?.sum() ?: 0

    val totalBreakTimeInMinutes: Int
        get() = breakTimeByWork?.values?.sum() ?: 0

    fun toEntity() = DailyStatEntity(
        date = date,
        studyTimeByWork = studyTimeByWork ?: emptyMap(),
        breakTimeByWork = breakTimeByWork ?: emptyMap(),
        checklist = checklist, // 매핑 추가
        retrospect = retrospect,
        updatedAt = System.currentTimeMillis() // 저장 시점의 시간으로 갱신
    )
}
