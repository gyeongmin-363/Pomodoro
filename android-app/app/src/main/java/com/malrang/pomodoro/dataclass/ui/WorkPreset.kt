package com.malrang.pomodoro.dataclass.ui

import java.util.UUID

// 설정 프리셋(Work)을 위한 데이터 클래스
data class WorkPreset(
    val id: String = UUID.randomUUID().toString(), // 고유 식별자
    val name: String,                              // "영어 공부", "코딩 공부" 등
    val settings: Settings                         // 해당 Work에 대한 모든 설정값
)