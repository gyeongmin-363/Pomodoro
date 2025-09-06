package com.malrang.pomodoro.ui.screen.main

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.malrang.pomodoro.R
import com.malrang.pomodoro.dataclass.ui.Mode
import com.malrang.pomodoro.dataclass.ui.Screen
import com.malrang.pomodoro.dataclass.ui.WorkPreset
import com.malrang.pomodoro.ui.PixelArtConfirmDialog
import com.malrang.pomodoro.viewmodel.PomodoroViewModel
import kotlinx.coroutines.launch

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

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                // 스크롤을 위해 Column과 verticalScroll Modifier 추가
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Spacer(Modifier.height(12.dp))
                    NavigationDrawerItem(
                        icon = { Icon(painterResource(id = R.drawable.ic_collection), contentDescription = "동물 도감") },
                        label = { Text("동물 도감") },
                        selected = false,
                        onClick = {
                            viewModel.navigateTo(Screen.Collection)
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    NavigationDrawerItem(
                        icon = { Icon(painterResource(id = R.drawable.ic_stats), contentDescription = "통계") },
                        label = { Text("통계") },
                        selected = false,
                        onClick = {
                            viewModel.navigateTo(Screen.Stats)
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    NavigationDrawerItem(
                        icon = { Icon(painterResource(R.drawable.light_night), contentDescription = "배경 변경") },
                        label = { Text("배경 변경") },
                        selected = false,
                        onClick = {
                            if (state.useGrassBackground) {
                                Toast.makeText(context, "어두운 배경에서는 동물이 나타나지 않아요.", Toast.LENGTH_SHORT).show()
                            }
                            viewModel.toggleBackground()
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.ThumbUp, contentDescription = "스터디룸") },
                        label = { Text("스터디룸") },
                        selected = false,
                        onClick = {
                            viewModel.navigateTo(Screen.StudyRoom)
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
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
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.Gray,
                        cursorColor = Color.White,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
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

            // 기본 배경색 설정
            val backgroundColor = if (state.useGrassBackground) Color(0xFF99C658) else Color.Black
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
            )

            // 공부 모드가 아닐 때와 grass 배경을 사용할 때만 Image를 애니메이션과 함께 보이도록 설정
            AnimatedVisibility(
                visible = (state.currentMode != Mode.STUDY || state.isPaused) && state.useGrassBackground,
                enter = fadeIn(animationSpec = tween(durationMillis = 1000)),
                exit = fadeOut(animationSpec = tween(durationMillis = 1000))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.grass_background),
                    contentDescription = "잔디, 꽃 배경",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
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

            val onMenuClick: () -> Unit = {
                scope.launch { drawerState.open() }
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
                        highlightColor = highlightColor,
                        onMenuClick = onMenuClick
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
                        highlightColor = highlightColor,
                        onMenuClick = onMenuClick
                    )
                }
            }
        }
    }
}