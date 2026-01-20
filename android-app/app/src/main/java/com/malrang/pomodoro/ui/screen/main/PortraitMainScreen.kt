package com.malrang.pomodoro.ui.screen.main

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malrang.pomodoro.R
import com.malrang.pomodoro.dataclass.ui.BlockMode
import com.malrang.pomodoro.dataclass.ui.Mode
import com.malrang.pomodoro.dataclass.ui.Screen
import com.malrang.pomodoro.dataclass.ui.WorkPreset
import com.malrang.pomodoro.ui.ModernConfirmDialog
import com.malrang.pomodoro.viewmodel.SettingsViewModel
import com.malrang.pomodoro.viewmodel.TimerViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PortraitMainScreen(
    timerViewModel: TimerViewModel,
    settingsViewModel: SettingsViewModel,
    events: MainScreenEvents,
    onNavigateTo: (Screen) -> Unit,
    paddingValues: PaddingValues
) {
    val timerState by timerViewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()
    val context = LocalContext.current

    val titleText = when (timerState.currentMode) {
        Mode.STUDY -> "집중 시간"
        Mode.SHORT_BREAK, Mode.LONG_BREAK -> "휴식 시간"
    }

    val listState = rememberLazyListState()

    // --- 상태 관리 ---
    var presetIdToSelect by remember { mutableStateOf<String?>(null) }
    var presetForOptions by remember { mutableStateOf<WorkPreset?>(null) }
    var presetToRename by remember { mutableStateOf<WorkPreset?>(null) }
    var newPresetName by remember { mutableStateOf("") }
    var presetToDelete by remember { mutableStateOf<WorkPreset?>(null) }

    // 1. 단순 선택 시 확인 다이얼로그 (실행 중일 때)
    if (presetIdToSelect != null) {
        ModernConfirmDialog(
            onDismissRequest = { presetIdToSelect = null },
            title = "Work 변경",
            confirmText = "확인",
            onConfirm = {
                settingsViewModel.selectWorkPreset(presetIdToSelect!!) { newSettings ->
                    timerViewModel.reset(newSettings)
                }
                presetIdToSelect = null
            },
            text = "Work를 변경하면 현재 진행상황이 초기화됩니다. 계속하시겠습니까?"
        )
    }

    // 2. 롱클릭 시 나타나는 옵션 다이얼로그 (상세 정보 + 버튼들)
    if (presetForOptions != null) {
        val preset = presetForOptions!!
        ModernConfirmDialog(
            onDismissRequest = { presetForOptions = null },
            title = preset.name,
            confirmText = "닫기",
            onConfirm = { presetForOptions = null },
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // 상세 정보 요약 박스
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            InfoRow("공부 시간", "${preset.settings.studyTime}분")
                            InfoRow("짧은 휴식", "${preset.settings.shortBreakTime}분")
                            InfoRow("긴 휴식", "${preset.settings.longBreakTime}분")
                            InfoRow("차단 모드", when(preset.settings.blockMode) {
                                BlockMode.NONE -> "없음"
                                BlockMode.PARTIAL -> "부분 차단"
                                BlockMode.FULL -> "전체 차단"
                            })
                        }
                    }

                    // 관리 버튼들
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OptionButton("이름 수정", Modifier.weight(1f)) {
                            newPresetName = preset.name
                            presetToRename = preset
                            presetForOptions = null
                        }
                        OptionButton("설정 수정", Modifier.weight(1f)) {
                            settingsViewModel.startEditingWorkPreset(preset.id)
                            onNavigateTo(Screen.Settings)
                            presetForOptions = null
                        }
                    }
                    OptionButton("Work 삭제", Modifier.fillMaxWidth(), isError = true) {
                        if (settingsState.workPresets.size <= 1) {
                            Toast.makeText(context, "최소 한 개의 프리셋은 유지해야 합니다.", Toast.LENGTH_SHORT).show()
                        } else {
                            presetToDelete = preset
                        }
                        presetForOptions = null
                    }
                }
            }
        )
    }

    // 3. 이름 수정 다이얼로그
    if (presetToRename != null) {
        ModernConfirmDialog(
            onDismissRequest = { presetToRename = null },
            title = "이름 변경",
            confirmText = "확인",
            onConfirm = {
                settingsViewModel.updateWorkPresetName(presetToRename!!.id, newPresetName)
                presetToRename = null
            },
            content = {
                OutlinedTextField(
                    value = newPresetName,
                    onValueChange = { if (it.length <= 10) newPresetName = it },
                    label = { Text("새 이름 (최대 10자)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )
    }

    // 4. 삭제 확인 다이얼로그
    if (presetToDelete != null) {
        ModernConfirmDialog(
            onDismissRequest = { presetToDelete = null },
            title = "Work 삭제",
            confirmText = "삭제",
            onConfirm = {
                settingsViewModel.deleteWorkPreset(presetToDelete!!.id) { newSettings ->
                    timerViewModel.reset(newSettings)
                }
                presetToDelete = null
            },
            text = "'${presetToDelete!!.name}'을(를) 정말 삭제하시겠습니까?"
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp)
                .padding(bottom = paddingValues.calculateBottomPadding()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            LazyRow(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(settingsState.workPresets) { preset ->
                    val isSelected = preset.id == settingsState.currentWorkId

                    WorkPresetTab(
                        name = preset.name,
                        studyTime = preset.settings.studyTime,
                        shortBreakTime = preset.settings.shortBreakTime,
                        longBreakTime = preset.settings.longBreakTime,
                        isSelected = isSelected,
                        onClick = {
                            if (isSelected) return@WorkPresetTab
                            if (timerState.isTimerStartedOnce) {
                                presetIdToSelect = preset.id
                            } else {
                                settingsViewModel.selectWorkPreset(preset.id) { newSettings ->
                                    timerViewModel.reset(newSettings)
                                }
                            }
                        },
                        onLongClick = {
                            presetForOptions = preset
                        }
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // 2. 메인 타이머 영역
            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = titleText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "%02d:%02d".format(timerState.timeLeft / 60, timerState.timeLeft % 60),
                    fontSize = 90.sp,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.displayLarge,
                    letterSpacing = 4.sp
                )
            }

            Spacer(Modifier.weight(1f))

            // 3. 하단 컨트롤 영역
            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        buildAnnotatedString {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Medium)) { append("완료한 세션  ") }
                            withStyle(style = SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)) { append("${timerState.totalSessions}") }
                        }
                    )
                }

                Spacer(Modifier.height(20.dp))

                CycleIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    currentMode = timerState.currentMode,
                    totalSessions = timerState.totalSessions,
                    longBreakInterval = settingsState.settings.longBreakInterval,
                    itemsPerRow = 8
                )

                Spacer(Modifier.height(50.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    NeoIconButton(
                        onClick = { events.onShowResetConfirmChange(true) },
                        iconRes = R.drawable.ic_reset,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        size = 56.dp,
                        iconSize = 24.dp
                    )

                    NeoIconButton(
                        onClick = {
                            if (!timerState.isRunning) timerViewModel.startTimer(settingsState.settings)
                            else timerViewModel.pauseTimer()
                        },
                        iconRes = if (!timerState.isRunning) R.drawable.ic_play else R.drawable.ic_pause,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        size = 88.dp,
                        iconSize = 40.dp,
                        shadowOffset = 6.dp
                    )

                    NeoIconButton(
                        onClick = { events.onShowSkipConfirmChange(true) },
                        iconRes = R.drawable.ic_skip,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        size = 56.dp,
                        iconSize = 24.dp
                    )
                }
                Spacer(Modifier.height(48.dp))
            }
        }
    }
}

