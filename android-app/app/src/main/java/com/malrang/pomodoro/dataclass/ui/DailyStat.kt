package com.malrang.pomodoro.dataclass.ui

// 일별 기록
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
}