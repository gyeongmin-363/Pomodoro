package com.malrang.pomodoro.data


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

enum class SpriteState { IDLE, JUMP }

// 스프라이트 1개 (세션 생명주기: 앱 재시작 시 사라짐)
data class AnimalSprite(
    val id: String,                  // 고유 ID (중복 생성 방지 위해 UUID 등)
    val animalId: String,            // 동물 종류
    val idleSheetRes: Int,
    val idleCols: Int,
    val idleRows: Int,
    val jumpSheetRes: Int,
    val jumpCols: Int,
    val jumpRows: Int,
    val spriteState: SpriteState = SpriteState.IDLE,
    val frameDurationMs: Long = 120L,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val sizeDp: Float
)



object AnimalsTable {
    // 희귀도별로 Animal enum class의 멤버들을 필터링하여 목록을 생성합니다.
    private val common = Animal.entries.filter { it.rarity == Rarity.COMMON }
    private val rare = Animal.entries.filter { it.rarity == Rarity.RARE }
    private val epic = Animal.entries.filter { it.rarity == Rarity.EPIC }
    private val legendary = Animal.entries.filter { it.rarity == Rarity.LEGENDARY }

    // 모든 동물을 포함하는 목록입니다.
    private val allAnimals = Animal.entries

    fun byId(id: String): Animal? = allAnimals.find { it.id == id }

    fun randomByRarity(r: Rarity): Animal = when (r) {
        Rarity.COMMON -> common.random()
        Rarity.RARE -> rare.random()
        Rarity.EPIC -> epic.random()
        Rarity.LEGENDARY -> legendary.random()
    }
}


//enum class AnimalId(val id: String) {
//    WHITE_CAT("white_cat"),
//    XMAS_CAT("xmas_cat"),
//    TIGER_CAT("tiger_cat"),
//    THREE_CAT("three_cat"),
//    SIAMESE_CAT("siamese_cat"),
//    EGYPT_CAT("egypt_cat"),
//    DEMONIC_CAT("demonic_cat"),
//    CLASSICAL_CAT("classical_cat"),
//    BROWN_CAT("brown_cat"),
//    BLACK_CAT("black_cat"),
//    BATMAN_CAT("batman_cat");
//
//    companion object {
//        private val map = entries.associateBy(AnimalId::id)
//        fun from(id: String): AnimalId? = map[id]
//    }
//
//}
