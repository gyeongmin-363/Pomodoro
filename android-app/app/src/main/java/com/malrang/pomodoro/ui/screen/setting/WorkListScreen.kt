package com.malrang.pomodoro.ui.screen.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.malrang.pomodoro.dataclass.ui.Screen
import com.malrang.pomodoro.dataclass.ui.Settings
import com.malrang.pomodoro.dataclass.ui.WorkPreset
import com.malrang.pomodoro.ui.ModernConfirmDialog
import com.malrang.pomodoro.viewmodel.SettingsViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkListScreen(
    settingsViewModel: SettingsViewModel,
    onPresetSelected: (Settings) -> Unit,
    onNavigateTo: (Screen) -> Unit
) {
    val uiState by settingsViewModel.uiState.collectAsState()

    var presetToRename by remember { mutableStateOf<WorkPreset?>(null) }
    var newPresetName by remember { mutableStateOf("") }
    var presetToDelete by remember { mutableStateOf<WorkPreset?>(null) }
    var presetIdToSelect by remember { mutableStateOf<String?>(null) }

    // --- Dialogs (Confirmations) ---
    // (Dialog 내용은 로직이므로 스타일 변화 없음, 다이얼로그 내부 스타일은 ModernConfirmDialog에 의존)
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
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
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
                        withStyle(style = SpanStyle(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        ) {
                            append(presetToDelete!!.name)
                        }
                        append("' Work를 삭제하시겠습니까?")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background, // Neo Background
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "설정",
                        fontWeight = FontWeight.Black, // Extra Bold
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 저장된 Work 목록 (메인 리스트)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "내 프리셋",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TextButton(
                            onClick = { onNavigateTo(Screen.Whitelist) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                "- 앱 차단",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        // 추가 버튼도 스타일링
                        TextButton(
                            onClick = { settingsViewModel.addWorkPreset() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onBackground)
                            Spacer(Modifier.width(2.dp))
                            Text("추가", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                }

                // 리스트 컨테이너 (여기서는 투명하게 처리하고 아이템들이 각각 카드가 됨)
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    uiState.workPresets.forEachIndexed { index, preset ->
                        val isSelected = preset.id == uiState.currentWorkId
                        val isDeleteEnabled = uiState.workPresets.size > 1

                        WorkPresetItem(
                            preset = preset,
                            isSelected = isSelected,
                            isDeleteEnabled = isDeleteEnabled,
                            onSelect = {
                                if (!isSelected) {
                                    presetIdToSelect = preset.id
                                }
                            },
                            onRename = {
                                newPresetName = preset.name
                                presetToRename = preset
                            },
                            onEditSettings = {
                                settingsViewModel.startEditingWorkPreset(preset.id)
                            },
                            onDelete = { presetToDelete = preset }
                        )
                        // Neo 스타일에서는 Divider 대신 아이템 간 간격(Spacer)을 주로 사용하거나 생략
                        // WorkPresetItem 내부에 paddingBottom이 있으므로 Divider 제거
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}