// --- 보조 컴포넌트들 ---

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun OptionButton(text: String, modifier: Modifier = Modifier, isError: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .background(if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .border(2.dp, if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, fontWeight = FontWeight.Black, color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WorkPresetTab(
    name: String,
    studyTime: Int,
    shortBreakTime: Int,
    longBreakTime: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val shadowColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(bottom = 4.dp, end = 4.dp)
    ) {
        Box(modifier = Modifier.matchParentSize().offset(x = 4.dp, y = 4.dp).background(shadowColor, RoundedCornerShape(12.dp)))
        Box(
            modifier = Modifier
                .background(backgroundColor, RoundedCornerShape(12.dp))
                .border(width = if (isSelected) 3.dp else 2.dp, color = borderColor, shape = RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = name, fontSize = 15.sp, fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold, color = contentColor)
                Text(text = "$studyTime / $shortBreakTime / $longBreakTime", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = contentColor.copy(alpha = 0.8f))
            }
        }
    }
}

@Composable
private fun NeoIconButton(onClick: () -> Unit, iconRes: Int, containerColor: Color, contentColor: Color, size: Dp, iconSize: Dp, shadowOffset: Dp = 4.dp) {
    Box(modifier = Modifier.size(size).clickable(onClick = onClick)) {
        Box(modifier = Modifier.fillMaxSize().offset(x = shadowOffset, y = shadowOffset).background(MaterialTheme.colorScheme.outline, CircleShape))
        Box(
            modifier = Modifier.fillMaxSize().background(containerColor, CircleShape).border(2.dp, MaterialTheme.colorScheme.outline, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(painter = painterResource(id = iconRes), contentDescription = null, tint = contentColor, modifier = Modifier.size(iconSize))
        }
    }
}