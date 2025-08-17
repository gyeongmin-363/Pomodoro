package com.malrang.pomodoro.data

import com.malrang.pomodoro.R

data class SpriteData(
    val idleRes: Int,
    val idleCols: Int = 7,
    val idleRows: Int = 1,
    val jumpRes: Int = 13,
    val jumpCols: Int = 13,
    val jumpRows: Int = 1,
)
object SpriteMap {
    val map: Map<String, SpriteData> = mapOf(
        "cat" to SpriteData(idleRes = R.drawable.idle_catttt, jumpRes = R.drawable.jump_cat),
//        TODO()
//        "dog" to SpriteData(R.drawable.sheet_dog, 6, 1),
//        "rabbit" to SpriteData(R.drawable.pixel_rabbit, 1, 1), // 예시
//        "hamster" to SpriteData(R.drawable.pixel_hamster, 1, 1), // 예시
//        "panda" to SpriteData(R.drawable.pixel_panda, 1, 1), // 예시
//        "koala" to SpriteData(R.drawable.pixel_koala, 1, 1), // 예시
//        "penguin" to SpriteData(R.drawable.pixel_penguin, 1, 1), // 예시
//        "fox" to SpriteData(R.drawable.pixel_fox, 1, 1), // 예시
//        "lion" to SpriteData(R.drawable.pixel_lion, 1, 1), // 예시
//        "tiger" to SpriteData(R.drawable.pixel_tiger, 1, 1), // 예시
//        "wolf" to SpriteData(R.drawable.pixel_wolf, 1, 1), // 예시
//        "eagle" to SpriteData(R.drawable.pixel_eagle, 1, 1), // 예시
//        "unicorn" to SpriteData(R.drawable.pixel_unicorn, 1, 1), // 예시
//        "dragon" to SpriteData(R.drawable.pixel_dragon, 1, 1), // 예시
//        "phoenix" to SpriteData(R.drawable.pixel_phoenix, 1, 1), // 예시
//        "griffin" to SpriteData(R.drawable.pixel_griffin, 1, 1) // 예시
    )
}
