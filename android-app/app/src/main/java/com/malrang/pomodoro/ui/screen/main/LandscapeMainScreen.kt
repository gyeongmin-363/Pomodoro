package com.malrang.pomodoro.ui.screen.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.rememberAsyncImagePainter
import com.malrang.pomodoro.R
import com.malrang.pomodoro.dataclass.ui.Mode
import com.malrang.pomodoro.dataclass.ui.Screen
import com.malrang.pomodoro.viewmodel.BackgroundType
import com.malrang.pomodoro.viewmodel.BackgroundViewModel
import com.malrang.pomodoro.viewmodel.SettingsViewModel
import com.malrang.pomodoro.viewmodel.TimerViewModel
import java.io.File

@Composable
fun LandscapeMainScreen(
    timerViewModel: TimerViewModel,
    settingsViewModel: SettingsViewModel,
    backgroundViewModel: BackgroundViewModel,
    events: MainScreenEvents,
    onNavigateTo: (Screen) -> Unit,
    paddingValues: PaddingValues
) {
    val timerState by timerViewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()
    val backgroundState by backgroundViewModel.uiState.collectAsState()

    val customBgColor = Color(backgroundState.customBgColor)
    val customTextColor = Color(backgroundState.customTextColor)
    val isImageMode = backgroundState.backgroundType == BackgroundType.IMAGE
    val imagePath = backgroundState.selectedImagePath

    val titleText = when (timerState.currentMode) {
        Mode.STUDY -> "집중 시간"
        Mode.SHORT_BREAK, Mode.LONG_BREAK -> "휴식 시간"
    }
    val currentWorkName = settingsState.workPresets.find { it.id == settingsState.currentWorkId }?.name ?: "기본"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(customBgColor)
    ) {
        if (isImageMode && imagePath != null) {
            Image(
                painter = rememberAsyncImagePainter(model = File(imagePath)),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 16.dp)
                .padding(bottom = paddingValues.calculateBottomPadding()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 좌측: 정보 및 사이클 (1/3)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    color = customTextColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(50),
                ) {
                    Text(
                        text = currentWorkName,
                        fontSize = 14.sp,
                        color = customTextColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "완료 세션: ${timerState.totalSessions}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = customTextColor
                )
                Spacer(Modifier.height(16.dp))
                CycleIndicator(
                    modifier = Modifier.fillMaxWidth(0.8f),
                    currentMode = timerState.currentMode,
                    totalSessions = timerState.totalSessions,
                    longBreakInterval = settingsState.settings.longBreakInterval,
                    borderColor = customTextColor.copy(alpha = 0.5f),
                    itemsPerRow = 6
                )
            }

            // 중앙: 타이머 (1/3)
            Column(
                modifier = Modifier.weight(1.2f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = titleText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Normal,
                    color = customTextColor.copy(alpha = 0.9f)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "%02d:%02d".format(timerState.timeLeft / 60, timerState.timeLeft % 60),
                    fontSize = 80.sp,
                    fontWeight = FontWeight.Bold,
                    color = customTextColor,
                    letterSpacing = 2.sp
                )
            }

            // 우측: 컨트롤 버튼 (1/3)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                // 재생/일시정지
                FilledIconButton(
                    onClick = {
                        if (!timerState.isRunning) timerViewModel.startTimer(settingsState.settings)
                        else timerViewModel.pauseTimer()
                    },
                    modifier = Modifier.size(64.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        painterResource(id = if (!timerState.isRunning) R.drawable.ic_play else R.drawable.ic_pause),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    IconButton(onClick = { events.onShowResetConfirmChange(true) }) {
                        Icon(painterResource(id = R.drawable.ic_reset), contentDescription = "리셋", tint = customTextColor)
                    }
                    IconButton(onClick = { events.onShowSkipConfirmChange(true) }) {
                        Icon(painterResource(id = R.drawable.ic_skip), contentDescription = "스킵", tint = customTextColor)
                    }
                }
            }
        }
    }
}