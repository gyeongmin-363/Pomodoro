package com.malrang.pomodoro.ui.screen.main

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import com.malrang.pomodoro.dataclass.ui.Screen
import com.malrang.pomodoro.ui.ModernConfirmDialog
import com.malrang.pomodoro.viewmodel.SettingsViewModel
import com.malrang.pomodoro.viewmodel.TimerViewModel

data class MainScreenEvents(
    val onShowResetConfirmChange: (Boolean) -> Unit,
    val onShowSkipConfirmChange: (Boolean) -> Unit,
)

@Composable
fun MainScreen(
    timerViewModel: TimerViewModel,
    settingsViewModel: SettingsViewModel,
    onNavigateTo: (Screen) -> Unit,
    paddingValues: PaddingValues // [추가] 상위 Scaffold로부터 받은 패딩
) {
    val timerState by timerViewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()

    var showResetConfirm by remember { mutableStateOf(false) }
    var showSkipConfirm by remember { mutableStateOf(false) }

    val events = MainScreenEvents(
        onShowResetConfirmChange = { showResetConfirm = it },
        onShowSkipConfirmChange = { showSkipConfirm = it }
    )

    if (showSkipConfirm) {
        ModernConfirmDialog(
            onDismissRequest = { showSkipConfirm = false },
            title = "세션 건너뛰기",
            confirmText = "확인",
            onConfirm = {
                timerViewModel.skipSession()
                showSkipConfirm = false
            },
            text = "현재 세션을 건너뛰시겠습니까?"
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { /* widthPx, heightPx are not used */ }
    ) {
        if (showResetConfirm) {
            ModernConfirmDialog(
                onDismissRequest = { showResetConfirm = false },
                title = "리셋 확인",
                confirmText = "확인",
                onConfirm = {
                    timerViewModel.reset(settingsState.settings)
                    showResetConfirm = false
                },
                text = "정말 리셋할 건가요?\n세션과 공부시간 등이 모두 초기화됩니다."
            )
        }

        // MainScreen 자체가 배경을 그리기 때문에 별도의 background Box는 제거하거나 투명 처리
        // PortraitMainScreen/LandscapeMainScreen 내부에서 배경 처리

        val configuration = LocalConfiguration.current
        when (configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                LandscapeMainScreen(
                    timerViewModel = timerViewModel,
                    settingsViewModel = settingsViewModel,
                    events = events,
                    onNavigateTo = onNavigateTo,
                    paddingValues = paddingValues // [추가] 패딩 전달
                )
            }
            else -> {
                PortraitMainScreen(
                    timerViewModel = timerViewModel,
                    settingsViewModel = settingsViewModel,
                    events = events,
                    onNavigateTo = onNavigateTo,
                    paddingValues = paddingValues // [추가] 패딩 전달
                )
            }
        }
    }
}