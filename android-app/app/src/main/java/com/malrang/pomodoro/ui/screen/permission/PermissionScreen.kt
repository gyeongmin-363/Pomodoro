package com.malrang.pomodoro.ui.screen.permission

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.malrang.pomodoro.dataclass.ui.PermissionType
import com.malrang.pomodoro.dataclass.ui.Screen
import com.malrang.pomodoro.ui.theme.SetBackgroundImage
import com.malrang.pomodoro.ui.theme.Typography
import com.malrang.pomodoro.viewmodel.PomodoroViewModel
import androidx.core.net.toUri

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

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
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
            vm.navigateTo(Screen.Main)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SetBackgroundImage()


        Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "앱 사용을 위한 권한 설정", modifier = Modifier.padding(bottom = 24.dp))

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
                                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        "package:${context.packageName}".toUri())
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
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp).border(2.dp, Color.White),
                shape = RoundedCornerShape(0.dp),
                enabled = !allPermissionsGranted && nextPermission != null,
                colors = ButtonDefaults.buttonColors().copy(
                    containerColor = Color(0xFF37A8FF)
                )
            ) {
                Text(
                    text = if (nextPermission != null) {
                        "권한 설정하기 ($attemptedCount/$totalCount)"
                    } else {
                        "모든 권한 설정 완료"
                    },
                    fontSize = 16.sp,
                    style = Typography.bodyLarge
                )
            }
        }
    }
}

