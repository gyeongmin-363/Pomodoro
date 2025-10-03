package com.malrang.pomodoro.ui.screen.main

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.malrang.pomodoro.ui.theme.SetBackgroundImage
import com.malrang.pomodoro.viewmodel.PomodoroViewModel
import kotlinx.coroutines.launch

// 드로어 아이템을 위한 데이터 클래스
private data class DrawerItem(
    val iconRes: Int? = null,
    val imageVector: ImageVector? = null,
    val label: String,
    val screen: Screen? = null,
    val onCustomClick: (() -> Unit)? = null
)

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
    var presetIdToSelect by remember { mutableStateOf<String?>(null) }

    val contentColor = if (state.useGrassBackground) Color.Black else Color.White
    val secondaryTextColor = Color.LightGray
    val highlightColor = if (state.useGrassBackground) Color(0xFF01579B) else Color.Cyan

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()


    // 1. 픽셀 아트 스타일 색상
    val drawerContentColor = Color(0xFFF0F0F0)   // 밝은 텍스트/아이콘 색상

    // 3. 드로어 메뉴 아이템 리스트
    val drawerItems = listOf(
        DrawerItem(iconRes = R.drawable.ic_collection, label = "동물 도감", screen = Screen.Collection),
        DrawerItem(iconRes = R.drawable.ic_stats, label = "통계", screen = Screen.Stats),
        DrawerItem(
            iconRes = R.drawable.light_night,
            label = "배경 변경",
            onCustomClick = {
                if (state.useGrassBackground) {
                    Toast.makeText(context, "어두운 배경에서는 동물이 나타나지 않아요.", Toast.LENGTH_SHORT).show()
                }
                viewModel.toggleBackground()
            }
        ),
        DrawerItem(iconRes = R.drawable.ic_military_tech_24px, label = "챌린지룸", screen = Screen.StudyRoom),
        DrawerItem(imageVector = Icons.Filled.AccountCircle, label = "계정 설정", screen = Screen.AccountSettings)
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerShape = RoundedCornerShape(0.dp),
                modifier = Modifier.fillMaxWidth(0.7f),
            ) {
                Box{
                    SetBackgroundImage()
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(top = 16.dp) // 상단 여백 추가
                    ) {
                        drawerItems.forEach { item ->
                            NavigationDrawerItem(
                                icon = {
                                    if (item.iconRes != null) {
                                        Icon(
                                            painterResource(id = item.iconRes),
                                            contentDescription = item.label,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    } else if (item.imageVector != null) {
                                        Icon(
                                            item.imageVector,
                                            contentDescription = item.label,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                },
                                label = {
                                    Text(
                                        text = item.label,
                                        fontWeight = FontWeight.Normal
                                    )
                                },
                                selected = false, // 선택 상태는 이 예제에서 사용하지 않음
                                onClick = {
                                    item.screen?.let { viewModel.navigateTo(it) }
                                    item.onCustomClick?.invoke()
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                                    .border(2.dp, Color.White.copy(alpha = 0.7f)), // 픽셀 느낌을 위한 테두리
                                shape = RectangleShape, // 각진 모양
                                colors = NavigationDrawerItemDefaults.colors(
                                    unselectedContainerColor = Color.Gray.copy(alpha = 0.7f), // 기본 배경 투명
                                    unselectedIconColor = drawerContentColor,
                                    unselectedTextColor = drawerContentColor,
                                )
                            )
                        }
                    }
                }
            }
        }
    ) {
        if (presetIdToSelect != null) {
            PixelArtConfirmDialog(
                onDismissRequest = { presetIdToSelect = null },
                title = "Work 변경",
                confirmText = "확인",
                onConfirm = {
                    viewModel.selectWorkPreset(presetIdToSelect!!)
                    presetIdToSelect = null
                }
            ) {
                Text(
                    "Work를 변경하면 현재 진행상황이 초기화됩니다. 계속하시겠습니까?",
                    color = secondaryTextColor
                )
            }
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
                    onValueChange = {
                        if (it.length <= 10) { // 10자 이하로 제한
                            newPresetName = it
                        }},
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
                        modifier = Modifier
                            .absoluteOffset { IntOffset(sp.x.toInt(), sp.y.toInt()) }
                            .size(sp.sizeDp.dp)
                    )
                }
            }


            val onMenuClick: () -> Unit = {
                scope.launch { drawerState.open() }
            }

            val onSelectPreset: (String) -> Unit = { presetId ->
                if (state.currentWorkId != presetId) {
                    presetIdToSelect = presetId
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
                        onSelectPreset = onSelectPreset,
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
                        onSelectPreset = onSelectPreset,
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