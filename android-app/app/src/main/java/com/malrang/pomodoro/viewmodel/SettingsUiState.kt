package com.malrang.pomodoro.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malrang.pomodoro.dataclass.ui.BlockMode
import com.malrang.pomodoro.dataclass.ui.Settings
import com.malrang.pomodoro.dataclass.ui.WorkPreset
import com.malrang.pomodoro.localRepo.PomodoroRepository
import com.malrang.pomodoro.networkRepo.SupabaseProvider
import com.malrang.pomodoro.networkRepo.SupabaseRepository
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

enum class BackgroundType { COLOR, IMAGE }

data class SettingsUiState(
    val settings: Settings = Settings(),
    val workPresets: List<WorkPreset> = emptyList(),
    val currentWorkId: String? = null,
    val editingWorkPreset: WorkPreset? = null,
    val draftSettings: Settings? = null,
    val blockedApps: Set<String> = emptySet(),
    val customBgColor: Int = android.graphics.Color.BLACK,
    val customTextColor: Int = android.graphics.Color.WHITE,
    val backgroundType: BackgroundType = BackgroundType.COLOR,
    val selectedImagePath: String? = null,
    val availableImages: List<String> = emptyList()
)

class SettingsViewModel(
    private val localRepo: PomodoroRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // [추가] Supabase Repository 직접 생성 (ViewModelFactory 수정 없이 사용하기 위함)
    private val supabaseRepo = SupabaseRepository(SupabaseProvider.client.postgrest, SupabaseProvider.client.storage)

    init {
        viewModelScope.launch {
            val presets = localRepo.loadWorkPresets()
            val currentWorkId = localRepo.loadCurrentWorkId() ?: presets.firstOrNull()?.id
            val currentWork = presets.find { it.id == currentWorkId }
            val currentSettings = currentWork?.settings ?: Settings()
            val blockedApps = localRepo.loadBlockedApps()

            val savedBg = localRepo.loadCustomBgColor() ?: android.graphics.Color.BLACK
            val savedText = localRepo.loadCustomTextColor() ?: android.graphics.Color.WHITE
            val bgTypeString = localRepo.loadBackgroundType()
            val bgType = try { BackgroundType.valueOf(bgTypeString) } catch(e: Exception) { BackgroundType.COLOR }
            val selectedImgPath = localRepo.loadSelectedBgImagePath()

            _uiState.update {
                it.copy(
                    settings = currentSettings,
                    workPresets = presets,
                    currentWorkId = currentWorkId,
                    blockedApps = blockedApps,
                    customBgColor = savedBg,
                    customTextColor = savedText,
                    backgroundType = bgType,
                    selectedImagePath = selectedImgPath
                )
            }
        }
    }

    // [추가] 현재 프리셋 목록을 서버와 동기화하는 헬퍼 함수
    private fun syncPresetsToServer() {
        val userId = SupabaseProvider.client.auth.currentUserOrNull()?.id ?: return
        // 현재 UI 상태의 프리셋 목록을 가져옴
        val currentPresets = _uiState.value.workPresets

        viewModelScope.launch(Dispatchers.IO) {
            try {
                supabaseRepo.upsertWorkPresets(userId, currentPresets)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadAvailableImages(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val dir = File(context.filesDir, "bg_images")
            if (!dir.exists()) dir.mkdirs()

            val files = dir.listFiles()?.sortedByDescending { it.lastModified() }?.map { it.absolutePath } ?: emptyList()
            _uiState.update { it.copy(availableImages = files) }
        }
    }

    fun addBackgroundImage(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dir = File(context.filesDir, "bg_images")
                if (!dir.exists()) dir.mkdirs()

                val newFileName = "bg_${UUID.randomUUID()}.jpg"
                val newFile = File(dir, newFileName)

                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(newFile).use { output ->
                        input.copyTo(output)
                    }
                }

                val files = dir.listFiles()?.sortedBy { it.lastModified() }
                if (files != null && files.size > 20) {
                    val filesToDelete = files.take(files.size - 20)
                    filesToDelete.forEach { it.delete() }
                }

                loadAvailableImages(context)
                selectBackgroundImage(newFile.absolutePath)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun selectBackgroundImage(path: String) {
        viewModelScope.launch {
            localRepo.saveSelectedBgImagePath(path)
            localRepo.saveBackgroundType(BackgroundType.IMAGE.name)
            _uiState.update {
                it.copy(
                    selectedImagePath = path,
                    backgroundType = BackgroundType.IMAGE
                )
            }
        }
    }

    fun setBackgroundType(type: BackgroundType) {
        viewModelScope.launch {
            localRepo.saveBackgroundType(type.name)
            _uiState.update { it.copy(backgroundType = type) }
        }
    }

    fun updateCustomColors(bgColor: Int, textColor: Int) {
        viewModelScope.launch {
            localRepo.saveCustomColors(bgColor, textColor)
            _uiState.update {
                it.copy(
                    customBgColor = bgColor,
                    customTextColor = textColor
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

            // [추가] 설정 변경 후 서버 동기화
            syncPresetsToServer()
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
            localRepo.insertNewWorkPresets(listOf(newPreset))
            val updatedPresets = _uiState.value.workPresets + newPreset
            _uiState.update { it.copy(workPresets = updatedPresets) }

            // [추가] 프리셋 추가 후 서버 동기화
            syncPresetsToServer()
        }
    }

    fun refreshPresets() {
        viewModelScope.launch {
            val presets = localRepo.loadWorkPresets()
            _uiState.update { it.copy(workPresets = presets) }
        }
    }

    fun deleteWorkPreset(id: String, onReset: (Settings) -> Unit) {
        viewModelScope.launch {
            localRepo.deleteWorkPreset(id)
            val updatedPresets = _uiState.value.workPresets.filterNot { it.id == id }
            _uiState.update { it.copy(workPresets = updatedPresets) }
            if (_uiState.value.currentWorkId == id) {
                selectWorkPreset(updatedPresets.firstOrNull()?.id ?: "", onReset)
            }

            // [추가] 삭제 후 서버 동기화 (delete 요청)
            val userId = SupabaseProvider.client.auth.currentUserOrNull()?.id
            if (userId != null) {
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

            // [추가] 이름 변경 후 서버 동기화
            syncPresetsToServer()
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