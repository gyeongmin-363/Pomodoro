package com.malrang.pomodoro.ui.screen.setting

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

    // 배터리 최적화 관련 로직은 "일반" 탭 제거로 인해 UI에서 제외되었으나,
    // 기능적 필요에 따라 백그라운드 체크용으로 남겨두거나 삭제할 수 있습니다.
    // 화면 간결화를 위해 UI 표시 부분은 제거되었습니다.

    var presetToRename by remember { mutableStateOf<WorkPreset?>(null) }
    var newPresetName by remember { mutableStateOf("") }
    var presetToDelete by remember { mutableStateOf<WorkPreset?>(null) }
    var presetIdToSelect by remember { mutableStateOf<String?>(null) }

    // --- Dialogs (Confirmations) ---
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("설정", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
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
            // "일반" 섹션 및 "현재 사용 중" 섹션 제거됨

            // 저장된 Work 목록 (메인 리스트)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "내 프리셋",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    // 우측 상단 액션 버튼 그룹 (- 앱 차단, + 추가)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // 앱 차단 바로가기 버튼 (붉은색 텍스트)
                        TextButton(
                            onClick = { onNavigateTo(Screen.Whitelist) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                "- 앱 차단",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        // 프리셋 추가 버튼
                        TextButton(
                            onClick = { settingsViewModel.addWorkPreset() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(2.dp))
                            Text("추가", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        uiState.workPresets.forEachIndexed { index, preset ->
                            val isSelected = preset.id == uiState.currentWorkId

                            WorkPresetItem(
                                preset = preset,
                                isSelected = isSelected,
                                onSelect = {
                                    // 선택되지 않은 아이템을 클릭했을 때만 선택 다이얼로그 트리거
                                    if (!isSelected) {
                                        presetIdToSelect = preset.id
                                    }
                                },
                                onRename = {
                                    newPresetName = preset.name
                                    presetToRename = preset
                                },
                                onEditSettings = {
                                    // 설정 버튼(톱니바퀴)을 눌러야만 상세 설정 진입
                                    settingsViewModel.startEditingWorkPreset(preset.id)
                                },
                                onDelete = { presetToDelete = preset }
                            )

                            if (index < uiState.workPresets.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
