package com.malrang.pomodoro.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malrang.pomodoro.localRepo.PomodoroRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

enum class BackgroundType { COLOR, IMAGE }

data class BackgroundUiState(
    val customBgColor: Int = android.graphics.Color.BLACK,
    val customTextColor: Int = android.graphics.Color.WHITE,
    val backgroundType: BackgroundType = BackgroundType.COLOR,
    val selectedImagePath: String? = null,
    val availableImages: List<String> = emptyList()
)

class BackgroundViewModel(
    private val localRepo: PomodoroRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackgroundUiState())
    val uiState: StateFlow<BackgroundUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val savedBg = localRepo.loadCustomBgColor() ?: android.graphics.Color.BLACK
            val savedText = localRepo.loadCustomTextColor() ?: android.graphics.Color.WHITE
            val bgTypeString = localRepo.loadBackgroundType()
            val bgType = try { BackgroundType.valueOf(bgTypeString) } catch(e: Exception) { BackgroundType.COLOR }
            val selectedImgPath = localRepo.loadSelectedBgImagePath()

            _uiState.update {
                it.copy(
                    customBgColor = savedBg,
                    customTextColor = savedText,
                    backgroundType = bgType,
                    selectedImagePath = selectedImgPath
                )
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

    // [추가됨] 이미지 삭제 함수
    fun deleteBackgroundImage(context: Context, path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                }

                // 현재 사용 중인 배경 이미지를 삭제한 경우, 배경 타입을 색상으로 되돌림
                if (_uiState.value.selectedImagePath == path) {
                    localRepo.saveSelectedBgImagePath("")
                    setBackgroundType(BackgroundType.COLOR)
                }

                // 목록 갱신
                loadAvailableImages(context)
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
}