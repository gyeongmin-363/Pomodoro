package com.malrang.pomodoro.networkRepo

import androidx.annotation.Keep
import com.malrang.pomodoro.dataclass.ui.Settings
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
    val retrospect: String?
)

@Keep
@Serializable
data class WorkPresetDto(val id: String, val user_id: String, val name: String, val settings: Settings)