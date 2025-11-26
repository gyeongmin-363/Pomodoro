package com.malrang.pomodoro.ui.screen.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.Dp
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
        // 배경 이미지 처리
        if (isImageMode && imagePath != null) {
            Image(
                painter = rememberAsyncImagePainter(model = File(imagePath)),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // 딤 처리
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp, start = 24.dp, end = 24.dp)
                .padding(bottom = paddingValues.calculateBottomPadding()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. 상단 정보 (Work 이름) - 배지 스타일
            Spacer(Modifier.height(32.dp))
            Box(
                modifier = Modifier
                    .offset(x = 4.dp, y = 4.dp)
                    .background(MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            ) {
                Box(
                    modifier = Modifier
                        .offset(x = (-4).dp, y = (-4).dp)
                        .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(12.dp)) // Pink Badge
                        .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = currentWorkName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // 2. 메인 타이머 영역
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = customTextColor.copy(alpha = 0.9f)
                )

                Spacer(Modifier.height(24.dp))

                // 타이머 텍스트
                Text(
                    text = "%02d:%02d".format(timerState.timeLeft / 60, timerState.timeLeft % 60),
                    fontSize = 90.sp,
                    fontWeight = FontWeight.Black, // 가장 굵게
                    color = customTextColor,
                    style = MaterialTheme.typography.displayLarge,
                    letterSpacing = 4.sp
                )
            }

            Spacer(Modifier.weight(1f))

            // 3. 하단 컨트롤 및 정보 영역
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                // 구간 정보 (카드 스타일)
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        buildAnnotatedString {
                            withStyle(style = SpanStyle(color = customTextColor, fontWeight = FontWeight.Medium)) { append("완료한 세션  ") }
                            withStyle(
                                style = SpanStyle(
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            ) { append("${timerState.totalSessions}") }
                        }
                    )
                }

                Spacer(Modifier.height(20.dp))

                CycleIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    currentMode = timerState.currentMode,
                    totalSessions = timerState.totalSessions,
                    longBreakInterval = settingsState.settings.longBreakInterval,
                    borderColor = customTextColor,
                    itemsPerRow = 8
                )

                Spacer(Modifier.height(50.dp))

                // 컨트롤 버튼
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 리셋 버튼
                    NeoIconButton(
                        onClick = { events.onShowResetConfirmChange(true) },
                        iconRes = R.drawable.ic_reset,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        size = 56.dp,
                        iconSize = 24.dp
                    )

                    // 재생/일시정지 버튼 (크고 화려하게)
                    NeoIconButton(
                        onClick = {
                            if (!timerState.isRunning) timerViewModel.startTimer(settingsState.settings)
                            else timerViewModel.pauseTimer()
                        },
                        iconRes = if (!timerState.isRunning) R.drawable.ic_play else R.drawable.ic_pause,
                        containerColor = MaterialTheme.colorScheme.primary, // Blue Pop
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        size = 88.dp,
                        iconSize = 40.dp,
                        shadowOffset = 6.dp
                    )

                    // 스킵 버튼
                    NeoIconButton(
                        onClick = { events.onShowSkipConfirmChange(true) },
                        iconRes = R.drawable.ic_skip,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        size = 56.dp,
                        iconSize = 24.dp
                    )
                }
                Spacer(Modifier.height(48.dp))
            }
        }
    }
}

// Neo-Brutalism 스타일 아이콘 버튼
@Composable
private fun NeoIconButton(
    onClick: () -> Unit,
    iconRes: Int,
    containerColor: Color,
    contentColor: Color,
    size: Dp,
    iconSize: Dp,
    shadowOffset: Dp = 4.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clickable(onClick = onClick)
    ) {
        // Shadow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = shadowOffset, y = shadowOffset)
                .background(MaterialTheme.colorScheme.outline, CircleShape)
        )
        // Button Face
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(containerColor, CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}