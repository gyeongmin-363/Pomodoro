package com.malrang.pomodoro.dataclass.ui

// 일별 기록
data class DailyStat(
    val date: String,          // "yyyy-MM-dd"
    val studySessions: Int     // 완료한 공부 세션 수
)
