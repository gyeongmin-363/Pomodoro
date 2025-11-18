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
// Lottie 관련 import 추가
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.malrang.pomodoro.R
import com.malrang.pomodoro.dataclass.ui.Mode
import com.malrang.pomodoro.dataclass.ui.Screen
import com.malrang.pomodoro.viewmodel.SettingsViewModel
import com.malrang.pomodoro.viewmodel.TimerViewModel

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

                // --- 중앙 타이머 영역 (Lottie 수정) ---
                Box(
                    modifier = Modifier
                        .height(350.dp), // 기존 높이 유지
                    contentAlignment = Alignment.Center
                ) {
                    // --- 'isReadyToStart' 분기 제거 ---
                    // 2. "실행 중" 또는 "일시정지" 상태일 때 (항상 이 UI 표시)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = titleText, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = contentColor)
                        Spacer(Modifier.height(8.dp))

                        // --- Lottie 애니메이션 추가 ---
                        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.flight))
                        // '공부' 모드이고 '실행 중'일 때만 isPlaying = true
                        val isPlaying = timerState.currentMode == Mode.STUDY && timerState.isRunning

                        LottieAnimation(
                            composition = composition,
                            iterations = LottieConstants.IterateForever, // 계속 반복
                            isPlaying = isPlaying, // 조건부 재생
                            modifier = Modifier.height(150.dp) // 원하는 높이로 조절
                        )
                        // --- Lottie 애니메이션 끝 ---

                        Spacer(Modifier.height(8.dp)) // 애니메이션과 타이머 사이 간격

                        Text(
                            text = "%02d:%02d".format(timerState.timeLeft / 60, timerState.timeLeft % 60),
                            fontSize = 60.sp,
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        )
                        // --- 기존 Spacer 제거 ---
                        // Spacer(Modifier.height(16.dp))
                        // Spacer(Modifier.height(350.dp))
                    }

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

                // --- 버튼 로직 (isReadyToStart 분기 제거) ---
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Play/Pause 버튼
                    // 'isReadyToStart' 분기 제거. 이제 항상 Play/Pause 로직을 따름
                    if (!timerState.isRunning) {
                        IconButton(onClick = {
                            // showTicketAnimation = true 제거
                            timerViewModel.startTimer(settingsState.settings)
                        }) {
                            Icon(painterResource(id = R.drawable.ic_play), contentDescription = "운행 시작", tint = contentColor)
                        }
                    } else {
                        IconButton(onClick = { timerViewModel.pauseTimer() }) {
                            Icon(painterResource(id = R.drawable.ic_pause), contentDescription = "일시 정차", tint = contentColor)
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
    }
}