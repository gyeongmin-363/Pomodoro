package com.malrang.pomodoro.ui.screen.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.malrang.pomodoro.viewmodel.BackgroundViewModel
import com.malrang.pomodoro.viewmodel.SettingsViewModel
import com.malrang.pomodoro.viewmodel.TimerViewModel
import java.io.File

@Composable
fun PortraitMainScreen(
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
        // 배경 이미지
        if (isImageMode && imagePath != null) {
            Image(
                painter = rememberAsyncImagePainter(model = File(imagePath)),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // 배경 딤 처리
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)) // 가독성을 위해 조금 더 어둡게
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp, start = 24.dp, end = 24.dp)
                .padding(bottom = paddingValues.calculateBottomPadding()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. 상단 정보 (Work 이름)
            Spacer(Modifier.height(20.dp))
            Surface(
                color = customTextColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(50),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Text(
                    text = currentWorkName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = customTextColor,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            Spacer(Modifier.weight(1f))

            // 2. 메인 타이머 영역
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Normal,
                    color = customTextColor.copy(alpha = 0.9f)
                )

                Spacer(Modifier.height(16.dp))

                // 타이머 텍스트 (매우 크게)
                Text(
                    text = "%02d:%02d".format(timerState.timeLeft / 60, timerState.timeLeft % 60),
                    fontSize = 80.sp, // 크기 확대
                    fontWeight = FontWeight.Bold,
                    color = customTextColor,
                    style = MaterialTheme.typography.displayLarge,
                    letterSpacing = 4.sp // 자간 넓힘
                )
            }

            Spacer(Modifier.weight(1f))

            // 3. 하단 컨트롤 및 정보 영역
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                // 구간 정보
                Text(
                    buildAnnotatedString {
                        withStyle(style = SpanStyle(color = customTextColor.copy(alpha = 0.7f))) { append("완료한 세션  ") }
                        withStyle(
                            style = SpanStyle(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary // 강조색 사용
                            )
                        ) { append("${timerState.totalSessions}") }
                    },
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(Modifier.height(12.dp))

                CycleIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    currentMode = timerState.currentMode,
                    totalSessions = timerState.totalSessions,
                    longBreakInterval = settingsState.settings.longBreakInterval,
                    borderColor = customTextColor.copy(alpha = 0.5f),
                    itemsPerRow = 8
                )

                Spacer(Modifier.height(40.dp))

                // 컨트롤 버튼 (Play 버튼 강조)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 리셋 버튼 (보조)
                    IconButton(
                        onClick = { events.onShowResetConfirmChange(true) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            painterResource(id = R.drawable.ic_reset),
                            contentDescription = "리셋",
                            tint = customTextColor.copy(alpha = 0.8f),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // 재생/일시정지 버튼 (메인 - 큼직하게)
                    FilledIconButton(
                        onClick = {
                            if (!timerState.isRunning) timerViewModel.startTimer(settingsState.settings)
                            else timerViewModel.pauseTimer()
                        },
                        modifier = Modifier.size(72.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            painterResource(id = if (!timerState.isRunning) R.drawable.ic_play else R.drawable.ic_pause),
                            contentDescription = if (!timerState.isRunning) "시작" else "일시정지",
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // 스킵 버튼 (보조)
                    IconButton(
                        onClick = { events.onShowSkipConfirmChange(true) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            painterResource(id = R.drawable.ic_skip),
                            contentDescription = "스킵",
                            tint = customTextColor.copy(alpha = 0.8f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}