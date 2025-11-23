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
    onPresetSelected: (Settings) -> Unit
) {
    val uiState by settingsViewModel.uiState.collectAsState()

    BackHandler(enabled = uiState.editingWorkPreset != null) {
        settingsViewModel.stopEditingWorkPreset()
    }

    if (uiState.editingWorkPreset != null) {
        SettingsDetailScreen(
            settingsViewModel = settingsViewModel,
            onNavigateTo = onNavigateTo,
            onSave = onSave
        )
    } else {
        WorkListScreen(
            settingsViewModel = settingsViewModel,
            onPresetSelected = onPresetSelected,
            onNavigateTo = onNavigateTo
        )
    }
}