package com.malrang.pomodoro.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malrang.pomodoro.data.AnimalSprite
import com.malrang.pomodoro.data.Mode
import com.malrang.pomodoro.data.Screen
import com.malrang.pomodoro.data.SpriteState
import com.malrang.pomodoro.viewmodel.PomodoroViewModel
import kotlinx.coroutines.delay

/**
 * 앱의 메인 화면을 표시하는 컴포저블 함수입니다.
 * 타이머, 통계, 네비게이션 버튼을 포함합니다.
 *
 * @param viewModel [PomodoroViewModel]의 인스턴스입니다.
 */
@Composable
fun MainScreen(viewModel: PomodoroViewModel) {
    val state by viewModel.uiState.collectAsState()
    var widthPx by remember { mutableStateOf(0) }
    var heightPx by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF312E81), Color(0xFF6D28D9), Color(0xFFDB2777))
                )
            )
            .onSizeChanged { sz ->
                widthPx = sz.width
                heightPx = sz.height
            }
    ) {
        // 동물 스프라이트
        if (state.currentMode == Mode.BREAK || state.isPaused) {
            state.activeSprites.forEach { sp ->
                SpriteSheetImage(
                    sprite = sp,
                    onJumpFinished = { id -> viewModel.onJumpFinished(id) },
                    modifier = Modifier
                        .absoluteOffset { IntOffset(sp.x.toInt(), sp.y.toInt()) }
                        .size(sp.sizeDp.dp)
                )
            }
        }

        // 이동 루프
        LaunchedEffect(widthPx, heightPx, state.activeSprites.size) {
            if (widthPx == 0 || heightPx == 0) return@LaunchedEffect
            var last = System.nanoTime()
            while (true) {
                withFrameNanos { now ->
                    val dt = (now - last) / 1_000_000_000f
                    last = now
                    viewModel.updateSprites(dt.coerceIn(0f, 0.05f), widthPx, heightPx)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🐾 포모도로 동물원", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("공부하고 동물 친구들을 만나보세요!", color = Color(0xFFDDD6FE))

            Spacer(Modifier.height(16.dp))

            // 원형 타이머
            Box(contentAlignment = Alignment.Center) {
                val totalTime = if (state.currentMode == Mode.STUDY) state.settings.studyTime * 60 else state.settings.breakTime * 60
                val progress = 1f - state.timeLeft.toFloat() / totalTime

                CircularProgressIndicator(
                    progress = progress,
                    strokeWidth = 12.dp,
                    modifier = Modifier.size(200.dp),
                    color = if (state.currentMode == Mode.STUDY) Color(0xFF10B981) else Color(0xFFF59E0B)
                )
                Text(
                    text = "%02d:%02d".format(state.timeLeft / 60, state.timeLeft % 60),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(16.dp))

            Row {
                if (!state.isRunning) {
                    Button(onClick = { viewModel.startTimer() }) { Text("시작") }
                } else {
                    Button(onClick = { viewModel.pauseTimer() }) { Text("일시정지") }
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { viewModel.resetTimer() }) { Text("리셋") }
            }

            Spacer(Modifier.height(24.dp))

            // 통계
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${state.collectedAnimals.size}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Green)
                    Text("수집한 동물", color = Color.LightGray)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${state.totalSessions}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Cyan)
                    Text("완료한 세션", color = Color.LightGray)
                }
            }

            Spacer(Modifier.height(24.dp))

            // 네비게이션
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(onClick = { viewModel.showScreen(Screen.Collection) }) { Text("📚 동물 도감") }
                Button(onClick = { viewModel.showScreen(Screen.Settings) }) { Text("⚙️ 설정") }
                Button(onClick = { viewModel.showScreen(Screen.Stats) }) { Text("📊 통계") }
            }
        }
    }
}

@Composable
fun SpriteSheetImage(
    sprite: AnimalSprite,
    onJumpFinished: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val (res, cols, rows) = when (sprite.spriteState) {
        SpriteState.IDLE -> Triple(sprite.idleSheetRes, sprite.idleCols, sprite.idleRows)
        SpriteState.JUMP -> Triple(sprite.jumpSheetRes, sprite.jumpCols, sprite.jumpRows)
    }

    val image = ImageBitmap.imageResource(id = res)
    val frameWidth = image.width / cols
    val frameHeight = image.height / rows

    var frameIndex by remember(sprite.id, sprite.spriteState) { mutableStateOf(0) }

    LaunchedEffect(sprite.id, sprite.spriteState) {
        frameIndex = 0
        while (true) {
            delay(sprite.frameDurationMs)
            frameIndex++
            if (frameIndex >= cols * rows) {
                if (sprite.spriteState == SpriteState.JUMP) {
                    // Jump 끝 → Idle 복귀
                    onJumpFinished(sprite.id)
                }
                frameIndex = 0
            }
        }
    }

    val col = frameIndex % cols
    val row = frameIndex / cols

    Canvas(modifier = modifier) {
        val dstWidth = size.width.toInt()
        val dstHeight = size.height.toInt()

        if (sprite.vx <= 0) {
            // 왼쪽 이동 → 좌우 반전
            withTransform({
                scale(-1f, 1f, pivot = Offset(size.width / 2f, size.height / 2f))
            }) {
                drawImage(
                    image = image,
                    srcOffset = IntOffset(col * frameWidth, row * frameHeight),
                    srcSize = IntSize(frameWidth, frameHeight),
                    dstSize = IntSize(dstWidth, dstHeight),
                    dstOffset = IntOffset(0, 0),
                    filterQuality = FilterQuality.None
                )
            }
        } else {
            // 오른 이동 → 기본
            drawImage(
                image = image,
                srcOffset = IntOffset(col * frameWidth, row * frameHeight),
                srcSize = IntSize(frameWidth, frameHeight),
                dstSize = IntSize(dstWidth, dstHeight),
                filterQuality = FilterQuality.None
            )
        }
    }
}