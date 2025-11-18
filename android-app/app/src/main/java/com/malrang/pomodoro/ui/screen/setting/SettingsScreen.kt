package com.malrang.pomodoro.ui.screen.setting

import android.content.Intent
import android.provider.Settings as AndroidSettings
import android.widget.Toast // ✅ Toast import 추가
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malrang.pomodoro.R
import com.malrang.pomodoro.dataclass.ui.BlockMode
import com.malrang.pomodoro.dataclass.ui.Screen
import com.malrang.pomodoro.service.AccessibilityUtils
import com.malrang.pomodoro.ui.ModernConfirmDialog
import com.malrang.pomodoro.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    onNavigateTo: (Screen) -> Unit,
    onSave: () -> Unit
) {
    val uiState by settingsViewModel.uiState.collectAsState()
    val settings = uiState.draftSettings
    val title = uiState.editingWorkPreset?.name ?: "기본 설정"
    val context = LocalContext.current

    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        settingsViewModel.initializeDraftSettings()
    }

    if (settings == null) {
        return
    }

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
            Text("⚙️ $title 설정", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(24.dp))

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

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("다른 앱 차단", fontSize = 18.sp, fontWeight = FontWeight.Medium)
            TextButton(onClick = { onNavigateTo(Screen.Whitelist) }) {
                Text("예외 목록 설정")
            }
        }
        Spacer(Modifier.height(8.dp))

        val blockOptions = listOf(
            BlockMode.NONE to "없음",
            BlockMode.PARTIAL to "부분 차단",
            BlockMode.FULL to "완전 차단"
        )

        Column {
            blockOptions.forEach { (mode, text) ->
                // 중복되는 클릭 로직을 람다로 분리
                val onBlockModeClick = {
                    if (mode != BlockMode.NONE && !AccessibilityUtils.isAccessibilityServiceEnabled(context)) {
                        // ✅ [추가된 부분] 안내 메시지 출력
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
                onClick = {
                    settingsViewModel.clearDraftSettings()
                    onNavigateTo(Screen.Main)
                },
            ) {
                Icon(Icons.Default.Close, "취소")
            }

            Spacer(modifier = Modifier.width(16.dp))

            IconButton(onClick = { showDialog = true }) {
                Icon(painterResource(R.drawable.ic_save),"저장")
            }
        }
    }

    if (showDialog) {
        ModernConfirmDialog(
            onDismissRequest = { showDialog = false },
            title = "저장하시겠습니까?",
            confirmText = "확인",
            onConfirm = {
                onSave()
                showDialog = false
            },
            text = "저장하면 타이머가 초기화됩니다.\n계속 진행할까요?"
        )
    }
}