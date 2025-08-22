package com.malrang.pomodoro.ui.screen

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.malrang.pomodoro.R
import com.malrang.pomodoro.dataclass.sprite.AnimalSprite
import com.malrang.pomodoro.dataclass.sprite.SpriteState
import com.malrang.pomodoro.dataclass.ui.Mode
import com.malrang.pomodoro.dataclass.ui.Screen
import com.malrang.pomodoro.dataclass.ui.WorkPreset // --- import 추가 ---
import com.malrang.pomodoro.viewmodel.PomodoroViewModel
import kotlinx.coroutines.delay

/**
 * 앱의 메인 화면을 표시하는 컴포저블 함수입니다.
 */
@Composable
fun MainScreen(viewModel: PomodoroViewModel) {
    val state by viewModel.uiState.collectAsState()
    var widthPx by remember { mutableStateOf(0) }
    var heightPx by remember { mutableStateOf(0) }
    val context = LocalContext.current

    // --- ▼▼▼ 추가된 부분 ▼▼▼ ---
    var showWorkSelectionModal by remember { mutableStateOf(false) }

    if (showWorkSelectionModal) {
        WorkSelectionModal(
            presets = state.workPresets,
            currentPresetId = state.currentWorkId,
            onPresetSelected = { presetId ->
                viewModel.selectWorkPreset(presetId)
                showWorkSelectionModal = false
            },
            onDismiss = { showWorkSelectionModal = false }
        )
    }
    // --- ▲▲▲ 추가된 부분 ▲▲▲ ---

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { sz ->
                widthPx = sz.width
                heightPx = sz.height
            }
    ) {
        // ... (배경 이미지, 동물 스프라이트, 이동 루프는 기존과 동일)
        Image(
            painter = painterResource(id = R.drawable.grass_background),
            contentDescription = "grass background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
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

            // --- ▼▼▼ 수정/추가된 부분 ▼▼▼ ---
            val currentWorkName = state.workPresets.find { it.id == state.currentWorkId }?.name ?: "기본"

            TextButton(onClick = { showWorkSelectionModal = true }) {
                Text(currentWorkName, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Work 선택")
            }
            // --- ▲▲▲ 수정/추가된 부분 ▲▲▲ ---

            // 원형 타이머
            Text(
                text = "%02d:%02d".format(state.timeLeft / 60, state.timeLeft % 60),
                fontSize = 60.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(16.dp))

            // ... (CycleIndicator, 버튼, 네비게이션 등 나머지 UI는 기존과 동일)
            CycleIndicator(
                modifier = Modifier.fillMaxWidth(),
                currentMode = state.currentMode,
                totalSessions = state.totalSessions,
                longBreakInterval = state.settings.longBreakInterval
            )
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
            Text(
                buildAnnotatedString {
                    withStyle(style = SpanStyle(color = Color.LightGray)) { append("연속 완료 세션 : ") }
                    withStyle(style = SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Cyan)) { append("${state.totalSessions} ") }
                }
            )
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                IconButton(onClick = { viewModel.showScreen(Screen.Collection) }) {
                    Icon(painterResource(id = R.drawable.ic_collection), contentDescription = "동물 도감")
                }
                IconButton(onClick = {
                    if (state.isTimerStartedOnce) {
                        Toast.makeText(context, "변경사항은 리셋 이후 적용됩니다", Toast.LENGTH_SHORT).show()
                    }
                    viewModel.showScreen(Screen.Settings)

                }) {
                    Icon(painterResource(id = R.drawable.ic_settings), contentDescription = "설정")
                }
                IconButton(onClick = { viewModel.showScreen(Screen.Stats) }) {
                    Icon(painterResource(id = R.drawable.ic_stats), contentDescription = "통계")
                }
            }
        }
    }
}

// --- ▼▼▼ 추가된 Composable ▼▼▼ ---
/**
 * Work 프리셋을 선택하는 모달창 Composable
 */
@Composable
fun WorkSelectionModal(
    presets: List<WorkPreset>,
    currentPresetId: String?,
    onPresetSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Work 선택",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                LazyColumn {
                    items(presets) { preset ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onPresetSelected(preset.id) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (preset.id == currentPresetId),
                                onClick = { onPresetSelected(preset.id) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = preset.name, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("닫기")
                    }
                }
            }
        }
    }
}
// --- ▲▲▲ 추가된 Composable ▲▲▲ ---

// ... CycleIndicator, SpriteSheetImage 함수는 기존과 동일 ...
@Composable
fun CycleIndicator(
    modifier: Modifier = Modifier,
    currentMode: Mode,
    totalSessions: Int,
    longBreakInterval: Int
) {
    if (longBreakInterval <= 0) return

    // 1. 전체 사이클 시퀀스 생성
    val cycleSequence = remember(longBreakInterval) {
        buildList {
            for (i in 1 until longBreakInterval) {
                add(Mode.STUDY)
                add(Mode.SHORT_BREAK)
            }
            add(Mode.STUDY)
            add(Mode.LONG_BREAK)
        }
    }

    // 2. 현재 세션 인덱스 계산
    val currentIndex = remember(currentMode, totalSessions, longBreakInterval) {
        val cyclePosition = (totalSessions - 1).coerceAtLeast(0) % longBreakInterval
        when (currentMode) {
            Mode.STUDY -> (totalSessions % longBreakInterval) * 2
            else -> cyclePosition * 2 + 1
        }
    }

    // 3. 시퀀스를 8개씩 묶어 여러 행으로 그림
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        cycleSequence.withIndex().chunked(8).forEach { rowItems ->
            Row(
                modifier = Modifier.padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                rowItems.forEach { (index, mode) ->
                    val color = when (mode) {
                        Mode.STUDY -> Color.Red
                        Mode.SHORT_BREAK -> Color.Green
                        Mode.LONG_BREAK -> Color.Blue
                    }

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(16.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            when {
                                // 완료된 세션: 채워진 원
                                index < currentIndex -> {
                                    drawCircle(color = color)
                                }
                                // 현재 또는 미래 세션: 테두리 원
                                else -> {
                                    drawCircle(color = color, style = Stroke(width = 2.dp.toPx()))
                                }
                            }
                        }
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