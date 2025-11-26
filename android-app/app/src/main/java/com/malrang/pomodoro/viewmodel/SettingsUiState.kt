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
    val blockedApps: Set<String> = emptySet()
)

class SettingsViewModel(
    private val localRepo: PomodoroRepository,
    // [삭제] supabaseRepo 제거
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // [수정] 앱 시작 시 활성 프리셋을 가져옵니다. (없으면 내부적으로 생성 및 첫번째 선택)
            val activePreset = localRepo.getActiveWorkPreset()
            val presets = localRepo.loadWorkPresets()
            val blockedApps = localRepo.loadBlockedApps()

            _uiState.update {
                it.copy(
                    settings = activePreset.settings,
                    workPresets = presets,
                    currentWorkId = activePreset.id,
                    blockedApps = blockedApps
                )
            }
        }
    }

    // [삭제] syncPresetsToServer() 메서드 전체 제거됨

    fun addToBlockList(packageName: String) {
        viewModelScope.launch {
            val updatedBlockList = _uiState.value.blockedApps + packageName
            localRepo.saveBlockedApps(updatedBlockList)
            _uiState.update { it.copy(blockedApps = updatedBlockList) }
        }
    }

    fun removeFromBlockList(packageName: String) {
        viewModelScope.launch {
            val updatedBlockList = _uiState.value.blockedApps - packageName
            localRepo.saveBlockedApps(updatedBlockList)
            _uiState.update { it.copy(blockedApps = updatedBlockList) }
        }
    }

    fun blockAllApps(allPackages: List<String>) {
        viewModelScope.launch {
            val updatedBlockList = _uiState.value.blockedApps + allPackages
            localRepo.saveBlockedApps(updatedBlockList)
            _uiState.update { it.copy(blockedApps = updatedBlockList) }
        }
    }

    fun unblockAllApps(packagesToUnblock: List<String>) {
        viewModelScope.launch {
            val updatedBlockList = _uiState.value.blockedApps - packagesToUnblock.toSet()
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

            val changedPreset = updatedPresets.find { it.id == presetIdToUpdate }
            if (changedPreset != null) {
                localRepo.updateWorkPresets(listOf(changedPreset))
            }

            val newActiveSettings = updatedPresets.find { it.id == currentId }?.settings ?: Settings()
            _uiState.update {
                it.copy(
                    workPresets = updatedPresets,
                    settings = newActiveSettings
                )
            }
            onReset(newActiveSettings)
            clearDraftSettings()

            // [삭제] syncPresetsToServer 호출 제거
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
            localRepo.upsertWorkPresets(listOf(newPreset))
            val updatedPresets = _uiState.value.workPresets + newPreset
            _uiState.update { it.copy(workPresets = updatedPresets) }

            // [삭제] syncPresetsToServer 호출 제거
        }
    }

    fun refreshPresets() {
        viewModelScope.launch {
            val presets = localRepo.loadWorkPresets()
            _uiState.update { it.copy(workPresets = presets) }
        }
    }

    // [수정] 서버 삭제 로직 제거 및 getActiveWorkPreset 활용
    fun deleteWorkPreset(id: String, onReset: (Settings) -> Unit) {
        viewModelScope.launch {
            // 로컬 데이터 삭제
            localRepo.deleteWorkPreset(id)

            // 삭제 후 안전하게 활성 프리셋 재조회 (삭제된 것이 현재 선택된 것이었다면 자동으로 변경됨)
            val activePreset = localRepo.getActiveWorkPreset()
            val updatedPresets = localRepo.loadWorkPresets()

            _uiState.update {
                it.copy(
                    workPresets = updatedPresets,
                    currentWorkId = activePreset.id,
                    settings = activePreset.settings
                )
            }

            // 현재 설정값으로 리셋
            onReset(activePreset.settings)

            // [삭제] 서버 데이터 삭제 로직 제거됨
        }
    }

    fun updateWorkPresetName(id: String, newName: String) {
        viewModelScope.launch {
            val updatedPresets = _uiState.value.workPresets.map {
                if (it.id == id) it.copy(name = newName) else it
            }
            val changedPreset = updatedPresets.find { it.id == id }
            if (changedPreset != null) {
                localRepo.updateWorkPresets(listOf(changedPreset))
            }
            _uiState.update { it.copy(workPresets = updatedPresets) }

            // [삭제] syncPresetsToServer 호출 제거
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