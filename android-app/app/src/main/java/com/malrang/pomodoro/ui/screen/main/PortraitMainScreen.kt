package com.malrang.pomodoro.ui.screen.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malrang.pomodoro.R
import com.malrang.pomodoro.dataclass.ui.Mode
import com.malrang.pomodoro.dataclass.ui.Screen
import com.malrang.pomodoro.dataclass.ui.WorkPreset
import com.malrang.pomodoro.ui.theme.Typography
import com.malrang.pomodoro.viewmodel.MainViewModel
import com.malrang.pomodoro.viewmodel.SettingsUiState
import com.malrang.pomodoro.viewmodel.SettingsViewModel
import com.malrang.pomodoro.viewmodel.TimerUiState
import com.malrang.pomodoro.viewmodel.TimerViewModel

@Composable
fun PortraitMainScreen(
    mainViewModel: MainViewModel,
    timerViewModel: TimerViewModel,
    settingsViewModel: SettingsViewModel,
    events: MainScreenEvents, // 여러 파라미터를 하나로 받음
    contentColor: Color,
    secondaryTextColor: Color,
    highlightColor: Color,
) {
    val timerState by timerViewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()
    // showWorkManager 상태를 내부에서 직접 관리
    var showWorkManager by remember { mutableStateOf(false) }

    val titleText = when (timerState.currentMode) {
        Mode.STUDY -> "📖 공부 시간"
        Mode.SHORT_BREAK, Mode.LONG_BREAK -> "☕ 휴식 시간"
    }
    val currentWorkName = settingsState.workPresets.find { it.id == settingsState.currentWorkId }?.name ?: "기본"

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TextButton(onClick = { showWorkManager = !showWorkManager }) {
                    Text(currentWorkName, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = contentColor, style = Typography.bodyLarge)
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
                            mainViewModel.navigateTo(Screen.Settings)
                        },
                        useGrassBackground = settingsState.useGrassBackground
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
                Spacer(Modifier.height(24.dp))
                Text(
                    buildAnnotatedString {
                        withStyle(style = SpanStyle(color = secondaryTextColor)) { append("연속 완료 세션 : ") }
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!timerState.isRunning) {
                        IconButton(onClick = { timerViewModel.startTimer(settingsState.settings) }) {
                            Icon(painterResource(id = R.drawable.ic_play), contentDescription = "시작", tint = contentColor)
                        }
                    } else {
                        IconButton(onClick = { timerViewModel.pauseTimer() }) {
                            Icon(painterResource(id = R.drawable.ic_pause), contentDescription = "일시정지", tint = contentColor)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { events.onShowResetConfirmChange(true) }) {
                        Icon(painterResource(id = R.drawable.ic_reset), contentDescription = "리셋", tint = contentColor)
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { events.onShowSkipConfirmChange(true) }) {
                        Icon(painterResource(id = R.drawable.ic_skip), contentDescription = "건너뛰기", tint = contentColor)
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