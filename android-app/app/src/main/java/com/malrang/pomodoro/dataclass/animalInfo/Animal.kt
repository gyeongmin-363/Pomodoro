package com.malrang.pomodoro.dataclass.animalInfo

/**
 * 수집할 수 있는 동물을 나타내는 열거형 클래스입니다.
 *
 * 각 항목의 description은 고양이의 특징을 살려 귀엽게 표현했습니다.
 *
 * @property id 동물의 고유 식별자입니다.
 * @property displayName 동물의 표시 이름입니다.
 * @property rarity 동물의 희귀도입니다.
 * @property description 동물에 대한 짧은 설명입니다.
 */
enum class Animal(
    val id: String,
    val displayName: String,
    val description: String
) {
    /** Common 일반 */
    CLASSICAL_CAT(
        "classical_cat",
        "고전 고양이",
        "언제나 편안하게 휴식을 즐기는 가장 익숙한 고양이입니다."
    ),
}
