package com.malrang.pomodoro.ui.screen.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.rememberAsyncImagePainter
import com.malrang.pomodoro.R
import com.malrang.pomodoro.dataclass.ui.Mode
import com.malrang.pomodoro.dataclass.ui.Screen
import com.malrang.pomodoro.viewmodel.BackgroundType
import com.malrang.pomodoro.viewmodel.BackgroundViewModel // [추가]
import com.malrang.pomodoro.viewmodel.SettingsViewModel
import com.malrang.pomodoro.viewmodel.TimerViewModel
import java.io.File

@Composable
fun PortraitMainScreen(
    timerViewModel: TimerViewModel,
    settingsViewModel: SettingsViewModel,
    backgroundViewModel: BackgroundViewModel, // [추가] BackgroundViewModel 주입
    events: MainScreenEvents,
    onNavigateTo: (Screen) -> Unit,
    paddingValues: PaddingValues
) {
    val timerState by timerViewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()
    val backgroundState by backgroundViewModel.uiState.collectAsState() // [추가] 상태 구독

    // [수정] settingsState -> backgroundState로 변경
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = currentWorkName,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = customTextColor
                )

                Spacer(Modifier.height(16.dp))

                Box(
                    modifier = Modifier.height(350.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = titleText,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = customTextColor
                        )
                        Spacer(Modifier.height(24.dp))

                        Text(
                            text = "%02d:%02d".format(timerState.timeLeft / 60, timerState.timeLeft % 60),
                            fontSize = 60.sp,
                            fontWeight = FontWeight.Bold,
                            color = customTextColor
                        )
                    }
                }

                Text(
                    buildAnnotatedString {
                        withStyle(style = SpanStyle(color = customTextColor.copy(alpha = 0.7f))) { append("구간 완료 : ") }
                        withStyle(
                            style = SpanStyle(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
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
                    borderColor = customTextColor.copy(alpha = 0.5f),
                    itemsPerRow = 8
                )
                Spacer(Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!timerState.isRunning) {
                        IconButton(onClick = {
                            timerViewModel.startTimer(settingsState.settings)
                        }) {
                            Icon(painterResource(id = R.drawable.ic_play), contentDescription = "시작", tint = customTextColor)
                        }
                    } else {
                        IconButton(onClick = { timerViewModel.pauseTimer() }) {
                            Icon(painterResource(id = R.drawable.ic_pause), contentDescription = "일시 정지", tint = customTextColor)
                        }
                    }

                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { events.onShowResetConfirmChange(true) }) {
                        Icon(painterResource(id = R.drawable.ic_reset), contentDescription = "리셋", tint = customTextColor)
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { events.onShowSkipConfirmChange(true) }) {
                        Icon(painterResource(id = R.drawable.ic_skip), contentDescription = "스킵", tint = customTextColor)
                    }
                }
            }
        }
    }
}