package com.malrang.pomodoro.data

/**
 * 수집할 수 있는 동물을 나타내는 데이터 클래스입니다.
 *
 * @property id 동물의 고유 식별자입니다.
 * @property name 동물의 이름입니다.
 * @property emoji 동물을 나타내는 이모지입니다.
 * @property rarity 동물의 희귀도입니다.
 */
data class Animal(
    val id: String,
    val name: String,
    val emoji: String,
    val rarity: Rarity
)

/**
 * 동물의 희귀도를 나타내는 열거형 클래스입니다.
 */
enum class Rarity {
    /** 일반 */
    COMMON,
    /** 레어 */
    RARE,
    /** 에픽 */
    EPIC,
    /** 레전더리 */
    LEGENDARY
}
