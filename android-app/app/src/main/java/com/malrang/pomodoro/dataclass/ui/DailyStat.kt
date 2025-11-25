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
    val retrospect: String? = null,
    // [추가] 마지막 업데이트 시간 (기본값: 생성 시점)
    // 동기화 시 충돌 해결(Conflict Resolution)의 기준이 됩니다.
    val updatedAt: Long = System.currentTimeMillis()
) {
    val totalStudyTimeInMinutes: Int
        get() = studyTimeByWork?.values?.sum() ?: 0

    val totalBreakTimeInMinutes: Int
        get() = breakTimeByWork?.values?.sum() ?: 0

    // [추가] 필터링된 공부 시간을 반환하는 함수
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
        // [수정] 무조건 현재 시간이 아니라, 객체가 가진 updatedAt을 그대로 DB에 저장해야 함
        // (그래야 서버에서 가져온 과거의 '최신' 데이터 타임스탬프가 유지됨)
        updatedAt = updatedAt
    )
}