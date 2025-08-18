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
//    val emoji: String,     // 이건 도감/텍스트용. 실제 스프라이트는 drawable 매핑 사용
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
    private val common = listOf(
        Animal("cat", "고양이", Rarity.COMMON),

    )
    private val rare = listOf(
        Animal("cat", "고양이", Rarity.COMMON),

    )
    private val epic = listOf(
        Animal("cat", "고양이",  Rarity.COMMON),

    )
    private val legendary = listOf(
        Animal("cat", "고양이",  Rarity.COMMON),
        )

    fun byId(id: String): Animal? = (common + rare + epic + legendary).find { it.id == id }

    fun randomByRarity(r: Rarity): Animal = when (r) {
        Rarity.COMMON -> common.random()
        Rarity.RARE -> rare.random()
        Rarity.EPIC -> epic.random()
        Rarity.LEGENDARY -> legendary.random()
    }
}


enum class AnimalId(val id: String) {
    WHITE_CAT("white_cat"),
    XMAS_CAT("xmas_cat"),
    TIGER_CAT("tiger_cat"),
    THREE_CAT("three_cat"),
    SIAMESE_CAT("siamese_cat"),
    EGYPT_CAT("egypt_cat"),
    DEMONIC_CAT("demonic_cat"),
    CLASSICAL_CAT("classical_cat"),
    BROWN_CAT("brown_cat"),
    BLACK_CAT("black_cat"),
    BATMAN_CAT("batman_cat");

    companion object {
        private val map = entries.associateBy(AnimalId::id)
        fun from(id: String): AnimalId? = map[id]
    }

}
