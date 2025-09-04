package com.malrang.pomodoro.networkRepo

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * public.users 테이블에 매핑되는 데이터 클래스
 */
@Keep
@Serializable
data class User(
    val id: String, // auth.users.id (UUID)
    val name: String // auth.users.user_metadata 에서 가져온 이름
)

/**
 * public.animals 테이블에 매핑되는 데이터 클래스
 */
@Keep
@Serializable
data class Animal(
    val id: Long,
    val name: String? = null,
    val rarity: String? = null,
    val image_url: String? = null
)


/**
 * public.study_rooms 테이블에 매핑되는 데이터 클래스
 */
@Keep
@Serializable
data class StudyRoom(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val habit_days: Int, // habitDays -> habit_days
    val creator_id: String? = null
)

/**
 * public.study_room_members 테이블에 매핑되는 데이터 클래스
 */
@Keep
@Serializable
data class StudyRoomMember(
    val id: String = UUID.randomUUID().toString(),
    val study_room_id: String? = null,
    val user_id: String? = null,
    val nickname: String,
    val animal: Long? = null,
    val is_admin: Boolean = false
)

/**
 * public.habit_progress 테이블에 매핑되는 데이터 클래스
 */
@Keep
@Serializable
data class HabitProgress(
    val id: String,
    val study_room_id: String? = null,
    val user_id: String? = null,
    val day_number: Int,
    val is_done: Boolean = false
)