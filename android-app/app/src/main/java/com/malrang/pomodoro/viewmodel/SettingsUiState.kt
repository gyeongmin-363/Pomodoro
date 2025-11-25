package com.malrang.pomodoro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malrang.pomodoro.dataclass.ui.BlockMode
import com.malrang.pomodoro.dataclass.ui.Settings
import com.malrang.pomodoro.dataclass.ui.WorkPreset
import com.malrang.pomodoro.localRepo.PomodoroRepository
import com.malrang.pomodoro.networkRepo.SupabaseProvider
import com.malrang.pomodoro.networkRepo.SupabaseRepository
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
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
    private val supabaseRepo: SupabaseRepository
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

    // [수정됨] 자동 동기화 설정 확인 로직 추가
    private fun syncPresetsToServer() {
        val userId = SupabaseProvider.client.auth.currentUserOrNull()?.id ?: return
        val currentPresets = _uiState.value.workPresets

        viewModelScope.launch(Dispatchers.IO) {
            // 자동 동기화가 꺼져있다면 서버 전송 중단
            if (!localRepo.isAutoSyncEnabled()) {
                return@launch
            }

            try {
                supabaseRepo.upsertWorkPresets(userId, currentPresets)
            } catch (e: Exception) {
                e.printStackTrace()
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

            syncPresetsToServer() // 내부에서 isAutoSyncEnabled 체크
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
            localRepo.upsertNewWorkPresets(listOf(newPreset))
            val updatedPresets = _uiState.value.workPresets + newPreset
            _uiState.update { it.copy(workPresets = updatedPresets) }

            syncPresetsToServer() // 내부에서 isAutoSyncEnabled 체크
        }
    }

    fun refreshPresets() {
        viewModelScope.launch {
            val presets = localRepo.loadWorkPresets()
            _uiState.update { it.copy(workPresets = presets) }
        }
    }

    // [수정됨] 삭제 시에도 자동 동기화 설정 확인
    fun deleteWorkPreset(id: String, onReset: (Settings) -> Unit) {
        viewModelScope.launch {
            // 1. 로컬 데이터 삭제 (무조건 실행)
            localRepo.deleteWorkPreset(id)
            val updatedPresets = _uiState.value.workPresets.filterNot { it.id == id }
            _uiState.update { it.copy(workPresets = updatedPresets) }
            if (_uiState.value.currentWorkId == id) {
                selectWorkPreset(updatedPresets.firstOrNull()?.id ?: "", onReset)
            }

            // 2. 서버 데이터 삭제 (자동 동기화 켜진 경우에만 실행)
            val userId = SupabaseProvider.client.auth.currentUserOrNull()?.id

            // 여기서 isAutoSyncEnabled 체크
            if (userId != null && localRepo.isAutoSyncEnabled()) {
                launch(Dispatchers.IO) {
                    try {
                        supabaseRepo.deleteWorkPreset(userId, id)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
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

            syncPresetsToServer() // 내부에서 isAutoSyncEnabled 체크
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