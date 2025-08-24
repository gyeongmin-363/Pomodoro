package com.malrang.pomodoro.ui.screen

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.malrang.pomodoro.R
import com.malrang.pomodoro.dataclass.sprite.AnimalSprite
import com.malrang.pomodoro.dataclass.sprite.SpriteState
import com.malrang.pomodoro.dataclass.ui.Mode
import com.malrang.pomodoro.dataclass.ui.Screen
import com.malrang.pomodoro.dataclass.ui.WorkPreset
import com.malrang.pomodoro.viewmodel.PomodoroViewModel
import kotlinx.coroutines.delay

@Composable
fun MainScreen(viewModel: PomodoroViewModel) {
    val state by viewModel.uiState.collectAsState()
    var widthPx by remember { mutableStateOf(0) }
    var heightPx by remember { mutableStateOf(0) }
    val context = LocalContext.current

    var showWorkManager by remember { mutableStateOf(false) }
    var presetToRename by remember { mutableStateOf<WorkPreset?>(null) }
    var presetToDelete by remember { mutableStateOf<WorkPreset?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }

    // 이름 변경 다이얼로그
    if (presetToRename != null) {
        RenamePresetDialog(
            preset = presetToRename!!,
            onConfirm = { newName ->
                viewModel.updateWorkPresetName(presetToRename!!.id, newName)
                presetToRename = null
            },
            onDismiss = { presetToRename = null }
        )
    }

    // --- ▼▼▼ 추가된 Composable: 삭제 확인 다이얼로그 ▼▼▼ ---
    if (presetToDelete != null) {
        DeleteConfirmDialog(
            preset = presetToDelete!!,
            onConfirm = {
                viewModel.deleteWorkPreset(presetToDelete!!.id)
                presetToDelete = null
            },
            onDismiss = { presetToDelete = null }
        )
    }
    // --- ▲▲▲ 추가된 Composable ▲▲▲ ---

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { sz ->
                widthPx = sz.width
                heightPx = sz.height
            }
    ) {
        // ✅ 리셋 확인 다이얼로그
        if (showResetConfirm) {
            AlertDialog(
                onDismissRequest = { showResetConfirm = false },
                title = { Text("리셋 확인") },
                text = { Text("정말 리셋할 건가요?\n세션과 공부시간 등이 모두 초기화됩니다.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.reset()   // ⬅️ 새 함수 호출
                            showResetConfirm = false
                        }
                    ) { Text("확인") }
                },
                dismissButton = {
                    TextButton(onClick = { showResetConfirm = false }) {
                        Text("취소")
                    }
                }
            )
        }

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

            val currentWorkName = state.workPresets.find { it.id == state.currentWorkId }?.name ?: "기본"

            TextButton(onClick = { showWorkManager = !showWorkManager }) {
                Text(currentWorkName, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Work 선택")
            }

            AnimatedVisibility(visible = showWorkManager) {
                WorkPresetsManager(
                    presets = state.workPresets,
                    currentPresetId = state.currentWorkId,
                    onPresetSelected = { viewModel.selectWorkPreset(it) },
                    onAddPreset = { viewModel.addWorkPreset() },
                    // --- ▼▼▼ 수정된 부분: 삭제 요청 시 상태 변경 ▼▼▼ ---
                    onDeletePreset = { preset -> presetToDelete = preset },
                    // --- ▲▲▲ 수정된 부분 ▲▲▲ ---
                    onRenamePreset = { preset -> presetToRename = preset },
                    onEditSettings = { presetId ->
                        viewModel.startEditingWorkPreset(presetId)
                        viewModel.showScreen(Screen.Settings)
                    }
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "%02d:%02d".format(state.timeLeft / 60, state.timeLeft % 60),
                fontSize = 60.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
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
                IconButton(onClick = { showResetConfirm = true }) {
                    Icon(painterResource(id = R.drawable.ic_reset), contentDescription = "리셋")
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { viewModel.skipSession() }) {   // ✅ 건너뛰기 버튼
                    Icon(painterResource(id = R.drawable.ic_settings), contentDescription = "건너뛰기")
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
                IconButton(onClick = { viewModel.showScreen(Screen.Stats) }) {
                    Icon(painterResource(id = R.drawable.ic_stats), contentDescription = "통계")
                }
            }
        }
    }
}

@Composable
fun WorkPresetsManager(
    presets: List<WorkPreset>,
    currentPresetId: String?,
    onPresetSelected: (String) -> Unit,
    onAddPreset: () -> Unit,
    onDeletePreset: (WorkPreset) -> Unit,
    onRenamePreset: (WorkPreset) -> Unit,
    onEditSettings: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column {
            LazyColumn(
                modifier = Modifier.heightIn(max = 200.dp)
            ) {
                items(presets) { preset ->
                    WorkPresetItem(
                        preset = preset,
                        isSelected = preset.id == currentPresetId,
                        onSelect = { onPresetSelected(preset.id) },
                        onRename = { onRenamePreset(preset) },
                        onEditSettings = { onEditSettings(preset.id) },
                        onDelete = { onDeletePreset(preset) }
                    )
                }
            }
            Divider()
            TextButton(
                onClick = onAddPreset,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = "Work 추가")
                Spacer(Modifier.width(4.dp))
                Text("새 Work 추가")
            }
        }
    }
}

@Composable
fun WorkPresetItem(
    preset: WorkPreset,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onRename: () -> Unit,
    onEditSettings: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelect
        )
        Text(
            text = preset.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onRename) {
            Icon(Icons.Default.Edit, contentDescription = "이름 변경", tint = MaterialTheme.colorScheme.primary)
        }
        IconButton(onClick = onEditSettings) {
            Icon(Icons.Default.Settings, contentDescription = "설정 변경", tint = MaterialTheme.colorScheme.secondary)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "삭제", tint = MaterialTheme.colorScheme.error)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenamePresetDialog(
    preset: WorkPreset,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(preset.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Work 이름 변경") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("이름") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank()
            ) {
                Text("확인")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

// --- ▼▼▼ 추가된 Composable: 삭제 확인창 ▼▼▼ ---
@Composable
fun DeleteConfirmDialog(
    preset: WorkPreset,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Work 삭제") },
        text = {
            Text(
                buildAnnotatedString {
                    append("정말로 '")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(preset.name)
                    }
                    append("' Work를 삭제하시겠습니까?")
                }
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("삭제")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
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
    val currentIndex = remember(currentMode, totalSessions, longBreakInterval) {
        val cyclePosition = (totalSessions - 1).coerceAtLeast(0) % longBreakInterval
        when (currentMode) {
            Mode.STUDY -> (totalSessions % longBreakInterval) * 2
            else -> cyclePosition * 2 + 1
        }
    }
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
                                index < currentIndex -> drawCircle(color = color)
                                else -> drawCircle(color = color, style = Stroke(width = 2.dp.toPx()))
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