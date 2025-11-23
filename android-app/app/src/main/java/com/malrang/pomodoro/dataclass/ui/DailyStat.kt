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

    // [추가] 필터링된 공부 시간을 반환하는 함수
    // "전체"일 경우 총 시간을, 특정 카테고리일 경우 해당 카테고리의 시간을 반환합니다.
    fun getStudyTime(filter: String): Int {
        return if (filter == "전체") {
            totalStudyTimeInMinutes
        } else {
            studyTimeByWork?.get(filter) ?: 0
        }
    }

    fun toEntity() = DailyStatEntity(
        date = date,
        studyTimeByWork = studyTimeByWork ?: emptyMap(),
        breakTimeByWork = breakTimeByWork ?: emptyMap(),
        checklist = checklist,
        retrospect = retrospect,
        updatedAt = System.currentTimeMillis() // 저장 시점의 시간으로 갱신
    )
}