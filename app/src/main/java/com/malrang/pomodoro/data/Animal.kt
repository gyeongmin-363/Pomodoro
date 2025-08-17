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
    val emoji: String,     // 이건 도감/텍스트용. 실제 스프라이트는 drawable 매핑 사용
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


// 스프라이트 1개 (세션 생명주기: 앱 재시작 시 사라짐)
data class AnimalSprite(
    val animalId: String,
    val sheetRes: Int,
    val frameCols: Int,
    val frameRows: Int,
    val frameDurationMs: Long = 120L, // 프레임 전환 간격
    var currentFrame: Int = 0,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val sizeDp: Float
)



object AnimalsTable {
    private val common = listOf(
        Animal("cat", "고양이", "🐱", Rarity.COMMON),
//        Animal("dog", "강아지", "🐶", Rarity.COMMON),
//        Animal("rabbit", "토끼", "🐰", Rarity.COMMON),
//        Animal("hamster", "햄스터", "🐹", Rarity.COMMON)
    )
    private val rare = listOf(
        Animal("cat", "고양이", "🐱", Rarity.COMMON),

//        Animal("panda", "팬더", "🐼", Rarity.RARE),
//        Animal("koala", "코알라", "🐨", Rarity.RARE),
//        Animal("penguin", "펭귄", "🐧", Rarity.RARE),
//        Animal("fox", "여우", "🦊", Rarity.RARE)
    )
    private val epic = listOf(
        Animal("cat", "고양이", "🐱", Rarity.COMMON),

//        Animal("lion", "사자", "🦁", Rarity.EPIC),
//        Animal("tiger", "호랑이", "🐅", Rarity.EPIC),
//        Animal("wolf", "늑대", "🐺", Rarity.EPIC),
//        Animal("eagle", "독수리", "🦅", Rarity.EPIC)
    )
    private val legendary = listOf(
        Animal("cat", "고양이", "🐱", Rarity.COMMON),

//        Animal("unicorn", "유니콘", "🦄", Rarity.LEGENDARY),
//        Animal("dragon", "드래곤", "🐉", Rarity.LEGENDARY),
//        Animal("phoenix", "피닉스", "🔥🐦", Rarity.LEGENDARY),
//        Animal("griffin", "그리핀", "🦅🦁", Rarity.LEGENDARY)
    )

    fun byId(id: String): Animal? = (common + rare + epic + legendary).find { it.id == id }

    fun randomByRarity(r: Rarity): Animal = when (r) {
        Rarity.COMMON -> common.random()
        Rarity.RARE -> rare.random()
        Rarity.EPIC -> epic.random()
        Rarity.LEGENDARY -> legendary.random()
    }
}
