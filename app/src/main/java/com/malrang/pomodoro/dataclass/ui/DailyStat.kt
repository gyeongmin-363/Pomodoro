package com.malrang.pomodoro.dataclass.ui

// 일별 기록
data class DailyStat(
    val date: String,          // "yyyy-MM-dd"
    val studyTimeInMinutes: Int, // 완료한 공부 시간 (분)
    val breakTimeInMinutes: Int  // 완료한 휴식 시간 (분)
)