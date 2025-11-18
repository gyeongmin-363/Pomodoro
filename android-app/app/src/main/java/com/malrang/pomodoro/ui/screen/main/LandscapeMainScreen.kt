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
// import com.airbnb.lottie.compose.* // Lottie import 제거
import com.malrang.pomodoro.R
import com.malrang.pomodoro.dataclass.ui.Mode
import com.malrang.pomodoro.dataclass.ui.Screen
import com.malrang.pomodoro.viewmodel.SettingsViewModel
import com.malrang.pomodoro.viewmodel.TimerViewModel
// import kotlinx.coroutines.delay // 제거

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

                // --- 버스 애니메이션 제거 ---
                // AnimatedVisibility(visible = timerState.isRunning && timerState.currentMode == Mode.STUDY) { ... } 블록 제거
                // 대신 Spacer 추가
                Spacer(Modifier.height(100.dp))
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
                            // showTicketAnimation = true 제거
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