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
import com.malrang.pomodoro.viewmodel.SettingsViewModel
import com.malrang.pomodoro.viewmodel.TimerViewModel
import java.io.File

@Composable
fun LandscapeMainScreen(
    timerViewModel: TimerViewModel,
    settingsViewModel: SettingsViewModel,
    events: MainScreenEvents,
    onNavigateTo: (Screen) -> Unit,
    paddingValues: PaddingValues
) {
    val timerState by timerViewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()

    val customBgColor = Color(settingsState.customBgColor)
    val customTextColor = Color(settingsState.customTextColor)
    val isImageMode = settingsState.backgroundType == BackgroundType.IMAGE
    val imagePath = settingsState.selectedImagePath

    val contentColor = customTextColor
    val highlightColor = MaterialTheme.colorScheme.primary

    val currentWorkName = settingsState.workPresets.find { it.id == settingsState.currentWorkId }?.name ?: "기본"
    val titleText = when (timerState.currentMode) {
        Mode.STUDY -> "집중 시간"
        Mode.SHORT_BREAK, Mode.LONG_BREAK -> "휴식 시간"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(customBgColor) // [수정] Fallback 배경색
    ) {
        if (isImageMode && imagePath != null) {
            // [수정] 비동기 이미지 로딩 (Coil)
            Image(
                painter = rememberAsyncImagePainter(model = File(imagePath)),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                .padding(bottom = paddingValues.calculateBottomPadding()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    buildAnnotatedString {
                        withStyle(style = SpanStyle(color = contentColor.copy(alpha = 0.7f))) { append("구간 완료 : ") }
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
                Text(
                    text = currentWorkName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor
                )
                Spacer(Modifier.height(16.dp))
                Text(text = titleText, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = contentColor)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "%02d:%02d".format(timerState.timeLeft / 60, timerState.timeLeft % 60),
                    fontSize = 60.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Spacer(Modifier.height(10.dp))
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!timerState.isRunning) {
                        IconButton(onClick = { timerViewModel.startTimer(settingsState.settings) }) {
                            Icon(painterResource(id = R.drawable.ic_play), contentDescription = "운행 시작", tint = contentColor)
                        }
                    } else {
                        IconButton(onClick = { timerViewModel.pauseTimer() }) {
                            Icon(painterResource(id = R.drawable.ic_pause), contentDescription = "일시 정차", tint = contentColor)
                        }
                    }
                    IconButton(onClick = { events.onShowResetConfirmChange(true) }) {
                        Icon(painterResource(id = R.drawable.ic_reset), contentDescription = "회차", tint = contentColor)
                    }
                    IconButton(onClick = { events.onShowSkipConfirmChange(true) }) {
                        Icon(painterResource(id = R.drawable.ic_skip), contentDescription = "다음 구간으로", tint = contentColor)
                    }
                }
            }
        }
    }
}