package com.malrang.pomodoro.dataclass.animalInfo

import com.malrang.pomodoro.dataclass.animalInfo.Rarity


/**
 * 수집할 수 있는 동물을 나타내는 열거형 클래스입니다.
 *
 * @property id 동물의 고유 식별자입니다.
 * @property name 동물의 이름입니다.
 * @property rarity 동물의 희귀도입니다.
 */
enum class Animal(
    val id: String,
    val displayName: String,
    val rarity: Rarity
) {
    WHITE_CAT("white_cat", "하얀 고양이", Rarity.COMMON),
    XMAS_CAT("xmas_cat", "크리스마스 고양이", Rarity.RARE),
    TIGER_CAT("tiger_cat", "호랑이 고양이", Rarity.RARE),
    THREE_CAT("three_cat", "삼색 고양이", Rarity.RARE),
    SIAMESE_CAT("siamese_cat", "샴 고양이", Rarity.COMMON),
    EGYPT_CAT("egypt_cat", "이집트 고양이", Rarity.EPIC),
    DEMONIC_CAT("demonic_cat", "악마 고양이", Rarity.LEGENDARY),
    CLASSICAL_CAT("classical_cat", "고전 고양이", Rarity.COMMON),
    BROWN_CAT("brown_cat", "갈색 고양이", Rarity.COMMON),
    BLACK_CAT("black_cat", "검은 고양이", Rarity.COMMON),
    BATMAN_CAT("batman_cat", "배트맨 고양이", Rarity.EPIC);
}



