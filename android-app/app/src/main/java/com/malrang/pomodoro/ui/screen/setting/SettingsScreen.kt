package com.malrang.pomodoro.ui.screen.setting

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.malrang.pomodoro.R
import com.malrang.pomodoro.dataclass.ui.BlockMode
import com.malrang.pomodoro.dataclass.ui.Screen
import com.malrang.pomodoro.dataclass.ui.Settings
import com.malrang.pomodoro.dataclass.ui.WorkPreset
import com.malrang.pomodoro.service.AccessibilityUtils
import com.malrang.pomodoro.ui.ModernConfirmDialog
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
            onNavigateTo = onNavigateTo
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkListScreen(
    settingsViewModel: SettingsViewModel,
    onPresetSelected: (Settings) -> Unit,
    onNavigateTo: (Screen) -> Unit
) {
    val uiState by settingsViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as PowerManager }
    val packageName = context.packageName

    var isIgnoringBatteryOptimizations by remember {
        mutableStateOf(powerManager.isIgnoringBatteryOptimizations(packageName))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isIgnoringBatteryOptimizations = powerManager.isIgnoringBatteryOptimizations(packageName)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. 차단 앱 관리 (기존 아이콘 사용: Icons.Default.Settings)
            SettingActionCard(
                title = "차단할 앱 관리",
                description = "공부 중 사용을 제한할 앱을 선택합니다.",
                icon = Icons.Default.Settings,
                onClick = { onNavigateTo(Screen.Whitelist) },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )

            // 2. 배터리 최적화 예외 요청 (기존 아이콘 사용: Icons.Default.Settings)
            if (!isIgnoringBatteryOptimizations) {
                SettingActionCard(
                    title = "배터리 제한 해제 필요",
                    description = "타이머가 멈추지 않으려면 터치하여 제한을 해제해주세요.",
                    icon = Icons.Default.Settings,
                    onClick = {
                        val intent = Intent(AndroidSettings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:$packageName")
                        }
                        context.startActivity(intent)
                    },
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // 3. 현재 선택된 Work
            val currentWork = uiState.workPresets.find { it.id == uiState.currentWorkId }
            if (currentWork != null) {
                Text(
                    "현재 사용 중",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { settingsViewModel.startEditingWorkPreset(currentWork.id) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_play), // 기존 리소스 사용
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentWork.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "탭하여 상세 설정 변경",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        // ArrowForward 대신 기존 ArrowBack을 180도 회전하여 사용
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "이동",
                            modifier = Modifier
                                .graphicsLayer(rotationZ = 180f),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // 4. 저장된 Work 목록
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "내 프리셋 목록",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { settingsViewModel.addWorkPreset() }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("추가")
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    uiState.workPresets.forEachIndexed { index, preset ->
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
                        if (index < uiState.workPresets.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun SettingActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$title 상세 설정", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { settingsViewModel.stopEditingWorkPreset() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 취소 버튼: OutlinedButton + 아이콘(Close)
                    OutlinedButton(
                        onClick = { settingsViewModel.stopEditingWorkPreset() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("취소")
                    }
                    // 저장 버튼: Filled Button + 아이콘(ic_save)
                    Button(
                        onClick = { showSaveDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(painterResource(R.drawable.ic_save), contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("저장")
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // 1. 타이머 설정 섹션 (아이콘 제거)
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

            // 2. 알림 설정 섹션 (아이콘 제거)
            SettingSection(title = "알림 및 피드백") {
                SwitchItem(
                    label = "알림음 사용",
                    checked = settings.soundEnabled,
                    onCheckedChange = { settingsViewModel.toggleSound(it) }
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
                SwitchItem(
                    label = "진동 사용",
                    checked = settings.vibrationEnabled,
                    onCheckedChange = { settingsViewModel.toggleVibration(it) }
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
                SwitchItem(
                    label = "자동 시작",
                    description = "휴식/공부 종료 후 다음 타이머 자동 시작",
                    checked = settings.autoStart,
                    onCheckedChange = { settingsViewModel.toggleAutoStart(it) }
                )
            }

            // 3. 차단 모드 설정 섹션 (아이콘 제거, 텍스트로만 구성)
            SettingSection(title = "차단 모드") {
                Column(Modifier.padding(16.dp)) {
                    val blockOptions = listOf(
                        BlockMode.NONE to "없음",
                        BlockMode.PARTIAL to "부분",
                        BlockMode.FULL to "완전"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        blockOptions.forEach { (mode, text) ->
                            val isSelected = settings.blockMode == mode
                            val containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
                            val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(60.dp) // 높이를 조금 줄임
                                    .clickable {
                                        if (mode != BlockMode.NONE && !AccessibilityUtils.isAccessibilityServiceEnabled(context)) {
                                            Toast.makeText(context, "[설치된 앱]->[포커스루트] 접근성 권한 허용이 필요합니다.", Toast.LENGTH_LONG).show()
                                            val intent = Intent(AndroidSettings.ACTION_ACCESSIBILITY_SETTINGS)
                                            context.startActivity(intent)
                                        } else {
                                            settingsViewModel.updateBlockMode(mode)
                                        }
                                    },
                                colors = CardDefaults.cardColors(containerColor = containerColor),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = text,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = contentColor
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = when (settings.blockMode) {
                            BlockMode.NONE -> "앱 사용을 제한하지 않습니다."
                            BlockMode.PARTIAL -> "화이트리스트에 있는 앱만 허용합니다."
                            BlockMode.FULL -> "기본 전화/문자를 제외한 모든 앱을 차단합니다."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
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
            onConfirm = {
                onSave()
                showSaveDialog = false
            },
            text = "설정을 저장하면 현재 진행 중인 타이머가 초기화됩니다.\n계속 진행하시겠습니까?"
        )
    }
}

// --- Helper Composables without Icons ---

@Composable
fun SettingSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            content()
        }
    }
}

@Composable
fun ModernSliderItem(
    label: String,
    subLabel: String? = null,
    value: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    steps: Int = 0,
    unit: String = "분"
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subLabel ?: "$value$unit",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(4.dp))
        Slider(
            value = value.toFloat(),
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.height(24.dp)
        )
    }
}

@Composable
fun SwitchItem(
    label: String,
    description: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            if (description != null) {
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}