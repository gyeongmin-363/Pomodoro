package com.malrang.pomodoro.ui.screen

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.malrang.pomodoro.dataclass.ui.PomodoroUiState
import com.malrang.pomodoro.dataclass.ui.Screen
import com.malrang.pomodoro.dataclass.ui.WorkPreset
import com.malrang.pomodoro.ui.PixelArtConfirmDialog
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
    var newPresetName by remember { mutableStateOf("") }
    var presetToDelete by remember { mutableStateOf<WorkPreset?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var showSkipConfirm by remember { mutableStateOf(false) }

    // 배경에 따른 컨텐츠 색상 결정
    val contentColor = if (state.useGrassBackground) Color.Black else Color.White
    val secondaryTextColor = Color.LightGray
    val highlightColor = if (state.useGrassBackground) Color(0xFF01579B) else Color.Cyan // 잔디 배경일 때 더 어두운 파란색
    val textFieldColors = if (state.useGrassBackground) {
        OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.Black,
            unfocusedBorderColor = Color.Gray,
            cursorColor = Color.Black,
            focusedLabelColor = Color.Black,
            unfocusedLabelColor = Color.Gray,
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black
        )
    } else {
        OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.White,
            unfocusedBorderColor = Color.Gray,
            cursorColor = Color.White,
            focusedLabelColor = Color.White,
            unfocusedLabelColor = Color.Gray,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        )
    }

    if (presetToRename != null) {
        PixelArtConfirmDialog(
            onDismissRequest = { presetToRename = null },
            title = "Work 이름 변경",
            confirmText = "확인",
            confirmButtonEnabled = newPresetName.isNotBlank(),
            onConfirm = {
                viewModel.updateWorkPresetName(presetToRename!!.id, newPresetName)
                presetToRename = null
            }
        ) {
            OutlinedTextField(
                value = newPresetName,
                onValueChange = { newPresetName = it },
                label = { Text("새 이름") },
                singleLine = true,
                colors = textFieldColors
            )
        }
    }

    if (presetToDelete != null) {
        PixelArtConfirmDialog(
            onDismissRequest = { presetToDelete = null },
            title = "Work 삭제",
            confirmText = "삭제",
            onConfirm = {
                viewModel.deleteWorkPreset(presetToDelete!!.id)
                presetToDelete = null
            }
        ) {
            Text(
                buildAnnotatedString {
                    append("정말로 '")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color.White)) {
                        append(presetToDelete!!.name)
                    }
                    append("' Work를 삭제하시겠습니까?")
                },
                color = secondaryTextColor
            )
        }
    }

    if (showSkipConfirm) {
        PixelArtConfirmDialog(
            onDismissRequest = { showSkipConfirm = false },
            title = "세션 건너뛰기",
            confirmText = "확인",
            onConfirm = {
                viewModel.skipSession()
                showSkipConfirm = false
            }
        ) {
            Text("현재 세션을 건너뛰시겠습니까?", color = secondaryTextColor)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { sz ->
                widthPx = sz.width
                heightPx = sz.height
            }
    ) {
        if (showResetConfirm) {
            PixelArtConfirmDialog(
                onDismissRequest = { showResetConfirm = false },
                title = "리셋 확인",
                confirmText = "확인",
                onConfirm = {
                    viewModel.reset()
                    showResetConfirm = false
                }
            ) {
                Text("정말 리셋할 건가요?\n세션과 공부시간 등이 모두 초기화됩니다.", color = secondaryTextColor)
            }
        }

        if (state.useGrassBackground) {
            Image(
                painter = painterResource(id = R.drawable.grass_background),
                contentDescription = "grass background",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            )
        }

        if ((state.currentMode != Mode.STUDY || state.isPaused) && state.useGrassBackground) {
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

        val configuration = LocalConfiguration.current
        when (configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                LandscapeMainScreen(
                    state = state,
                    viewModel = viewModel,
                    showWorkManager = showWorkManager,
                    onShowWorkManagerChange = { showWorkManager = it },
                    onPresetToDeleteChange = { presetToDelete = it },
                    onPresetToRenameChange = { preset ->
                        newPresetName = preset.name
                        presetToRename = preset
                    },
                    onShowResetConfirmChange = { showResetConfirm = it },
                    onShowSkipConfirmChange = { showSkipConfirm = it },
                    contentColor = contentColor,
                    secondaryTextColor = secondaryTextColor,
                    highlightColor = highlightColor
                )
            }
            else -> {
                PortraitMainScreen(
                    state = state,
                    viewModel = viewModel,
                    showWorkManager = showWorkManager,
                    onShowWorkManagerChange = { showWorkManager = it },
                    onPresetToDeleteChange = { presetToDelete = it },
                    onPresetToRenameChange = { preset ->
                        newPresetName = preset.name
                        presetToRename = preset
                    },
                    onShowResetConfirmChange = { showResetConfirm = it },
                    onShowSkipConfirmChange = { showSkipConfirm = it },
                    contentColor = contentColor,
                    secondaryTextColor = secondaryTextColor,
                    highlightColor = highlightColor
                )
            }
        }
    }
}

