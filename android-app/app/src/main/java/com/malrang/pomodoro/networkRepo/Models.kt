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
    val inform: String? = null,
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
data class HabitSummary(
    val id: String,
    val study_room_id: String? = null,
    val user_id: String? = null,
    val year_month: String,        // "2025-09" 형태
    val daily_progress: String,    // "1010010..." 형태, 각 자리 = 하루 인증 여부
    val updated_at: String? = null // ISO timestamp, optional
)

@Keep
@Serializable
data class ChatMessage(
    val id: Long? = null,
    val created_at: String,
    val study_room_id: String,
    val user_id: String,
    val message: String
)


// 간단히 id만 매핑 받는 용도
@Keep
@Serializable
data class StudyRoomMemberRef(
    val study_room_id: String
)

/**
 * UI에서 랭킹을 표시하기 위해 멤버 정보와 월별 진행 상황을 결합한 데이터 클래스
 */
data class StudyRoomMemberWithProgress(
    val member: StudyRoomMember,
    val completedDays: Int,
    val progress: Float
)