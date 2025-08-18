package com.malrang.pomodoro.dataclass.sprite

import com.malrang.pomodoro.R
import com.malrang.pomodoro.dataclass.animalInfo.Animal

object SpriteMap {
    val map: Map<Animal, SpriteData> = mapOf(
        Animal.CLASSICAL_CAT to SpriteData(idleRes = R.drawable.classical_idle, jumpRes = R.drawable.classical_jump),
        Animal.WHITE_CAT to SpriteData(idleRes = R.drawable.white_idle, jumpRes = R.drawable.white_jump),
        Animal.XMAS_CAT to SpriteData(idleRes = R.drawable.xmas_idle, jumpRes = R.drawable.xmas_jump),
        Animal.TIGER_CAT to SpriteData(idleRes = R.drawable.tiger_cat_idle, jumpRes = R.drawable.tiger_cat_jump),
        Animal.THREE_CAT to SpriteData(idleRes = R.drawable.three_color_idle, jumpRes = R.drawable.three_color_jump),
        Animal.SIAMESE_CAT to SpriteData(idleRes = R.drawable.siamese_idle, jumpRes = R.drawable.siamese_jump),
        Animal.EGYPT_CAT to SpriteData(idleRes = R.drawable.egypt_cat_idle, jumpRes = R.drawable.egypt_cat_jump),
        Animal.DEMONIC_CAT to SpriteData(idleRes = R.drawable.demonic_idle, jumpRes = R.drawable.demonic_jump),
        Animal.BROWN_CAT to SpriteData(idleRes = R.drawable.brown_idle, jumpRes = R.drawable.brown_jump),
        Animal.BLACK_CAT to SpriteData(idleRes = R.drawable.black_cat_idle, jumpRes = R.drawable.black_cat_jump),
        Animal.BATMAN_CAT to SpriteData(idleRes = R.drawable.batman_cat_idle, jumpRes = R.drawable.batman_cat_jump),
    )
}
