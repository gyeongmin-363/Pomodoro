package com.malrang.pomodoro.dataclass.ui

// 일별 기록
data class DailyStat(
    val date: String,          // "yyyy-MM-dd"
    val studyTimeByWork: Map<String, Int>? = emptyMap(),
    val breakTimeByWork: Map<String, Int>? = emptyMap()
) {
    // 도우미 속성
    val totalStudyTimeInMinutes: Int
        // studyTimeByWork가 null일 경우 0을 반환하도록 하여 NullPointerException 방지
        get() = studyTimeByWork?.values?.sum() ?: 0

    val totalBreakTimeInMinutes: Int
        // breakTimeByWork가 null일 경우 0을 반환하도록 하여 NullPointerException 방지
        get() = breakTimeByWork?.values?.sum() ?: 0
}