@Composable
fun PortraitMainScreen(
    state: PomodoroUiState,
    viewModel: PomodoroViewModel,
    showWorkManager: Boolean,
    onShowWorkManagerChange: (Boolean) -> Unit,
    onPresetToDeleteChange: (WorkPreset) -> Unit,
    onPresetToRenameChange: (WorkPreset) -> Unit,
    onShowResetConfirmChange: (Boolean) -> Unit,
    onShowSkipConfirmChange: (Boolean) -> Unit,
    contentColor: Color,
    secondaryTextColor: Color,
    highlightColor: Color
) {
    val context = LocalContext.current
    val titleText = when (state.currentMode) {
        Mode.STUDY -> "📖 공부 시간"
        Mode.SHORT_BREAK, Mode.LONG_BREAK -> "☕ 휴식 시간"
    }
    val currentWorkName = state.workPresets.find { it.id == state.currentWorkId }?.name ?: "기본"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        TextButton(onClick = { onShowWorkManagerChange(!showWorkManager) }) {
            Text(currentWorkName, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = contentColor)
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Work 선택", tint = contentColor)
        }

        AnimatedVisibility(visible = showWorkManager) {
            WorkPresetsManager(
                presets = state.workPresets,
                currentPresetId = state.currentWorkId,
                onPresetSelected = { viewModel.selectWorkPreset(it) },
                onAddPreset = { viewModel.addWorkPreset() },
                onDeletePreset = { preset -> onPresetToDeleteChange(preset) },
                onRenamePreset = { preset -> onPresetToRenameChange(preset) },
                onEditSettings = { presetId ->
                    viewModel.startEditingWorkPreset(presetId)
                    viewModel.showScreen(Screen.Settings)
                },
                useGrassBackground = state.useGrassBackground
            )
        }
        Spacer(Modifier.height(16.dp))

        Text(text = titleText, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = contentColor)

        Spacer(Modifier.height(8.dp))
        Text(
            text = "%02d:%02d".format(state.timeLeft / 60, state.timeLeft % 60),
            fontSize = 60.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
        Spacer(Modifier.height(16.dp))
        CycleIndicator(
            modifier = Modifier.fillMaxWidth(),
            currentMode = state.currentMode,
            totalSessions = state.totalSessions,
            longBreakInterval = state.settings.longBreakInterval,
            borderColor = contentColor.copy(alpha = 0.5f),
            itemsPerRow = 8
        )
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!state.isRunning) {
                IconButton(onClick = { viewModel.startTimer() }) {
                    Icon(painterResource(id = R.drawable.ic_play), contentDescription = "시작", tint = contentColor)
                }
            } else {
                IconButton(onClick = { viewModel.pauseTimer() }) {
                    Icon(painterResource(id = R.drawable.ic_pause), contentDescription = "일시정지", tint = contentColor)
                }
            }
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { onShowResetConfirmChange(true) }) {
                Icon(painterResource(id = R.drawable.ic_reset), contentDescription = "리셋", tint = contentColor)
            }
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { onShowSkipConfirmChange(true) }) {
                Icon(painterResource(id = R.drawable.ic_skip), contentDescription = "건너뛰기", tint = contentColor)
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            buildAnnotatedString {
                withStyle(style = SpanStyle(color = secondaryTextColor)) { append("연속 완료 세션 : ") }
                withStyle(style = SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = highlightColor)) { append("${state.totalSessions} ") }
            }
        )
        Spacer(Modifier.height(24.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            IconButton(onClick = { viewModel.showScreen(Screen.Collection) }) {
                Icon(painterResource(id = R.drawable.ic_collection), contentDescription = "동물 도감", tint = contentColor)
            }
            IconButton(onClick = { viewModel.showScreen(Screen.Stats) }) {
                Icon(painterResource(id = R.drawable.ic_stats), contentDescription = "통계", tint = contentColor)
            }
            IconButton(onClick = {
                if (state.useGrassBackground) {
                    Toast.makeText(context, "어두운 배경에서는 동물이 나타나지 않아요.", Toast.LENGTH_SHORT).show()
                }
                viewModel.toggleBackground()
            }) {
                Icon(painterResource(R.drawable.light_night), contentDescription = "배경 변경", tint = contentColor)
            }
        }
    }
}

