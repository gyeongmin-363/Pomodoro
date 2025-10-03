package com.malrang.pomodoro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malrang.pomodoro.dataclass.ui.BlockMode
import com.malrang.pomodoro.dataclass.ui.Settings
import com.malrang.pomodoro.dataclass.ui.WorkPreset
import com.malrang.pomodoro.localRepo.PomodoroRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: Settings = Settings(),
    val workPresets: List<WorkPreset> = emptyList(),
    val currentWorkId: String? = null,
    val useGrassBackground: Boolean = false,
    val editingWorkPreset: WorkPreset? = null,
    val draftSettings: Settings? = null,
    val whitelistedApps: Set<String> = emptySet() // ✅ 화이트리스트 상태 추가
)

class SettingsViewModel(
    private val localRepo: PomodoroRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val presets = localRepo.loadWorkPresets()
            val currentWorkId = localRepo.loadCurrentWorkId() ?: presets.firstOrNull()?.id
            val useGrassBackground = localRepo.loadUseGrassBackground()
            val currentWork = presets.find { it.id == currentWorkId }
            val currentSettings = currentWork?.settings ?: Settings()
            val whitelistedApps = localRepo.loadWhitelistedApps() // ✅ 화이트리스트 로드

            _uiState.update {
                it.copy(
                    settings = currentSettings,
                    workPresets = presets,
                    currentWorkId = currentWorkId,
                    useGrassBackground = useGrassBackground,
                    whitelistedApps = whitelistedApps // ✅ 상태 업데이트
                )
            }
        }
    }

    // ✅ WhitelistViewModel의 기능들을 여기로 통합합니다.
    fun addToWhitelist(packageName: String) {
        viewModelScope.launch {
            val updatedWhitelist = _uiState.value.whitelistedApps + packageName
            localRepo.saveWhitelistedApps(updatedWhitelist)
            _uiState.update { it.copy(whitelistedApps = updatedWhitelist) }
        }
    }

    fun removeFromWhitelist(packageName: String) {
        viewModelScope.launch {
            val updatedWhitelist = _uiState.value.whitelistedApps - packageName
            localRepo.saveWhitelistedApps(updatedWhitelist)
            _uiState.update { it.copy(whitelistedApps = updatedWhitelist) }
        }
    }

    fun initializeDraftSettings() {
        val settingsToEdit = _uiState.value.editingWorkPreset?.settings ?: _uiState.value.settings
        _uiState.update { it.copy(draftSettings = settingsToEdit) }
    }

    fun saveSettingsAndReset(onReset: (Settings) -> Unit) {
        viewModelScope.launch {
            val settingsToSave = _uiState.value.draftSettings ?: return@launch
            val editingId = _uiState.value.editingWorkPreset?.id
            val currentId = _uiState.value.currentWorkId
            val presetIdToUpdate = editingId ?: currentId
            val updatedPresets = _uiState.value.workPresets.map { preset ->
                if (preset.id == presetIdToUpdate) {
                    preset.copy(settings = settingsToSave)
                } else {
                    preset
                }
            }
            localRepo.saveWorkPresets(updatedPresets)
            _uiState.update { it.copy(workPresets = updatedPresets) }
            val newActiveSettings = updatedPresets.find { it.id == currentId }?.settings ?: Settings()
            onReset(newActiveSettings)
            clearDraftSettings()
        }
    }

    fun clearDraftSettings() {
        _uiState.update { it.copy(draftSettings = null, editingWorkPreset = null) }
    }

    private fun updateDraftSettings(transform: Settings.() -> Settings) {
        _uiState.update { it.copy(draftSettings = it.draftSettings?.transform()) }
    }

    fun selectWorkPreset(presetId: String, onReset: (Settings) -> Unit) {
        viewModelScope.launch {
            localRepo.saveCurrentWorkId(presetId)
            val newSettings = _uiState.value.workPresets.find { it.id == presetId }?.settings ?: Settings()
            _uiState.update {
                it.copy(currentWorkId = presetId, settings = newSettings)
            }
            onReset(newSettings)
        }
    }

    fun addWorkPreset() {
        viewModelScope.launch {
            val newPreset = WorkPreset(name = "새 Work", settings = Settings())
            val updatedPresets = _uiState.value.workPresets + newPreset
            localRepo.saveWorkPresets(updatedPresets)
            _uiState.update { it.copy(workPresets = updatedPresets) }
        }
    }

    fun deleteWorkPreset(id: String, onReset: (Settings) -> Unit) {
        viewModelScope.launch {
            val updatedPresets = _uiState.value.workPresets.filterNot { it.id == id }
            localRepo.saveWorkPresets(updatedPresets)
            _uiState.update { it.copy(workPresets = updatedPresets) }
            if (_uiState.value.currentWorkId == id) {
                selectWorkPreset(updatedPresets.firstOrNull()?.id ?: "", onReset)
            }
        }
    }

    fun updateWorkPresetName(id: String, newName: String) {
        viewModelScope.launch {
            val updatedPresets = _uiState.value.workPresets.map {
                if (it.id == id) it.copy(name = newName) else it
            }
            localRepo.saveWorkPresets(updatedPresets)
            _uiState.update { it.copy(workPresets = updatedPresets) }
        }
    }

    fun startEditingWorkPreset(id: String) {
        val preset = _uiState.value.workPresets.find { it.id == id }
        _uiState.update { it.copy(editingWorkPreset = preset) }
    }

    fun stopEditingWorkPreset() {
        _uiState.update { it.copy(editingWorkPreset = null) }
        val currentSettings = _uiState.value.workPresets.find { it.id == _uiState.value.currentWorkId }?.settings ?: Settings()
        _uiState.update { it.copy(settings = currentSettings) }
    }

    fun toggleBackground() {
        viewModelScope.launch {
            val newPreference = !_uiState.value.useGrassBackground
            localRepo.saveUseGrassBackground(newPreference)
            _uiState.update { it.copy(useGrassBackground = newPreference) }
        }
    }

    fun updateBlockMode(mode: BlockMode) = updateDraftSettings { copy(blockMode = mode) }
    fun updateStudyTime(v: Int) = updateDraftSettings { copy(studyTime = v) }
    fun updateShortBreakTime(v: Int) = updateDraftSettings { copy(shortBreakTime = v) }
    fun updateLongBreakTime(v: Int) = updateDraftSettings { copy(longBreakTime = v) }
    fun updateLongBreakInterval(v: Int) = updateDraftSettings { copy(longBreakInterval = v) }
    fun toggleSound(b: Boolean) = updateDraftSettings { copy(soundEnabled = b) }
    fun toggleVibration(b: Boolean) = updateDraftSettings { copy(vibrationEnabled = b) }
    fun toggleAutoStart(b: Boolean) = updateDraftSettings { copy(autoStart = b) }
}