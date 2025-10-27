package com.malrang.pomodoro.ui.screen.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
fun PortraitMainScreen(
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


    val titleText = when (timerState.currentMode) {
        Mode.STUDY -> "운행 중" // 용어 변경
        Mode.SHORT_BREAK, Mode.LONG_BREAK -> "정차 중" // 용어 변경
    }
    val currentWorkName = settingsState.workPresets.find { it.id == settingsState.currentWorkId }?.name ?: "기본"

    // --- Lottie 애니메이션 상태 ---
    var showTicketAnimation by remember { mutableStateOf(false) }
    var showBusAnimation by remember { mutableStateOf(false) } // [추가] 버스 애니메이션 표시 상태
    val busComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.bus_ticket))
    val ticketComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.ticket_icon_animation))

    val busProgress by animateLottieCompositionAsState(
        composition = busComposition,
        isPlaying = showBusAnimation, // showBusAnimation 상태에 따라 재생
        iterations = LottieConstants.IterateForever // 무한 반복
    )
    val ticketProgress by animateLottieCompositionAsState(
        composition = ticketComposition,
        isPlaying = showTicketAnimation, // 재생 제어
        restartOnPlay = true // 다시 재생될 때 처음부터
    )

    // 티켓 애니메이션이 끝나면 상태 변경 및 버스 애니메이션 시작
    LaunchedEffect(showTicketAnimation, ticketProgress) {
        // 1f에 정확히 도달하지 못하는 경우를 대비해 0.99f 이상으로 체크
        if (showTicketAnimation && ticketProgress >= 0.99f) {
            showTicketAnimation = false
        }
        // [추가] 티켓 애니메이션이 끝나고, 현재 모드가 STUDY일 때 버스 애니메이션 시작
        if (!showTicketAnimation && ticketProgress >= 0.99f && timerState.currentMode == Mode.STUDY) {
            showBusAnimation = true
        }
        // [추가] STUDY 모드가 아니거나 타이머가 멈추면 버스 애니메이션 중지
        if (timerState.currentMode != Mode.STUDY || !timerState.isRunning) {
            showBusAnimation = false
        }
    }
    // --- Lottie 끝 ---

    // --- "준비" 상태인지 확인하는 로직 ---
    // 현재 모드의 전체 시간을 초 단위로 계산
    val currentModeFullTimeInSeconds = when (timerState.currentMode) {
        Mode.STUDY -> settingsState.settings.studyTime * 60
        Mode.SHORT_BREAK -> settingsState.settings.shortBreakTime * 60
        Mode.LONG_BREAK -> settingsState.settings.longBreakTime * 60
    }

    // "준비 상태"인지 확인 (타이머가 멈춰있음 && 시간이 꽉 차 있음)
    // (이 상태는 공부, 휴식 모든 모드의 첫 시작에 해당)
    val isReadyToStart = !timerState.isRunning &&
            timerState.timeLeft == currentModeFullTimeInSeconds

    // "준비" 상태일 때 표시할 분
    val currentModeMinutes = timerState.timeLeft / 60

    // "준비" 상태일 때 표시할 제목
    val readyTitle = when (timerState.currentMode) {
        Mode.STUDY -> "운행 준비 ($currentModeMinutes 분)"
        Mode.SHORT_BREAK, Mode.LONG_BREAK -> "정차 준비 ($currentModeMinutes 분)"
    }
    // --- "준비" 로직 끝 ---


    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // 메인 컨텐츠 영역 (세로 중앙 정렬 및 가중치 적용)
            Column(
                modifier = Modifier
                    .weight(1f) // 남은 공간을 모두 차지하도록
                    .fillMaxWidth(), // 가로 폭 채우기
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
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

                // --- 중앙 타이머/Lottie/버스 영역 ---
                // 이 Box는 Lottie 아이콘과 버스 애니메이션의 컨테이너 역할을 합니다.
                Box(
                    modifier = Modifier
                        .height(350.dp), // [중요] 모든 Lottie 이미지는 이 크기를 따라감
                    contentAlignment = Alignment.Center
                ) {
                    if (isReadyToStart) {
                        // 1. "준비" 상태일 때
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = readyTitle, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = contentColor)

                            // --- 중앙 Lottie 티켓 (클릭 가능) ---
                            // 오버레이와 겹치게 하므로 Box로 감쌀 필요 없음
                            LottieAnimation(
                                composition = ticketComposition,
                                progress = { 0.4f }, // 40% 진행된 프레임
                                modifier = Modifier
                                    .fillMaxSize() // 원하는 큰 크기로 조절
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null, // 클릭 효과 제거
                                        onClick = {
                                            showTicketAnimation = true // 티켓 오버레이 애니메이션 시작
                                            timerViewModel.startTimer(settingsState.settings)
                                        }
                                    )
                            )
                            // --- 중앙 Lottie 끝 ---

                            Text(text = "티켓을 눌러 타이머 시작하기", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = contentColor)
                        }
                    } else {
                        // 2. "실행 중" 또는 "일시정지" 상태일 때
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = titleText, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = contentColor)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "%02d:%02d".format(timerState.timeLeft / 60, timerState.timeLeft % 60),
                                fontSize = 60.sp,
                                fontWeight = FontWeight.Bold,
                                color = contentColor
                            )
                            Spacer(Modifier.height(16.dp)) // 시간과 버스 사이 간격

                            // --- 버스 ---
                            //STUDY 모드일 때만 보이도록
                            if(timerState.currentMode == Mode.STUDY){
                                LottieAnimation(
                                    composition = busComposition,
                                    progress = { busProgress },
                                    modifier = Modifier.fillMaxSize() // 원하는 크기로 조절
                                )
                            }
                            // --- 버스 끝 ---
                        }
                    }

                    // --- 티켓 오버레이 애니메이션 (중앙 Lottie 위치와 동일하게) ---
                    if (showTicketAnimation) {
                        LottieAnimation(
                            composition = ticketComposition,
                            progress = { ticketProgress },
                            modifier = Modifier
                                .fillMaxSize() // 중앙 Lottie와 동일한 크기
                                .align(Alignment.Center) // Box 내에서 중앙 정렬
                                .clickable( // 애니메이션 중 클릭 방지
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {}
                                )
                        )
                    }
                    // --- 티켓 애니메이션 끝 ---
                }
                // --- 중앙 영역 끝 ---


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
                    itemsPerRow = 8
                )
                Spacer(Modifier.height(16.dp))

                // --- 버튼 로직 (리셋/스킵 항상 표시) ---
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Play/Pause 버튼 또는 빈 공간 (조건부)
                    if (isReadyToStart) {
                        // "준비" 상태일 때는 Play/Pause 버튼 자리를 비워둠
                        Spacer(Modifier.width(48.dp))
                    } else {
                        // "실행 중" 또는 "일시정지" 상태일 때 Play/Pause 버튼 표시
                        if (!timerState.isRunning) {
                            IconButton(onClick = {
                                timerViewModel.startTimer(settingsState.settings)
                            }) {
                                Icon(painterResource(id = R.drawable.ic_play), contentDescription = "운행 시작", tint = contentColor)
                            }
                        } else {
                            IconButton(onClick = { timerViewModel.pauseTimer() }) {
                                Icon(painterResource(id = R.drawable.ic_pause), contentDescription = "일시 정차", tint = contentColor)
                            }
                        }
                    }

                    // 2. 리셋, 스킵 버튼 (항상 표시)
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { events.onShowResetConfirmChange(true) }) {
                        Icon(painterResource(id = R.drawable.ic_reset), contentDescription = "회차", tint = contentColor) // 용어 변경
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { events.onShowSkipConfirmChange(true) }) {
                        Icon(painterResource(id = R.drawable.ic_skip), contentDescription = "다음 구간으로", tint = contentColor) // 용어 변경
                    }
                }
                // --- 버튼 로직 끝 ---
            }
        }

        // 탑 앱바 (메뉴 버튼) - 위치 유지
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