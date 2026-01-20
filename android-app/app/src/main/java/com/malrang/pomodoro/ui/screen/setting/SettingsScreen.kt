package com.malrang.pomodoro.ui.screen.setting

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.malrang.pomodoro.dataclass.ui.Screen
import com.malrang.pomodoro.dataclass.ui.Settings
import com.malrang.pomodoro.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    onNavigateTo: (Screen) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onPresetSelected: (Settings) -> Unit
) {
    val uiState by settingsViewModel.uiState.collectAsState()

    // 시스템 뒤로가기 대응
    BackHandler(enabled = uiState.editingWorkPreset != null) {
        settingsViewModel.stopEditingWorkPreset()
        onCancel()
    }

    if (uiState.editingWorkPreset != null) {
        // [최적화] WorkListScreen을 제거하고 바로 상세 설정으로 연결
        SettingsDetailScreen(
            settingsViewModel = settingsViewModel,
            onNavigateTo = onNavigateTo,
            onSave = onSave,
            onCancel = onCancel
        )
    } else {
        // 편집 대상이 없을 경우(직접 접근 등) 자동으로 이전 화면으로 복귀
        LaunchedEffect(Unit) {
            onCancel()
        }
    }
}