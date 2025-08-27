package com.malrang.pomodoro.ui.screen

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.malrang.pomodoro.dataclass.ui.PermissionInfo
import com.malrang.pomodoro.dataclass.ui.PermissionType
import com.malrang.pomodoro.dataclass.ui.Screen
import com.malrang.pomodoro.viewmodel.PomodoroViewModel

/**
 * 앱 실행에 필요한 권한을 설정하는 화면입니다.
 */
@Composable
fun PermissionScreen(vm: PomodoroViewModel) {
    val uiState by vm.uiState.collectAsState()
    val sessionAttemptedPermissions by vm.sessionAttemptedPermissions.collectAsState()
    val notificationPermanentlyDenied by vm.notificationPermanentlyDenied.collectAsState()

    val permissions = uiState.permissions
    val context = LocalContext.current
    val activity = context as Activity

    // 🔽 [수정] '시도 횟수'와 '총 권한 수'를 계산합니다.
    val attemptedCount = sessionAttemptedPermissions.size
    val totalCount = permissions.size
    val grantedCount = permissions.count { it.isGranted }

    val nextPermissionToAttempt = permissions.firstOrNull { !sessionAttemptedPermissions.contains(it.type) }
    val nextPermission = nextPermissionToAttempt ?: permissions.firstOrNull { !it.isGranted }

    // 🔽 [추가] 시도 횟수가 총 권한 수와 같아지면 메인 화면으로 자동 이동하는 효과
    LaunchedEffect(attemptedCount, totalCount) {
        // 요청할 권한이 있고, 모든 권한에 대한 시도가 완료되었다면 이동
        if (totalCount > 0 && attemptedCount == totalCount) {
            vm.showScreen(Screen.Main)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "앱 사용을 위한 권한 설정",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(permissions) { permission ->
                    PermissionItem(
                        permission = permission,
                        hasBeenAttempted = sessionAttemptedPermissions.contains(permission.type)
                    )
                }
            }

            Button(
                onClick = {
                    if (nextPermission != null) {
                        vm.setPermissionAttemptedInSession(nextPermission.type)
                        when (nextPermission.type) {
                            PermissionType.NOTIFICATION -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val permissionString = Manifest.permission.POST_NOTIFICATIONS
                                    val wasAlreadyAttemptedInSession = sessionAttemptedPermissions.contains(PermissionType.NOTIFICATION)
                                    val shouldGoToSettings = notificationPermanentlyDenied ||
                                            (wasAlreadyAttemptedInSession &&
                                                    !ActivityCompat.shouldShowRequestPermissionRationale(activity, permissionString))

                                    if (shouldGoToSettings) {
                                        vm.setNotificationPermanentlyDenied()
                                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                        }
                                        context.startActivity(intent)
                                    } else {
                                        ActivityCompat.requestPermissions(activity, arrayOf(permissionString), 1001)
                                    }
                                }
                            }
                            PermissionType.OVERLAY -> {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            }
                            PermissionType.USAGE_STATS -> {
                                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                context.startActivity(intent)
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                enabled = grantedCount < totalCount
            ) {
                Text(
                    text = if (nextPermission != null) {
                        // 🔽 [수정] 버튼 텍스트의 카운터를 '시도 횟수'로 변경합니다.
                        "${nextPermission.title} 권한 설정하기 ($attemptedCount/$totalCount)"
                    } else {
                        "모든 권한 허용됨"
                    },
                    fontSize = 16.sp
                )
            }
        }
    }
}

/**
 * 개별 권한 항목을 표시하는 컴포저블
 */
@Composable
fun PermissionItem(permission: PermissionInfo, hasBeenAttempted: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = permission.title, fontWeight = FontWeight.Bold)
                Text(text = permission.description, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.width(16.dp))
            if (hasBeenAttempted) {
                Text(
                    text = if (permission.isGranted) "O" else "X",
                    color = if (permission.isGranted) Color(0xFF4CAF50) else Color(0xFFF44336),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}