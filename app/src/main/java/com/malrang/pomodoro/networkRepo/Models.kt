package com.malrang.pomodoro.networkRepo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * public.users 테이블에 매핑되는 데이터 클래스
 * @Serializable: kotlinx.serialization 라이브러리를 통해 JSON 직렬화/역직렬화 지원
 * @SerialName: JSON 필드명과 Kotlin 프로퍼티명을 매핑
 */
@Serializable
data class User(
    @SerialName("id")
    val id: String, // Supabase는 UUID를 문자열로 다루는 경우가 많으므로 String 타입이 더 편리할 수 있습니다.
    @SerialName("name")
    val name: String
)

/**
 * public.animals 테이블에 매핑되는 데이터 클래스
 */
@Serializable
data class Animal(
    @SerialName("id")
    val id: Long,
    @SerialName("name")
    val name: String? = null,
    @SerialName("rarity")
    val rarity: String? = null,
    @SerialName("image_url")
    val imageUrl: String? = null
)

/**
 * public.study_rooms 테이블에 매핑되는 데이터 클래스
 */
@Serializable
data class StudyRoom(
    @SerialName("id")
    val id: String,
    @SerialName("name")
    val name: String,
    @SerialName("habit_days")
    val habitDays: Int,
    @SerialName("creator_id")
    val creatorId: String? = null
)

/**
 * public.study_room_members 테이블에 매핑되는 데이터 클래스
 */
@Serializable
data class StudyRoomMember(
    @SerialName("id")
    val id: String,
    @SerialName("study_room_id")
    val studyRoomId: String? = null,
    @SerialName("user_id")
    val userId: String? = null,
    @SerialName("nickname")
    val nickname: String,
    @SerialName("animal")
    val animal: Long, // animals.id 외래 키
    @SerialName("is_admin")
    val isAdmin: Boolean = false
)

/**
 * public.habit_progress 테이블에 매핑되는 데이터 클래스
 */
@Serializable
data class HabitProgress(
    @SerialName("id")
    val id: String,
    @SerialName("study_room_id")
    val studyRoomId: String? = null,
    @SerialName("user_id")
    val userId: String? = null,
    @SerialName("day_number")
    val dayNumber: Int,
    @SerialName("is_done")
    val isDone: Boolean = false
)

