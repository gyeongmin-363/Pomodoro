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
import androidx.compose.ui.text.font.FontWeight
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
                .padding(horizontal = 48.dp, vertical = 24.dp)
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
                // Work Name Badge
                Box(
                    modifier = Modifier
                        .offset(x = 4.dp, y = 4.dp)
                        .background(MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .offset(x = (-4).dp, y = (-4).dp)
                            .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(8.dp))
                            .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = currentWorkName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

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
                    borderColor = customTextColor,
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
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = customTextColor.copy(alpha = 0.9f)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "%02d:%02d".format(timerState.timeLeft / 60, timerState.timeLeft % 60),
                    fontSize = 90.sp,
                    fontWeight = FontWeight.Black,
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
                // Play Button
                NeoIconButton(
                    onClick = {
                        if (!timerState.isRunning) timerViewModel.startTimer(settingsState.settings)
                        else timerViewModel.pauseTimer()
                    },
                    iconRes = if (!timerState.isRunning) R.drawable.ic_play else R.drawable.ic_pause,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    size = 80.dp,
                    iconSize = 36.dp,
                    shadowOffset = 5.dp
                )

                Spacer(Modifier.height(32.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    NeoIconButton(
                        onClick = { events.onShowResetConfirmChange(true) },
                        iconRes = R.drawable.ic_reset,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        size = 56.dp,
                        iconSize = 24.dp
                    )
                    NeoIconButton(
                        onClick = { events.onShowSkipConfirmChange(true) },
                        iconRes = R.drawable.ic_skip,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        size = 56.dp,
                        iconSize = 24.dp
                    )
                }
            }
        }
    }
}

// Landscape용 동일한 NeoIconButton 컴포저블 (파일 내 복사)
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = shadowOffset, y = shadowOffset)
                .background(MaterialTheme.colorScheme.outline, CircleShape)
        )
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