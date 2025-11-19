package com.malrang.pomodoro.ui.screen.main

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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
import com.malrang.pomodoro.viewmodel.BackgroundType
import com.malrang.pomodoro.viewmodel.SettingsViewModel
import com.malrang.pomodoro.viewmodel.TimerViewModel

@Composable
fun PortraitMainScreen(
    timerViewModel: TimerViewModel,
    settingsViewModel: SettingsViewModel,
    events: MainScreenEvents,
    onNavigateTo: (Screen) -> Unit,
    paddingValues: PaddingValues // [추가]
) {
    val timerState by timerViewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()

    val customBgColor = Color(settingsState.customBgColor)
    val customTextColor = Color(settingsState.customTextColor)
    val isImageMode = settingsState.backgroundType == BackgroundType.IMAGE
    val imagePath = settingsState.selectedImagePath

    val titleText = when (timerState.currentMode) {
        Mode.STUDY -> "운행 중"
        Mode.SHORT_BREAK, Mode.LONG_BREAK -> "정차 중"
    }
    val currentWorkName = settingsState.workPresets.find { it.id == settingsState.currentWorkId }?.name ?: "기본"

    // 배경 렌더링: 화면 전체를 채움 (Nav Bar 뒤까지)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(if (!isImageMode) Modifier.background(customBgColor) else Modifier)
    ) {
        if (isImageMode && imagePath != null) {
            val bitmap = remember(imagePath) {
                BitmapFactory.decodeFile(imagePath)?.asImageBitmap()
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                )
            } else {
                Box(Modifier.fillMaxSize().background(customBgColor))
            }
        }

        // 콘텐츠 영역: 전달받은 패딩(paddingValues)을 적용하여 Nav Bar 위에 표시
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp) // 상단, 좌우 패딩
                .padding(bottom = paddingValues.calculateBottomPadding()) // [중요] 하단 패딩 적용
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
                            Icon(painterResource(id = R.drawable.ic_play), contentDescription = "운행 시작", tint = customTextColor)
                        }
                    } else {
                        IconButton(onClick = { timerViewModel.pauseTimer() }) {
                            Icon(painterResource(id = R.drawable.ic_pause), contentDescription = "일시 정차", tint = customTextColor)
                        }
                    }

                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { events.onShowResetConfirmChange(true) }) {
                        Icon(painterResource(id = R.drawable.ic_reset), contentDescription = "회차", tint = customTextColor)
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { events.onShowSkipConfirmChange(true) }) {
                        Icon(painterResource(id = R.drawable.ic_skip), contentDescription = "다음 구간으로", tint = customTextColor)
                    }
                }
            }
        }
    }
}