@Composable
fun LandscapeMainScreen(
    state: PomodoroUiState,
    viewModel: PomodoroViewModel,
    showWorkManager: Boolean,
    onShowWorkManagerChange: (Boolean) -> Unit,
    onPresetToDeleteChange: (WorkPreset) -> Unit,
    onPresetToRenameChange: (WorkPreset) -> Unit,
    onShowResetConfirmChange: (Boolean) -> Unit,
    onShowSkipConfirmChange: (Boolean) -> Unit,
    contentColor: Color,
    secondaryTextColor: Color,
    highlightColor: Color
) {
    val context = LocalContext.current
    val currentWorkName = state.workPresets.find { it.id == state.currentWorkId }?.name ?: "기본"
    val titleText = when (state.currentMode) {
        Mode.STUDY -> "📖 공부 시간"
        Mode.SHORT_BREAK, Mode.LONG_BREAK -> "☕ 휴식 시간"
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 왼쪽: CycleIndicator
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                buildAnnotatedString {
                    withStyle(style = SpanStyle(color = secondaryTextColor)) { append("연속 완료 세션 : ") }
                    withStyle(style = SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = highlightColor)) { append("${state.totalSessions} ") }
                }
            )
            Spacer(Modifier.height(16.dp))
            CycleIndicator(
                modifier = Modifier.fillMaxWidth(),
                currentMode = state.currentMode,
                totalSessions = state.totalSessions,
                longBreakInterval = state.settings.longBreakInterval,
                borderColor = contentColor.copy(alpha = 0.5f),
                itemsPerRow = 6
            )
        }

        // 중앙: 타이머 및 정보
        Column(
            modifier = Modifier.weight(2f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {



            TextButton(onClick = { onShowWorkManagerChange(!showWorkManager) }) {
                Text(currentWorkName, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = contentColor)
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Work 선택", tint = contentColor)
            }

            AnimatedVisibility(visible = showWorkManager) {
                WorkPresetsManager(
                    presets = state.workPresets,
                    currentPresetId = state.currentWorkId,
                    onPresetSelected = { viewModel.selectWorkPreset(it) },
                    onAddPreset = { viewModel.addWorkPreset() },
                    onDeletePreset = { preset -> onPresetToDeleteChange(preset) },
                    onRenamePreset = { preset -> onPresetToRenameChange(preset) },
                    onEditSettings = { presetId ->
                        viewModel.startEditingWorkPreset(presetId)
                        viewModel.showScreen(Screen.Settings)
                    },
                    useGrassBackground = state.useGrassBackground
                )
            }
            Spacer(Modifier.height(16.dp))

            Text(text = titleText, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = contentColor)

            Spacer(Modifier.height(8.dp))
            Text(
                text = "%02d:%02d".format(state.timeLeft / 60, state.timeLeft % 60),
                fontSize = 60.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }

        // 오른쪽: 컨트롤 버튼
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!state.isRunning) {
                    IconButton(onClick = { viewModel.startTimer() }) {
                        Icon(painterResource(id = R.drawable.ic_play), contentDescription = "시작", tint = contentColor)
                    }
                } else {
                    IconButton(onClick = { viewModel.pauseTimer() }) {
                        Icon(painterResource(id = R.drawable.ic_pause), contentDescription = "일시정지", tint = contentColor)
                    }
                }
                IconButton(onClick = { onShowResetConfirmChange(true) }) {
                    Icon(painterResource(id = R.drawable.ic_reset), contentDescription = "리셋", tint = contentColor)
                }
                IconButton(onClick = { onShowSkipConfirmChange(true) }) {
                    Icon(painterResource(id = R.drawable.ic_skip), contentDescription = "건너뛰기", tint = contentColor)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = { viewModel.showScreen(Screen.Collection) }) {
                    Icon(painterResource(id = R.drawable.ic_collection), contentDescription = "동물 도감", tint = contentColor)
                }
                IconButton(onClick = { viewModel.showScreen(Screen.Stats) }) {
                    Icon(painterResource(id = R.drawable.ic_stats), contentDescription = "통계", tint = contentColor)
                }
                IconButton(onClick = {
                    if (state.useGrassBackground) {
                        Toast.makeText(context, "어두운 배경에서는 동물이 나타나지 않아요.", Toast.LENGTH_SHORT).show()
                    }
                    viewModel.toggleBackground()
                }) {
                    Icon(painterResource(R.drawable.light_night), contentDescription = "배경 변경", tint = contentColor)
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
    onEditSettings: (String) -> Unit,
    useGrassBackground: Boolean
) {
    val contentColor = if (useGrassBackground) Color.Black else Color.White

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(2.dp, contentColor),
        shape = RectangleShape,
        colors = CardDefaults.cardColors(
            containerColor = if (useGrassBackground) Color(0x99FFFFFF) else Color(0x992D2A5A)
        )
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
                        onDelete = { onDeletePreset(preset) },
                        useGrassBackground = useGrassBackground
                    )
                }
            }
            Divider(color = contentColor.copy(alpha = 0.5f))
            TextButton(
                onClick = onAddPreset,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = "Work 추가", tint = contentColor)
                Spacer(Modifier.width(4.dp))
                Text("새 Work 추가", color = contentColor)
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
    onDelete: () -> Unit,
    useGrassBackground: Boolean
) {
    val contentColor = if (useGrassBackground) Color.Black else Color.White
    val radioColors = if (useGrassBackground) {
        RadioButtonDefaults.colors(
            selectedColor = Color.Black,
            unselectedColor = Color.Gray
        )
    } else {
        RadioButtonDefaults.colors(
            selectedColor = Color.White,
            unselectedColor = Color.Gray
        )
    }
    val editIconTint = if (useGrassBackground) Color(0xFF0D47A1) else Color.Cyan
    val settingsIconTint = if (useGrassBackground) Color(0xFFF9A825) else Color.Yellow
    val deleteIconTint = if (useGrassBackground) Color(0xFFB71C1C) else Color(0xFFE91E63)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelect,
            colors = radioColors
        )
        Text(
            text = preset.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            color = contentColor
        )
        IconButton(onClick = onRename) {
            Icon(Icons.Default.Edit, contentDescription = "이름 변경", tint = editIconTint)
        }
        IconButton(onClick = onEditSettings) {
            Icon(Icons.Default.Settings, contentDescription = "설정 변경", tint = settingsIconTint)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "삭제", tint = deleteIconTint)
        }
    }
}

