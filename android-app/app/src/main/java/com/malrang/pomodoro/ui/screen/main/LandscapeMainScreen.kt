package com.malrang.pomodoro.ui.screen.main

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.malrang.pomodoro.viewmodel.SettingsViewModel
import com.malrang.pomodoro.viewmodel.TimerViewModel

@Composable
fun LandscapeMainScreen(
    timerViewModel: TimerViewModel,
    settingsViewModel: SettingsViewModel,
    events: MainScreenEvents,
    onNavigateTo: (Screen) -> Unit,
) {
    val timerState by timerViewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()

    // showWorkManager 상태 제거됨

    val contentColor = MaterialTheme.colorScheme.onBackground
    val secondaryTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val highlightColor = MaterialTheme.colorScheme.primary

    val currentWorkName = settingsState.workPresets.find { it.id == settingsState.currentWorkId }?.name ?: "기본"
    val titleText = when (timerState.currentMode) {
        Mode.STUDY -> "운행 중"
        Mode.SHORT_BREAK, Mode.LONG_BREAK -> "정차 중"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 왼쪽: 구간 정보 및 사이클 인디케이터
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    buildAnnotatedString {
                        withStyle(style = SpanStyle(color = secondaryTextColor)) { append("구간 완료 : ") }
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

            // 중앙: 타이머 및 상태 텍스트
            Column(
                modifier = Modifier.weight(2f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // ✅ Work 선택 버튼 제거 -> 텍스트만 표시
                Text(
                    text = currentWorkName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor
                )

                // AnimatedVisibility(visible = showWorkManager) 제거됨

                Spacer(Modifier.height(16.dp))

                Text(text = titleText, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = contentColor)

                Spacer(Modifier.height(8.dp))
                Text(
                    text = "%02d:%02d".format(timerState.timeLeft / 60, timerState.timeLeft % 60),
                    fontSize = 60.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )

                // 하단 여백 (기존 버스 애니메이션 자리)
                Spacer(Modifier.height(60.dp))
            }

            // 오른쪽: 제어 버튼
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!timerState.isRunning) {
                        IconButton(onClick = {
                            timerViewModel.startTimer(settingsState.settings)
                        }) {
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