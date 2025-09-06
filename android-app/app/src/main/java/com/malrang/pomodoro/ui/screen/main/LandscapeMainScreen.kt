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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.malrang.pomodoro.dataclass.ui.PomodoroUiState
import com.malrang.pomodoro.dataclass.ui.Screen
import com.malrang.pomodoro.dataclass.ui.WorkPreset
import com.malrang.pomodoro.viewmodel.PomodoroViewModel

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
    highlightColor: Color,
    onMenuClick: () -> Unit
) {
    val currentWorkName = state.workPresets.find { it.id == state.currentWorkId }?.name ?: "기본"
    val titleText = when (state.currentMode) {
        Mode.STUDY -> "📖 공부 시간"
        Mode.SHORT_BREAK, Mode.LONG_BREAK -> "☕ 휴식 시간"
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                        withStyle(style = SpanStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = highlightColor
                        )
                        ) { append("${state.totalSessions} ") }
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
                            viewModel.navigateTo(Screen.Settings)
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
            }
        }
        IconButton(
            onClick = onMenuClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Icon(imageVector = Icons.Default.Menu, contentDescription = "메뉴 열기", tint = contentColor)
        }
    }
}