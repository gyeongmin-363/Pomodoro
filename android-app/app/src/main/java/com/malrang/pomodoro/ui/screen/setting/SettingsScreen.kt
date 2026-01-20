package com.malrang.pomodoro.ui.screen.setting

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
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
    onCancel: () -> Unit, // 추가
    onPresetSelected: (Settings) -> Unit
) {
    val uiState by settingsViewModel.uiState.collectAsState()

    // 시스템 뒤로가기 버튼 클릭 시에도 집중 화면으로 이동하도록 수정
    BackHandler(enabled = uiState.editingWorkPreset != null) {
        settingsViewModel.stopEditingWorkPreset()
        onCancel()
    }

    if (uiState.editingWorkPreset != null) {
        SettingsDetailScreen(
            settingsViewModel = settingsViewModel,
            onNavigateTo = onNavigateTo,
            onSave = onSave,
            onCancel = onCancel // 전달
        )
    } else {
        WorkListScreen(
            settingsViewModel = settingsViewModel,
            onPresetSelected = onPresetSelected,
            onNavigateTo = onNavigateTo
        )
    }
}