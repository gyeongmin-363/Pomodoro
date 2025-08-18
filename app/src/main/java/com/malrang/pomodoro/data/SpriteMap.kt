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
        "cat" to SpriteData(idleRes = R.drawable.classical_idle, jumpRes = R.drawable.classical_jump),
//        TODO()
    )
}
