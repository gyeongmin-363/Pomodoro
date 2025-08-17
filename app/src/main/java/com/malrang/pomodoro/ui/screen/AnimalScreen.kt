//package com.malrang.pomodoro.ui.screen
//
//import androidx.compose.foundation.Canvas
//import androidx.compose.foundation.Image
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.absoluteOffset
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.size
//import androidx.compose.material3.Button
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.collectAsState
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.setValue
//import androidx.compose.runtime.withFrameNanos
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.geometry.Offset
//import androidx.compose.ui.graphics.Brush
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.FilterQuality
//import androidx.compose.ui.graphics.ImageBitmap
//import androidx.compose.ui.graphics.drawscope.scale
//import androidx.compose.ui.graphics.drawscope.withTransform
//import androidx.compose.ui.layout.onSizeChanged
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.platform.LocalDensity
//import androidx.compose.ui.res.imageResource
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.IntOffset
//import androidx.compose.ui.unit.IntSize
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import com.malrang.pomodoro.data.AnimalSprite
//import com.malrang.pomodoro.data.Screen
//import com.malrang.pomodoro.data.SpriteState
//import com.malrang.pomodoro.viewmodel.PomodoroViewModel
//import kotlinx.coroutines.delay
//
///**
// * 새로운 동물을 획득했을 때 표시되는 화면입니다.
// *
// * @param viewModel [PomodoroViewModel]의 인스턴스입니다.
// */
//@Composable
//fun AnimalScreen(vm: PomodoroViewModel) {
//    val state by vm.uiState.collectAsState()
//    val density = LocalDensity.current
//
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(Brush.verticalGradient(listOf(Color(0xFF6D28D9), Color(0xFFDB2777))))
//    ) {
//        var widthPx by remember { mutableStateOf(0) }
//        var heightPx by remember { mutableStateOf(0) }
//
//        Box(
//            Modifier
//                .fillMaxSize()
//                .onSizeChanged { sz ->
//                    widthPx = sz.width
//                    heightPx = sz.height
//                }
//        ) {
//            state.activeSprites.forEach { sp ->
//                SpriteSheetImage(
//                    sprite = sp,
//                    onJumpFinished = { id -> vm.onJumpFinished(id) },
//                    modifier = Modifier
//                        .absoluteOffset { IntOffset(sp.x.toInt(), sp.y.toInt()) }
//                        .size(sp.sizeDp.dp)
//                )
//            }
//        }
//
//        // 이동 루프
//        LaunchedEffect(widthPx, heightPx, state.activeSprites.size) {
//            if (widthPx == 0 || heightPx == 0) return@LaunchedEffect
//            var last = System.nanoTime()
//            while (true) {
//                withFrameNanos { now ->
//                    val dt = (now - last) / 1_000_000_000f
//                    last = now
//                    vm.updateSprites(dt.coerceIn(0f, 0.05f), widthPx, heightPx)
//                }
//            }
//        }
//
//        Button(
//            onClick = { vm.showScreen(Screen.Main) },
//            modifier = Modifier
//                .align(Alignment.BottomCenter)
//                .padding(24.dp)
//        ) {
//            Text("계속하기")
//        }
//    }
//}
//
//
//
//@Composable
//fun SpriteSheetImage(
//    sprite: AnimalSprite,
//    onJumpFinished: (String) -> Unit,
//    modifier: Modifier = Modifier
//) {
//    val (res, cols, rows) = when (sprite.spriteState) {
//        SpriteState.IDLE -> Triple(sprite.idleSheetRes, sprite.idleCols, sprite.idleRows)
//        SpriteState.JUMP -> Triple(sprite.jumpSheetRes, sprite.jumpCols, sprite.jumpRows)
//    }
//
//    val image = ImageBitmap.imageResource(id = res)
//    val frameWidth = image.width / cols
//    val frameHeight = image.height / rows
//
//    var frameIndex by remember(sprite.id, sprite.spriteState) { mutableStateOf(0) }
//
//    LaunchedEffect(sprite.id, sprite.spriteState) {
//        frameIndex = 0
//        while (true) {
//            delay(sprite.frameDurationMs)
//            frameIndex++
//            if (frameIndex >= cols * rows) {
//                if (sprite.spriteState == SpriteState.JUMP) {
//                    // Jump 끝 → Idle 복귀
//                    onJumpFinished(sprite.id)
//                }
//                frameIndex = 0
//            }
//        }
//    }
//
//    val col = frameIndex % cols
//    val row = frameIndex / cols
//
//    Canvas(modifier = modifier) {
//        val dstWidth = size.width.toInt()
//        val dstHeight = size.height.toInt()
//
//        if (sprite.vx <= 0) {
//            // 왼쪽 이동 → 좌우 반전
//            withTransform({
//                scale(-1f, 1f, pivot = Offset(size.width / 2f, size.height / 2f))
//            }) {
//                drawImage(
//                    image = image,
//                    srcOffset = IntOffset(col * frameWidth, row * frameHeight),
//                    srcSize = IntSize(frameWidth, frameHeight),
//                    dstSize = IntSize(dstWidth, dstHeight),
//                    dstOffset = IntOffset(0, 0)
//                )
//            }
//        } else {
//            // 오른 이동 → 기본
//            drawImage(
//                image = image,
//                srcOffset = IntOffset(col * frameWidth, row * frameHeight),
//                srcSize = IntSize(frameWidth, frameHeight),
//                dstSize = IntSize(dstWidth, dstHeight),
//                dstOffset = IntOffset(0, 0)
//            )
//        }
//    }
//}
