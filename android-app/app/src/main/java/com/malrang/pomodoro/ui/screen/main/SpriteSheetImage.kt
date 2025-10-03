package com.malrang.pomodoro.ui.screen.main

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.malrang.pomodoro.dataclass.sprite.AnimalSprite
import kotlinx.coroutines.delay

@Composable
fun SpriteSheetImage(
    sprite: AnimalSprite,
    modifier: Modifier = Modifier
) {
    val (res, cols, rows) = Triple(sprite.idleSheetRes, sprite.idleCols, sprite.idleRows)

    val image = ImageBitmap.imageResource(id = res)
    val frameWidth = image.width / cols
    val frameHeight = image.height / rows

    var frameIndex by remember(sprite.id) { mutableStateOf(0) }

    val col = frameIndex % cols
    val row = frameIndex / cols

    Canvas(modifier = modifier) {
        val dstWidth = size.width.toInt()
        val dstHeight = size.height.toInt()

        drawImage(
            image = image,
            srcOffset = IntOffset(col * frameWidth, row * frameHeight),
            srcSize = IntSize(frameWidth, frameHeight),
            dstSize = IntSize(dstWidth, dstHeight),
            dstOffset = IntOffset(0, 0),
            filterQuality = FilterQuality.None
        )
    }
}