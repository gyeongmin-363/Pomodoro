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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.malrang.pomodoro.dataclass.ui.PermissionInfo
import com.malrang.pomodoro.dataclass.ui.PermissionType
import com.malrang.pomodoro.dataclass.ui.Screen
import com.malrang.pomodoro.viewmodel.PomodoroViewModel

@Composable
fun PermissionScreen(vm: PomodoroViewModel) {
    val uiState by vm.uiState.collectAsState()
    val sessionAttemptedPermissions by vm.sessionAttemptedPermissions.collectAsState()
    val notificationDenialCount by vm.notificationDenialCount.collectAsState()

    val permissions = uiState.permissions
    val context = LocalContext.current
    val activity = context as Activity

    val attemptedCount = sessionAttemptedPermissions.size
    val totalCount = permissions.size
    val allPermissionsGranted = if (totalCount == 0) false else permissions.all { it.isGranted }
    val nextPermission = permissions.firstOrNull { it.type !in sessionAttemptedPermissions }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                vm.onPermissionRequestResult(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(attemptedCount, totalCount) {
        if (totalCount > 0 && attemptedCount >= totalCount) {
            vm.showScreen(Screen.Main)
        }
    }

    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "앱 사용을 위한 권한 설정", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 24.dp))

            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(permissions) { permission ->
                    PermissionItem(permission = permission, hasBeenAttempted = sessionAttemptedPermissions.contains(permission.type))
                }
            }

            Button(
                onClick = {
                    if (nextPermission != null) {
                        if (nextPermission.type == PermissionType.NOTIFICATION) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                if (notificationDenialCount >= 2) {
                                    // 🔽 [수정] 설정으로 이동할 때도 '시도'한 것으로 기록하여 무한 루프를 방지합니다.
                                    vm.setPermissionAttemptedInSession(nextPermission.type)
                                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    }
                                    context.startActivity(intent)
                                } else {
                                    vm.setPermissionAttemptedInSession(nextPermission.type)
                                    ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
                                }
                            }
                        } else {
                            vm.setPermissionAttemptedInSession(nextPermission.type)
                            when (nextPermission.type) {
                                PermissionType.OVERLAY -> {
                                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                                    context.startActivity(intent)
                                }
                                PermissionType.USAGE_STATS -> {
                                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                    context.startActivity(intent)
                                }
                                else -> {}
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                enabled = !allPermissionsGranted && nextPermission != null
            ) {
                Text(
                    text = if (nextPermission != null) {
                        "${nextPermission.title} 권한 설정하기 ($attemptedCount/$totalCount)"
                    } else {
                        "모든 권한 설정 완료"
                    },
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun PermissionItem(permission: PermissionInfo, hasBeenAttempted: Boolean) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
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