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
    val editingWorkPreset: WorkPreset? = null,
    val draftSettings: Settings? = null,
    val blockedApps: Set<String> = emptySet() // 차단 목록
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
            val currentWork = presets.find { it.id == currentWorkId }
            val currentSettings = currentWork?.settings ?: Settings()
            val blockedApps = localRepo.loadBlockedApps() // 차단 목록 로드

            _uiState.update {
                it.copy(
                    settings = currentSettings,
                    workPresets = presets,
                    currentWorkId = currentWorkId,
                    blockedApps = blockedApps
                )
            }
        }
    }

    // 개별 추가
    fun addToBlockList(packageName: String) {
        viewModelScope.launch {
            val updatedBlockList = _uiState.value.blockedApps + packageName
            localRepo.saveBlockedApps(updatedBlockList)
            _uiState.update { it.copy(blockedApps = updatedBlockList) }
        }
    }

    // 개별 제거
    fun removeFromBlockList(packageName: String) {
        viewModelScope.launch {
            val updatedBlockList = _uiState.value.blockedApps - packageName
            localRepo.saveBlockedApps(updatedBlockList)
            _uiState.update { it.copy(blockedApps = updatedBlockList) }
        }
    }

    // [추가] 모두 차단 (목록에 있는 모든 패키지 추가)
    fun blockAllApps(allPackages: List<String>) {
        viewModelScope.launch {
            // 기존 목록에 새 목록을 합침 (Set이라 중복 자동 제거)
            val updatedBlockList = _uiState.value.blockedApps + allPackages
            localRepo.saveBlockedApps(updatedBlockList)
            _uiState.update { it.copy(blockedApps = updatedBlockList) }
        }
    }

    // [추가] 모두 사용 (차단 목록 초기화)
    fun unblockAllApps() {
        viewModelScope.launch {
            val updatedBlockList = emptySet<String>()
            localRepo.saveBlockedApps(updatedBlockList)
            _uiState.update { it.copy(blockedApps = updatedBlockList) }
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

            val newActiveSettings = updatedPresets.find { it.id == currentId }?.settings ?: Settings()
            _uiState.update {
                it.copy(
                    workPresets = updatedPresets,
                    settings = newActiveSettings
                )
            }
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

    fun updateBlockMode(mode: BlockMode) = updateDraftSettings { copy(blockMode = mode) }
    fun updateStudyTime(v: Int) = updateDraftSettings { copy(studyTime = v) }
    fun updateShortBreakTime(v: Int) = updateDraftSettings { copy(shortBreakTime = v) }
    fun updateLongBreakTime(v: Int) = updateDraftSettings { copy(longBreakTime = v) }
    fun updateLongBreakInterval(v: Int) = updateDraftSettings { copy(longBreakInterval = v) }
    fun toggleSound(b: Boolean) = updateDraftSettings { copy(soundEnabled = b) }
    fun toggleVibration(b: Boolean) = updateDraftSettings { copy(vibrationEnabled = b) }
    fun toggleAutoStart(b: Boolean) = updateDraftSettings { copy(autoStart = b) }
}