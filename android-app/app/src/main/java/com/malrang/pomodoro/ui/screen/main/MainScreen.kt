package com.malrang.pomodoro.ui.screen.main

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.malrang.pomodoro.dataclass.ui.Screen
import com.malrang.pomodoro.dataclass.ui.WorkPreset
import com.malrang.pomodoro.ui.ModernConfirmDialog
import com.malrang.pomodoro.viewmodel.SettingsViewModel
import com.malrang.pomodoro.viewmodel.TimerViewModel

// ✅ MainScreenEvents는 유지하되, onMenuClick은 동작하지 않도록 변경됨
data class MainScreenEvents(
    val onPresetToDeleteChange: (WorkPreset) -> Unit,
    val onPresetToRenameChange: (WorkPreset) -> Unit,
    val onShowResetConfirmChange: (Boolean) -> Unit,
    val onShowSkipConfirmChange: (Boolean) -> Unit,
    val onSelectPreset: (String) -> Unit,
)

@Composable
fun MainScreen(
    timerViewModel: TimerViewModel,
    settingsViewModel: SettingsViewModel,
    onNavigateTo: (Screen) -> Unit
) {
    val timerState by timerViewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()

    var presetToRename by remember { mutableStateOf<WorkPreset?>(null) }
    var newPresetName by remember { mutableStateOf("") }
    var presetToDelete by remember { mutableStateOf<WorkPreset?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var showSkipConfirm by remember { mutableStateOf(false) }
    var presetIdToSelect by remember { mutableStateOf<String?>(null) }

    // 이벤트를 하나로 묶기
    val events = MainScreenEvents(
        onPresetToDeleteChange = { presetToDelete = it },
        onPresetToRenameChange = { preset ->
            newPresetName = preset.name
            presetToRename = preset
        },
        onShowResetConfirmChange = { showResetConfirm = it },
        onShowSkipConfirmChange = { showSkipConfirm = it },
        onSelectPreset = { presetId ->
            if (settingsState.currentWorkId != presetId) {
                presetIdToSelect = presetId
            }
        }
    )

    // ✅ ModalNavigationDrawer 래퍼를 제거하고 다이얼로그와 메인 컨텐츠를 바로 배치

    if (presetIdToSelect != null) {
        ModernConfirmDialog(
            onDismissRequest = { presetIdToSelect = null },
            title = "Work 변경",
            confirmText = "확인",
            onConfirm = {
                settingsViewModel.selectWorkPreset(presetIdToSelect!!) { newSettings ->
                    timerViewModel.reset(newSettings)
                }
                presetIdToSelect = null
            },
            text = "Work를 변경하면 현재 진행상황이 초기화됩니다. 계속하시겠습니까?"
        )
    }

    if (presetToRename != null) {
        ModernConfirmDialog(
            onDismissRequest = { presetToRename = null },
            title = "Work 이름 변경",
            confirmText = "확인",
            confirmButtonEnabled = newPresetName.isNotBlank(),
            onConfirm = {
                settingsViewModel.updateWorkPresetName(presetToRename!!.id, newPresetName)
                presetToRename = null
            },
            content = {
                OutlinedTextField(
                    value = newPresetName,
                    onValueChange = {
                        if (it.length <= 10) {
                            newPresetName = it
                        }
                    },
                    label = { Text("새 이름") },
                    singleLine = true
                )
            }
        )
    }

    if (presetToDelete != null) {
        ModernConfirmDialog(
            onDismissRequest = { presetToDelete = null },
            title = "Work 삭제",
            confirmText = "삭제",
            onConfirm = {
                settingsViewModel.deleteWorkPreset(presetToDelete!!.id) { newSettings ->
                    timerViewModel.reset(newSettings)
                }
                presetToDelete = null
            },
            content = {
                Text(
                    buildAnnotatedString {
                        append("정말로 '")
                        withStyle(
                            style = SpanStyle(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            append(presetToDelete!!.name)
                        }
                        append("' Work를 삭제하시겠습니까?")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
    }

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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        )

        val configuration = LocalConfiguration.current
        when (configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                LandscapeMainScreen(
                    timerViewModel = timerViewModel,
                    settingsViewModel = settingsViewModel,
                    events = events,
                    onNavigateTo = onNavigateTo,
                )
            }
            else -> {
                PortraitMainScreen(
                    timerViewModel = timerViewModel,
                    settingsViewModel = settingsViewModel,
                    events = events,
                    onNavigateTo = onNavigateTo,
                )
            }
        }
    }
}