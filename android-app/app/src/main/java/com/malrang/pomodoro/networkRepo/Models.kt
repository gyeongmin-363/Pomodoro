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
    val nickname: String?, // users.nickname
    val coins: Int = 0 // users.coins
)
