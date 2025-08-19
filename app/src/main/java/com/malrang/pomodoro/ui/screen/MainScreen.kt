package com.malrang.pomodoro.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malrang.pomodoro.R
import com.malrang.pomodoro.dataclass.sprite.AnimalSprite
import com.malrang.pomodoro.dataclass.sprite.SpriteState
import com.malrang.pomodoro.dataclass.ui.Mode
import com.malrang.pomodoro.dataclass.ui.Screen
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
            .onSizeChanged { sz ->
                widthPx = sz.width
                heightPx = sz.height
            }
    ) {
        Image(
            painter = painterResource(id = R.drawable.grass_background),
            contentDescription = "grass background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // 동물 스프라이트
        if (state.currentMode != Mode.STUDY || state.isPaused) {
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
            Text("픽모도로", fontSize = 28.sp, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(16.dp))

            // 원형 타이머
            Text(
                text = "%02d:%02d".format(state.timeLeft / 60, state.timeLeft % 60),
                fontSize = 60.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(16.dp))

            // ==================================
            //      새로운 사이클 표시기
            // ==================================
            CycleIndicator(
                modifier = Modifier.fillMaxWidth(),
                totalSessions = state.totalSessions,
                longBreakInterval = state.settings.longBreakInterval
            )
            // ==================================

            Spacer(Modifier.height(16.dp))

            Row {
                if (!state.isRunning) {
                    IconButton(onClick = { viewModel.startTimer() }) {
                        Icon(painterResource(id = R.drawable.ic_play), contentDescription = "시작")
                    }
                } else {
                    IconButton(onClick = { viewModel.pauseTimer() }) {
                        Icon(painterResource(id = R.drawable.ic_pause), contentDescription = "일시정지")
                    }
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { viewModel.resetTimer() }) {
                    Icon(painterResource(id = R.drawable.ic_reset), contentDescription = "리셋")
                }
            }

            Spacer(Modifier.height(24.dp))

            // 통계
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${state.totalSessions}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Cyan)
                    Text("완료한 세션", color = Color.LightGray)
                }
            }

            Spacer(Modifier.height(24.dp))

            // 네비게이션
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                IconButton(onClick = { viewModel.showScreen(Screen.Collection) }) {
                    Icon(painterResource(id = R.drawable.ic_collection), contentDescription = "동물 도감")
                }
                IconButton(onClick = { viewModel.showScreen(Screen.Settings) }) {
                    Icon(painterResource(id = R.drawable.ic_settings), contentDescription = "설정")
                }
                IconButton(onClick = { viewModel.showScreen(Screen.Stats) }) {
                    Icon(painterResource(id = R.drawable.ic_stats), contentDescription = "통계")
                }
            }
        }
    }
}

/**
 * 현재 뽀모도로 사이클 진행도를 원으로 표시하는 컴포저블
 */
@Composable
fun CycleIndicator(
    modifier: Modifier = Modifier,
    totalSessions: Int,
    longBreakInterval: Int
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (longBreakInterval <= 0) return@Row

        val completedInCycle = totalSessions % longBreakInterval

        // 현재 사이클에서 완료된 세션 수. 긴 휴식 직전에는 모든 원이 채워짐.
        val sessionsToShow = if (totalSessions > 0 && completedInCycle == 0) {
            longBreakInterval
        } else {
            completedInCycle
        }

        for (i in 1..longBreakInterval) {
            val isCompleted = i <= sessionsToShow
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(16.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (isCompleted) {
                        // 완료된 세션: 채워진 원
                        drawCircle(color = Color.Cyan)
                    } else {
                        // 남은 세션: 테두리 원
                        drawCircle(color = Color.LightGray, style = Stroke(width = 2.dp.toPx()))
                    }
                }
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