@Composable
fun CycleIndicator(
    modifier: Modifier = Modifier,
    currentMode: Mode,
    totalSessions: Int,
    longBreakInterval: Int,
    borderColor: Color,
    itemsPerRow: Int
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
        val cycleLength = longBreakInterval * 2
        if (currentMode == Mode.STUDY && totalSessions > 0 && totalSessions % longBreakInterval == 0) {
            cycleLength
        } else {
            val cyclePosition = (totalSessions - 1).coerceAtLeast(0) % longBreakInterval
            when (currentMode) {
                Mode.STUDY -> (totalSessions % longBreakInterval) * 2
                else -> cyclePosition * 2 + 1
            }
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        cycleSequence.withIndex().chunked(itemsPerRow).forEach { rowItems ->
            Row(
                modifier = Modifier.padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                rowItems.forEach { (index, mode) ->
                    val color = when (mode) {
                        Mode.STUDY -> Color(0xFFC62828)
                        Mode.SHORT_BREAK -> Color(0xFF2E7D32)
                        Mode.LONG_BREAK -> Color(0xFF1565C0)
                    }

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(16.dp)
                            .border(1.dp, borderColor, RectangleShape)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            when {
                                index < currentIndex -> drawRect(color = color)
                                else -> drawRect(color = color.copy(alpha=0.3f))
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