package com.malrang.pomodoro.ui.screen.main

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.malrang.pomodoro.ui.theme.Typography
import com.malrang.pomodoro.viewmodel.PomodoroViewModel

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

    // 1. 가장 바깥 Column: 전체 화면을 차지하며, 자식들을 수직으로 배치
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 2. 내부 Column: 중앙에 위치할 컨텐츠들을 그룹화하고 중앙 정렬
        Column(
            modifier = Modifier.weight(1f), // 하단 Row를 제외한 모든 공간 차지
            verticalArrangement = Arrangement.Center, // 차지한 공간 내에서 수직 중앙 정렬
            horizontalAlignment = Alignment.CenterHorizontally // 자식들을 수평 중앙 정렬
        ) {
            TextButton(onClick = { onShowWorkManagerChange(!showWorkManager) }) {
                Text(currentWorkName, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = contentColor, style = Typography.bodyLarge)
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
            Spacer(Modifier.height(24.dp))
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
        }

        // 3. 하단 Row: weight가 적용된 위의 Column 때문에 자동으로 최하단에 위치
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = { viewModel.navigateTo(Screen.Collection) }) {
                Icon(painterResource(id = R.drawable.ic_collection), contentDescription = "동물 도감", tint = contentColor)
            }
            IconButton(onClick = { viewModel.navigateTo(Screen.Stats) }) {
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
            IconButton(onClick = {
                viewModel.navigateTo(Screen.StudyRoom)
            }) {
                Icon(Icons.Default.ThumbUp, contentDescription = "스터디룸", tint = contentColor)
            }
        }
    }
}
