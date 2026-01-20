package com.malrang.pomodoro.ui.screen.setting

import android.content.Intent
import android.provider.Settings as AndroidSettings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.malrang.pomodoro.R
import com.malrang.pomodoro.dataclass.ui.BlockMode
import com.malrang.pomodoro.dataclass.ui.Screen
import com.malrang.pomodoro.service.AccessibilityUtils
import com.malrang.pomodoro.ui.ModernConfirmDialog
import com.malrang.pomodoro.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDetailScreen(
    settingsViewModel: SettingsViewModel,
    onNavigateTo: (Screen) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val uiState by settingsViewModel.uiState.collectAsState()
    val settings = uiState.draftSettings
    val editingPreset = uiState.editingWorkPreset
    val context = LocalContext.current
    var showSaveDialog by remember { mutableStateOf(false) }

    // 화면 진입 시 초기화
    LaunchedEffect(Unit) {
        if (editingPreset != null) {
            settingsViewModel.initializeDraftSettings()
        }
    }

    // 시스템 뒤로가기 버튼 처리
    BackHandler {
        settingsViewModel.stopEditingWorkPreset()
        onCancel()
    }

    // 수정할 데이터가 없으면 즉시 복귀
    if (editingPreset == null || settings == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        LaunchedEffect(editingPreset, settings) {
            if (editingPreset == null) onCancel()
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "${editingPreset.name} 상세 설정",
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.background(MaterialTheme.colorScheme.secondary).padding(4.dp)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        settingsViewModel.stopEditingWorkPreset()
                        onCancel()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSecondary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            Surface(
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            settingsViewModel.stopEditingWorkPreset()
                            onCancel()
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("취소", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { showSaveDialog = true },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(painterResource(R.drawable.ic_save), contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("저장", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            SettingSection(title = "타이머 시간") {
                ModernSliderItem(
                    label = "공부 시간",
                    value = settings.studyTime,
                    valueRange = 1f..60f,
                    onValueChange = { settingsViewModel.updateStudyTime(it.toInt()) }
                )
                ModernSliderItem(
                    label = "짧은 휴식",
                    value = settings.shortBreakTime,
                    valueRange = 1f..30f,
                    onValueChange = { settingsViewModel.updateShortBreakTime(it.toInt()) }
                )
                ModernSliderItem(
                    label = "긴 휴식",
                    value = settings.longBreakTime,
                    valueRange = 1f..60f,
                    onValueChange = { settingsViewModel.updateLongBreakTime(it.toInt()) }
                )
                ModernSliderItem(
                    label = "긴 휴식 간격",
                    subLabel = "${settings.longBreakInterval}회 마다",
                    value = settings.longBreakInterval,
                    valueRange = 2f..12f,
                    steps = 9,
                    onValueChange = { settingsViewModel.updateLongBreakInterval(it.toInt()) },
                    unit = ""
                )
            }

            SettingSection(title = "알림 및 피드백") {
                SwitchItem(label = "알림음 사용", checked = settings.soundEnabled, onCheckedChange = { settingsViewModel.toggleSound(it) })
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 2.dp, color = MaterialTheme.colorScheme.outline)
                SwitchItem(label = "진동 사용", checked = settings.vibrationEnabled, onCheckedChange = { settingsViewModel.toggleVibration(it) })
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 2.dp, color = MaterialTheme.colorScheme.outline)
                SwitchItem(label = "자동 시작", description = "휴식/공부 종료 후 다음 타이머 자동 시작", checked = settings.autoStart, onCheckedChange = { settingsViewModel.toggleAutoStart(it) })
            }

            SettingSection(title = "차단 모드") {
                Column(Modifier.padding(16.dp)) {
                    val blockOptions = listOf(BlockMode.NONE to "없음", BlockMode.PARTIAL to "부분", BlockMode.FULL to "완전")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        blockOptions.forEach { (mode, text) ->
                            val isSelected = settings.blockMode == mode
                            Card(
                                modifier = Modifier.weight(1f).height(60.dp).clickable {
                                    if (mode != BlockMode.NONE && !AccessibilityUtils.isAccessibilityServiceEnabled(context)) {
                                        Toast.makeText(context, "[설치된 앱]->[포커스루트] 접근성 권한 허용이 필요합니다.", Toast.LENGTH_LONG).show()
                                        context.startActivity(Intent(AndroidSettings.ACTION_ACCESSIBILITY_SETTINGS))
                                    } else { settingsViewModel.updateBlockMode(mode) }
                                },
                                colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(if (isSelected) 3.dp else 2.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(text = text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp)).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp)).padding(8.dp)) {
                        Text(
                            text = when (settings.blockMode) {
                                BlockMode.NONE -> "앱 사용을 제한하지 않습니다."
                                BlockMode.PARTIAL -> "화이트리스트에 있는 앱만 허용합니다."
                                BlockMode.FULL -> "기본 전화/문자를 제외한 모든 앱을 차단합니다."
                            },
                            style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }

    if (showSaveDialog) {
        ModernConfirmDialog(
            onDismissRequest = { showSaveDialog = false },
            title = "설정 저장",
            confirmText = "저장하기",
            onConfirm = { onSave(); showSaveDialog = false },
            text = "설정을 저장하면 현재 진행 중인 타이머가 초기화됩니다.\n계속 진행하시겠습니까?"
        )
    }
}