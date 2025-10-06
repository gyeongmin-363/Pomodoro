package com.malrang.pomodoro.ui.screen.main

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.malrang.pomodoro.R
import com.malrang.pomodoro.dataclass.ui.Screen
import com.malrang.pomodoro.dataclass.ui.WorkPreset
import com.malrang.pomodoro.ui.PixelArtConfirmDialog
import com.malrang.pomodoro.viewmodel.SettingsViewModel
import com.malrang.pomodoro.viewmodel.TimerViewModel
import kotlinx.coroutines.launch

// 드로어 아이템을 위한 데이터 클래스
private data class DrawerItem(
    val iconRes: Int? = null,
    val imageVector: ImageVector? = null,
    val label: String,
    val screen: Screen? = null,
    val onCustomClick: (() -> Unit)? = null
)

data class MainScreenEvents(
    val onPresetToDeleteChange: (WorkPreset) -> Unit,
    val onPresetToRenameChange: (WorkPreset) -> Unit,
    val onShowResetConfirmChange: (Boolean) -> Unit,
    val onShowSkipConfirmChange: (Boolean) -> Unit,
    val onSelectPreset: (String) -> Unit,
    val onMenuClick: () -> Unit
)

@Composable
fun MainScreen(
    timerViewModel: TimerViewModel,
    settingsViewModel: SettingsViewModel,
    onNavigateTo: (Screen) -> Unit
) {
    val timerState by timerViewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()

    var presetToRename by remember { mutableStateOf<WorkPreset?>(null) }
    var newPresetName by remember { mutableStateOf("") }
    var presetToDelete by remember { mutableStateOf<WorkPreset?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var showSkipConfirm by remember { mutableStateOf(false) }
    var presetIdToSelect by remember { mutableStateOf<String?>(null) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val drawerItems = listOf(
        DrawerItem(iconRes = R.drawable.ic_stats, label = "통계", screen = Screen.Stats),
        DrawerItem(imageVector = Icons.Filled.AccountCircle, label = "계정 설정", screen = Screen.AccountSettings)
    )

    // 이벤트를 하나로 묶기
    val events = MainScreenEvents(
        onPresetToDeleteChange = { presetToDelete = it },
        onPresetToRenameChange = { preset ->
            newPresetName = preset.name
            presetToRename = preset
        },
        onShowResetConfirmChange = { showResetConfirm = it },
        onShowSkipConfirmChange = { showSkipConfirm = it },
        onSelectPreset = { presetId ->
            if (settingsState.currentWorkId != presetId) {
                presetIdToSelect = presetId
            }
        },
        onMenuClick = {
            scope.launch { drawerState.open() }
        }
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerShape = RoundedCornerShape(0.dp),
                modifier = Modifier.fillMaxWidth(0.7f),
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(top = 16.dp)
                    ) {
                        drawerItems.forEach { item ->
                            NavigationDrawerItem(
                                icon = {
                                    if (item.iconRes != null) {
                                        Icon(
                                            painterResource(id = item.iconRes),
                                            contentDescription = item.label,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    } else if (item.imageVector != null) {
                                        Icon(
                                            item.imageVector,
                                            contentDescription = item.label,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                },
                                label = {
                                    Text(
                                        text = item.label,
                                        fontWeight = FontWeight.Normal
                                    )
                                },
                                selected = false,
                                onClick = {
                                    item.screen?.let { onNavigateTo(it) }
                                    item.onCustomClick?.invoke()
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                                    .border(
                                        2.dp,
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    ),
                                shape = RectangleShape,
                                colors = NavigationDrawerItemDefaults.colors(
                                    unselectedContainerColor = MaterialTheme.colorScheme.surface.copy(
                                        alpha = 0.7f
                                    ),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurface,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurface,
                                )
                            )
                        }
                    }
                }
            }
        }
    ) {
        if (presetIdToSelect != null) {
            PixelArtConfirmDialog(
                onDismissRequest = { presetIdToSelect = null },
                title = "Work 변경",
                confirmText = "확인",
                onConfirm = {
                    settingsViewModel.selectWorkPreset(presetIdToSelect!!) { newSettings ->
                        timerViewModel.reset(newSettings)
                    }
                    presetIdToSelect = null
                }
            ) {
                Text(
                    "Work를 변경하면 현재 진행상황이 초기화됩니다. 계속하시겠습니까?",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (presetToRename != null) {
            PixelArtConfirmDialog(
                onDismissRequest = { presetToRename = null },
                title = "Work 이름 변경",
                confirmText = "확인",
                confirmButtonEnabled = newPresetName.isNotBlank(),
                onConfirm = {
                    settingsViewModel.updateWorkPresetName(presetToRename!!.id, newPresetName)
                    presetToRename = null
                }
            ) {
                OutlinedTextField(
                    value = newPresetName,
                    onValueChange = {
                        if (it.length <= 10) {
                            newPresetName = it
                        }
                    },
                    label = { Text("새 이름") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }

        if (presetToDelete != null) {
            PixelArtConfirmDialog(
                onDismissRequest = { presetToDelete = null },
                title = "Work 삭제",
                confirmText = "삭제",
                onConfirm = {
                    settingsViewModel.deleteWorkPreset(presetToDelete!!.id) { newSettings ->
                        timerViewModel.reset(newSettings)
                    }
                    presetToDelete = null
                }
            ) {
                Text(
                    buildAnnotatedString {
                        append("정말로 '")
                        withStyle(
                            style = SpanStyle(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            append(presetToDelete!!.name)
                        }
                        append("' Work를 삭제하시겠습니까?")
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (showSkipConfirm) {
            PixelArtConfirmDialog(
                onDismissRequest = { showSkipConfirm = false },
                title = "세션 건너뛰기",
                confirmText = "확인",
                onConfirm = {
                    timerViewModel.skipSession()
                    showSkipConfirm = false
                }
            ) {
                Text(
                    "현재 세션을 건너뛰시겠습니까?",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { /* widthPx, heightPx are not used */ }
        ) {
            if (showResetConfirm) {
                PixelArtConfirmDialog(
                    onDismissRequest = { showResetConfirm = false },
                    title = "리셋 확인",
                    confirmText = "확인",
                    onConfirm = {
                        timerViewModel.reset(settingsState.settings)
                        showResetConfirm = false
                    }
                ) {
                    Text(
                        "정말 리셋할 건가요?\n세션과 공부시간 등이 모두 초기화됩니다.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            )


            val configuration = LocalConfiguration.current
            when (configuration.orientation) {
                Configuration.ORIENTATION_LANDSCAPE -> {
                    LandscapeMainScreen(
                        timerViewModel = timerViewModel,
                        settingsViewModel = settingsViewModel,
                        events = events,
                        onNavigateTo = onNavigateTo,
                    )
                }

                else -> {
                    PortraitMainScreen(
                        timerViewModel = timerViewModel,
                        settingsViewModel = settingsViewModel,
                        events = events,
                        onNavigateTo = onNavigateTo,
                    )
                }
            }
        }
    }
}