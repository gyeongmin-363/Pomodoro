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
    val rarity: Rarity,
    val description: String
) {
    /** Common 일반 */
    CLASSICAL_CAT(
        "classical_cat",
        "고전 고양이",
        Rarity.COMMON,
        "언제나 편안하게 휴식을 즐기는 가장 익숙한 고양이입니다."
    ),
    WHITE_CAT(
        "white_cat",
        "하얀 고양이",
        Rarity.COMMON,
        "눈처럼 새하얀 털을 가진 순수한 고양이입니다."
    ),
    SIAMESE_CAT(
        "siamese_cat",
        "샴 고양이",
        Rarity.COMMON,
        "푸른 눈빛과 우아한 몸선을 가진 고양이입니다."
    ),
    BROWN_CAT(
        "brown_cat",
        "갈색 고양이",
        Rarity.COMMON,
        "따뜻한 갈색 털을 가진 온화한 고양이입니다."
    ),
    BLACK_CAT(
        "black_cat",
        "검은 고양이",
        Rarity.COMMON,
        "빛나는 검은 털로 신비로움을 풍기는 고양이입니다."
    ),

    /** Rare 레어 */
    XMAS_CAT(
        "xmas_cat",
        "크리스마스 고양이",
        Rarity.RARE,
        "크리스마스를 좋아하고 산타를 보고싶어하는 고양이입니다."
    ),
    TIGER_CAT(
        "tiger_cat",
        "호랑이 고양이",
        Rarity.RARE,
        "호랑이 무늬를 닮은 용감하고 활기찬 고양이입니다."
    ),
    THREE_CAT(
        "three_cat",
        "삼색 고양이",
        Rarity.RARE,
        "세 가지 색의 털을 가진 행운의 상징 같은 고양이입니다."
    ),

    /** Epic 에픽 */
    EGYPT_CAT(
        "egypt_cat",
        "이집트 고양이",
        Rarity.EPIC,
        "고대 이집트의 신비로움을 간직한 고양이입니다."
    ),
    BATMAN_CAT(
        "batman_cat",
        "배트맨 고양이",
        Rarity.EPIC,
        "검은 마스크 무늬로 어떤 영웅을 떠올리게 하는 고양이입니다."
    ),

    /** Legendary 전설 */
    DEMONIC_CAT(
        "demonic_cat",
        "악마 고양이",
        Rarity.LEGENDARY,
        "작은 악마를 닮은 장난기 넘치는 고양이입니다."
    );
}
