package com.malrang.pomodoro.dataclass.sprite

data class SpriteData(
    val idleRes: Int,
    val idleCols: Int = 7,
    val idleRows: Int = 1,
    val jumpRes: Int,
    val jumpCols: Int = 13,
    val jumpRows: Int = 1,
)