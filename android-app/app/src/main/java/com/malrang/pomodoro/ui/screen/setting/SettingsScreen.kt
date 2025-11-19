package com.malrang.pomodoro.ui.screen.setting

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malrang.pomodoro.R
import com.malrang.pomodoro.dataclass.ui.BlockMode
import com.malrang.pomodoro.dataclass.ui.Screen
import com.malrang.pomodoro.dataclass.ui.Settings
import com.malrang.pomodoro.dataclass.ui.WorkPreset
import com.malrang.pomodoro.service.AccessibilityUtils
import com.malrang.pomodoro.ui.ModernConfirmDialog
import com.malrang.pomodoro.ui.screen.main.WorkPresetItem
import com.malrang.pomodoro.viewmodel.SettingsViewModel
import android.provider.Settings as AndroidSettings

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
            onNavigateTo = onNavigateTo // [추가] 네비게이션 전달
        )
    }
}

@Composable
fun WorkListScreen(
    settingsViewModel: SettingsViewModel,
    onPresetSelected: (Settings) -> Unit,
    onNavigateTo: (Screen) -> Unit // [추가]
) {
    val uiState by settingsViewModel.uiState.collectAsState()

    var presetToRename by remember { mutableStateOf<WorkPreset?>(null) }
    var newPresetName by remember { mutableStateOf("") }
    var presetToDelete by remember { mutableStateOf<WorkPreset?>(null) }
    var presetIdToSelect by remember { mutableStateOf<String?>(null) }

    if (presetIdToSelect != null) {
        ModernConfirmDialog(
            onDismissRequest = { presetIdToSelect = null },
            title = "Work 변경",
            confirmText = "확인",
            onConfirm = {
                settingsViewModel.selectWorkPreset(presetIdToSelect!!) { newSettings ->
                    onPresetSelected(newSettings)
                }
                presetIdToSelect = null
            },
            text = "Work를 변경하면 현재 진행상황이 초기화됩니다. 계속하시겠습니까?"
        )
    }

    if (presetToRename != null) {
        ModernConfirmDialog(
            onDismissRequest = { presetToRename = null },
            title = "Work 이름 변경",
            confirmText = "확인",
            confirmButtonEnabled = newPresetName.isNotBlank(),
            onConfirm = {
                settingsViewModel.updateWorkPresetName(presetToRename!!.id, newPresetName)
                presetToRename = null
            },
            content = {
                OutlinedTextField(
                    value = newPresetName,
                    onValueChange = { if (it.length <= 10) newPresetName = it },
                    label = { Text("새 이름") },
                    singleLine = true
                )
            }
        )
    }

    if (presetToDelete != null) {
        ModernConfirmDialog(
            onDismissRequest = { presetToDelete = null },
            title = "Work 삭제",
            confirmText = "삭제",
            onConfirm = {
                settingsViewModel.deleteWorkPreset(presetToDelete!!.id) { newSettings ->
                    onPresetSelected(newSettings)
                }
                presetToDelete = null
            },
            content = {
                Text(
                    buildAnnotatedString {
                        append("정말로 '")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)) {
                            append(presetToDelete!!.name)
                        }
                        append("' Work를 삭제하시겠습니까?")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("설정", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))

        // [추가] 차단 앱 관리 (전역 설정이므로 목록 상단에 배치)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateTo(Screen.Whitelist) }, // Route 이름은 유지하되 내용은 차단 목록
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "차단할 앱 관리",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = "공부 중 사용을 제한할 앱을 선택합니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))

        // 1. 현재 선택된 Work
        val currentWork = uiState.workPresets.find { it.id == uiState.currentWorkId }
        if (currentWork != null) {
            Text("현재 선택된 Work", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { settingsViewModel.startEditingWorkPreset(currentWork.id) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(painterResource(R.drawable.ic_play), contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = currentWork.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "탭하여 상세 설정",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // 2. 저장된 Work 목록
        Text("저장된 Work 목록", fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column {
                uiState.workPresets.forEach { preset ->
                    WorkPresetItem(
                        preset = preset,
                        isSelected = preset.id == uiState.currentWorkId,
                        onSelect = {
                            if (uiState.currentWorkId != preset.id) {
                                presetIdToSelect = preset.id
                            }
                        },
                        onItemClick = { settingsViewModel.startEditingWorkPreset(preset.id) },
                        onRename = {
                            newPresetName = preset.name
                            presetToRename = preset
                        },
                        onEditSettings = { settingsViewModel.startEditingWorkPreset(preset.id) },
                        onDelete = { presetToDelete = preset }
                    )
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                }

                TextButton(
                    onClick = { settingsViewModel.addWorkPreset() },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Work 추가")
                    Spacer(Modifier.width(4.dp))
                    Text("새 Work 추가")
                }
            }
        }
    }
}

@Composable
fun SettingsDetailScreen(
    settingsViewModel: SettingsViewModel,
    onNavigateTo: (Screen) -> Unit,
    onSave: () -> Unit
) {
    val uiState by settingsViewModel.uiState.collectAsState()
    val settings = uiState.draftSettings
    val title = uiState.editingWorkPreset?.name ?: "설정"
    val context = LocalContext.current
    var showSaveDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        settingsViewModel.initializeDraftSettings()
    }

    if (settings == null) return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { settingsViewModel.stopEditingWorkPreset() }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "뒤로가기",
                    modifier = Modifier.width(24.dp).height(24.dp)
                )
            }

            Text("⚙️ $title 상세 설정", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))

        Text("타이머 설정", fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))

        Text("공부 시간: ${settings.studyTime}분")
        Slider(
            value = settings.studyTime.toFloat(),
            onValueChange = { settingsViewModel.updateStudyTime(it.toInt()) },
            valueRange = 1f..60f
        )

        Text("짧은 휴식 시간: ${settings.shortBreakTime}분")
        Slider(
            value = settings.shortBreakTime.toFloat(),
            onValueChange = { settingsViewModel.updateShortBreakTime(it.toInt()) },
            valueRange = 1f..30f
        )

        Text("긴 휴식 시간: ${settings.longBreakTime}분")
        Slider(
            value = settings.longBreakTime.toFloat(),
            onValueChange = { settingsViewModel.updateLongBreakTime(it.toInt()) },
            valueRange = 1f..60f
        )

        Text("긴 휴식 간격: ${settings.longBreakInterval}회 마다")
        Slider(
            value = settings.longBreakInterval.toFloat(),
            onValueChange = { settingsViewModel.updateLongBreakInterval(it.toInt()) },
            valueRange = 2f..12f,
            steps = 9
        )
        Spacer(Modifier.height(24.dp))

        Text("알림 설정", fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = settings.soundEnabled, onCheckedChange = { settingsViewModel.toggleSound(it) })
            Text("알림음 사용")
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = settings.vibrationEnabled, onCheckedChange = { settingsViewModel.toggleVibration(it) })
            Text("진동 사용")
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = settings.autoStart, onCheckedChange = { settingsViewModel.toggleAutoStart(it) })
            Text("자동 시작")
        }
        Spacer(Modifier.height(24.dp))

        // [변경] 여기서 "다른 앱 차단" 목록 설정 버튼 제거함 (목록 화면으로 이동)
        Text("차단 모드", fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))

        val blockOptions = listOf(
            BlockMode.NONE to "없음",
            BlockMode.PARTIAL to "부분 차단",
            BlockMode.FULL to "완전 차단"
        )

        Column {
            blockOptions.forEach { (mode, text) ->
                val onBlockModeClick = {
                    if (mode != BlockMode.NONE && !AccessibilityUtils.isAccessibilityServiceEnabled(context)) {
                        Toast.makeText(context, "[설치된 앱]->[포커스루트]를 찾아 접근성 권한을 허용해주세요.", Toast.LENGTH_LONG).show()
                        val intent = Intent(AndroidSettings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    } else {
                        settingsViewModel.updateBlockMode(mode)
                    }
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = (settings.blockMode == mode),
                            onClick = onBlockModeClick
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (settings.blockMode == mode),
                        onClick = onBlockModeClick
                    )
                    Text(text = text, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(
                onClick = { settingsViewModel.stopEditingWorkPreset() },
            ) {
                Icon(Icons.Default.Close, "취소")
            }

            Spacer(modifier = Modifier.width(16.dp))

            IconButton(onClick = { showSaveDialog = true }) {
                Icon(painterResource(R.drawable.ic_save),"저장")
            }
        }
    }

    if (showSaveDialog) {
        ModernConfirmDialog(
            onDismissRequest = { showSaveDialog = false },
            title = "저장하시겠습니까?",
            confirmText = "확인",
            onConfirm = {
                onSave()
                showSaveDialog = false
            },
            text = "저장하면 타이머가 초기화됩니다.\n계속 진행할까요?"
        )
    }
}