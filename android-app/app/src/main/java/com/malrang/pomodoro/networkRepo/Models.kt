package com.malrang.pomodoro.networkRepo

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class DailyStatDto(
    val user_id: String,
    val date: String,
    val total_study_time: Int,
    val study_time_by_work: Map<String, Int>?,
    val break_time_by_work: Map<String, Int>?,
    val checklist: Map<String, Boolean>?,
    val retrospect: String?,
    // [추가] 마지막 업데이트 시간 (Unix Timestamp 권장)
    // Supabase DB 컬럼 타입이 bigint면 Long, timestamptz면 String 등 맞춰야 함
    // 여기서는 편의상 Long으로 가정 (Entity와 통일)
    val updated_at: Long = 0L
)

@Keep
@Serializable
data class WorkPresetDto(
    val id: String,
    val user_id: String,
    val name: String,
    val settings: com.malrang.pomodoro.dataclass.ui.Settings,
    // [추가] 프리셋도 업데이트 시간 관리하면 좋음
    val updated_at: Long = 0L
)