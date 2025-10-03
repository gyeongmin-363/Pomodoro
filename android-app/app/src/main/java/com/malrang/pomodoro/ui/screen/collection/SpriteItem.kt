package com.malrang.pomodoro.ui.screen.collection

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.malrang.pomodoro.dataclass.animalInfo.Animal
import com.malrang.pomodoro.dataclass.sprite.AnimalSprite
import com.malrang.pomodoro.dataclass.sprite.SpriteMap
import com.malrang.pomodoro.ui.screen.main.SpriteSheetImage

@Composable
fun SpriteItem(animal: Animal, size: Float) {
    val spriteData = SpriteMap.map[animal]
    if (spriteData == null) return
    val tempSprite = remember(animal.id) {
        AnimalSprite(
            id = animal.id + "-collection",
            animalId = animal.id,
            idleSheetRes = spriteData.idleRes,
            idleCols = spriteData.idleCols,
            idleRows = spriteData.idleRows,
            jumpSheetRes = spriteData.jumpRes,
            jumpCols = spriteData.jumpCols,
            jumpRows = spriteData.jumpRows,
            x = 0f,
            y = 0f,
            vx = 0f,
            vy = 0f,
            sizeDp = size
        )
    }

    SpriteSheetImage(
        sprite = tempSprite,
        modifier = Modifier.size(size.dp)
    )
}
