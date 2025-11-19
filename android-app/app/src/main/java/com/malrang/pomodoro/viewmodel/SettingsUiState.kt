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
            val blockedApps = localRepo.loadBlockedApps()

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

    // 모두 차단 (목록 추가)
    fun blockAllApps(allPackages: List<String>) {
        viewModelScope.launch {
            val updatedBlockList = _uiState.value.blockedApps + allPackages
            localRepo.saveBlockedApps(updatedBlockList)
            _uiState.update { it.copy(blockedApps = updatedBlockList) }
        }
    }

    // 모두 사용 (선택된 앱들만 차단 해제)
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

            // UI 상태 업데이트를 위한 리스트 계산
            val updatedPresets = _uiState.value.workPresets.map { preset ->
                if (preset.id == presetIdToUpdate) {
                    preset.copy(settings = settingsToSave)
                } else {
                    preset
                }
            }

            // [수정] DB 업데이트: 전체 저장(saveWorkPresets) 대신 변경된 항목만 updateWorkPresets 호출
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

            // [수정] DB에 새 프리셋 추가 (IGNORE 전략 사용)
            localRepo.insertNewWorkPresets(listOf(newPreset))

            val updatedPresets = _uiState.value.workPresets + newPreset
            _uiState.update { it.copy(workPresets = updatedPresets) }
        }
    }

    fun deleteWorkPreset(id: String, onReset: (Settings) -> Unit) {
        viewModelScope.launch {
            // [수정] DB에서 프리셋 삭제 (Soft Delete)
            localRepo.deleteWorkPreset(id)

            val updatedPresets = _uiState.value.workPresets.filterNot { it.id == id }
            _uiState.update { it.copy(workPresets = updatedPresets) }

            // 현재 선택된 프리셋을 삭제했다면 다른 프리셋 선택
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

            // [수정] 이름이 변경된 프리셋만 DB 업데이트
            val changedPreset = updatedPresets.find { it.id == id }
            if (changedPreset != null) {
                localRepo.updateWorkPresets(listOf(changedPreset))
            }

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