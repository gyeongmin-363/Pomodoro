package com.malrang.pomodoro.dataclass.sprite

import com.malrang.pomodoro.R
import com.malrang.pomodoro.dataclass.animalInfo.Animal

object SpriteMap {
    val map: Map<Animal, SpriteData> = mapOf(
        Animal.CLASSICAL_CAT to SpriteData(idleRes = R.drawable.classical_idle, jumpRes = R.drawable.classical_jump),
    )
}
