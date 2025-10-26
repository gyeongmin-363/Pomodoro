package com.malrang.pomodoro.ui.screen.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.* // Lottie import 추가
import com.malrang.pomodoro.R
import com.malrang.pomodoro.dataclass.ui.Mode
import com.malrang.pomodoro.dataclass.ui.Screen
import com.malrang.pomodoro.viewmodel.SettingsViewModel
import com.malrang.pomodoro.viewmodel.TimerViewModel
import kotlinx.coroutines.delay

@Composable
fun LandscapeMainScreen(
    timerViewModel: TimerViewModel,
    settingsViewModel: SettingsViewModel,
    events: MainScreenEvents, // 여러 파라미터를 하나로 받음
    onNavigateTo: (Screen) -> Unit,
) {
    val timerState by timerViewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()

    var showWorkManager by remember { mutableStateOf(false) }

    val contentColor = MaterialTheme.colorScheme.onBackground
    val secondaryTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val highlightColor = MaterialTheme.colorScheme.primary

    val currentWorkName = settingsState.workPresets.find { it.id == settingsState.currentWorkId }?.name ?: "기본"
    val titleText = when (timerState.currentMode) {
        Mode.STUDY -> "운행 중" // 용어 변경
        Mode.SHORT_BREAK, Mode.LONG_BREAK -> "정차 중" // 용어 변경
    }

    // --- Lottie 애니메이션 상태 ---
    var showTicketAnimation by remember { mutableStateOf(false) }
    val busComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.bus_ticket))
    val ticketComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.ticket_icon_animation))

    val busProgress by animateLottieCompositionAsState(
        composition = busComposition,
        iterations = LottieConstants.IterateForever // 무한 반복
    )
    val ticketProgress by animateLottieCompositionAsState(
        composition = ticketComposition,
        isPlaying = showTicketAnimation, // 재생 제어
        restartOnPlay = true // 다시 재생될 때 처음부터
    )

    // 티켓 애니메이션이 끝나면 상태 변경
    LaunchedEffect(ticketProgress) {
        if (ticketProgress == 1f) {
            showTicketAnimation = false
        }
    }
    // --- Lottie 끝 ---

    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    buildAnnotatedString {
                        withStyle(style = SpanStyle(color = secondaryTextColor)) { append("구간 완료 : ") } // 용어 변경
                        withStyle(style = SpanStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = highlightColor
                        )
                        ) { append("${timerState.totalSessions} ") }
                    }
                )
                Spacer(Modifier.height(16.dp))
                CycleIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    currentMode = timerState.currentMode,
                    totalSessions = timerState.totalSessions,
                    longBreakInterval = settingsState.settings.longBreakInterval,
                    borderColor = contentColor.copy(alpha = 0.5f),
                    itemsPerRow = 6
                )
            }

            Column(
                modifier = Modifier.weight(2f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                TextButton(onClick = { showWorkManager = !showWorkManager }) {
                    Text(currentWorkName, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = contentColor)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Work 선택", tint = contentColor)
                }

                AnimatedVisibility(visible = showWorkManager) {
                    WorkPresetsManager(
                        presets = settingsState.workPresets,
                        currentPresetId = settingsState.currentWorkId,
                        onPresetSelected = events.onSelectPreset,
                        onAddPreset = { settingsViewModel.addWorkPreset() },
                        onDeletePreset = { preset -> events.onPresetToDeleteChange(preset) },
                        onRenamePreset = { preset -> events.onPresetToRenameChange(preset) },
                        onEditSettings = { presetId ->
                            settingsViewModel.startEditingWorkPreset(presetId)
                            onNavigateTo(Screen.Settings)
                        },
                        // useGrassBackground 제거됨
                    )
                }
                Spacer(Modifier.height(16.dp))

                Text(text = titleText, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = contentColor)

                Spacer(Modifier.height(8.dp))
                Text(
                    text = "%02d:%02d".format(timerState.timeLeft / 60, timerState.timeLeft % 60),
                    fontSize = 60.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )

                // --- 버스 애니메이션 추가 ---
                AnimatedVisibility(visible = timerState.isRunning && timerState.currentMode == Mode.STUDY) {
                    LottieAnimation(
                        composition = busComposition,
                        progress = { busProgress },
                        modifier = Modifier.size(100.dp) // 가로 모드에 맞게 크기 조절
                    )
                }
                // --- 버스 애니메이션 끝 ---
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!timerState.isRunning) {
                        IconButton(onClick = {
                            showTicketAnimation = true // 티켓 애니메이션 시작
                            timerViewModel.startTimer(settingsState.settings)
                        }) {
                            Icon(painterResource(id = R.drawable.ic_play), contentDescription = "운행 시작", tint = contentColor) // 용어 변경
                        }
                    } else {
                        IconButton(onClick = { timerViewModel.pauseTimer() }) {
                            Icon(painterResource(id = R.drawable.ic_pause), contentDescription = "일시 정차", tint = contentColor) // 용어 변경
                        }
                    }
                    IconButton(onClick = { events.onShowResetConfirmChange(true) }) {
                        Icon(painterResource(id = R.drawable.ic_reset), contentDescription = "회차", tint = contentColor) // 용어 변경
                    }
                    IconButton(onClick = { events.onShowSkipConfirmChange(true) }) {
                        Icon(painterResource(id = R.drawable.ic_skip), contentDescription = "다음 구간으로", tint = contentColor) // 용어 변경
                    }
                }
            }
        }

        // --- 티켓 애니메이션 (버튼 위에 겹치도록) ---
        if (showTicketAnimation) {
            Box(
                modifier = Modifier.fillMaxSize(), // 전체 화면을 차지하도록
                contentAlignment = Alignment.Center // 중앙 정렬
            ) {
                LottieAnimation(
                    composition = ticketComposition,
                    progress = { ticketProgress },
                    modifier = Modifier
                        .size(180.dp) // 가로 모드에 맞게 크기 조절
                        .align(Alignment.Center)
                )
            }
        }
        // --- 티켓 애니메이션 끝 ---

        IconButton(
            onClick = events.onMenuClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Icon(imageVector = Icons.Default.Menu, contentDescription = "메뉴 열기", tint = contentColor)
        }
    }
}