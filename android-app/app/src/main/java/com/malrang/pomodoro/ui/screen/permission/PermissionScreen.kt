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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.malrang.pomodoro.dataclass.ui.PermissionType
import com.malrang.pomodoro.ui.theme.SetBackgroundImage
import com.malrang.pomodoro.ui.theme.Typography
import com.malrang.pomodoro.viewmodel.PermissionUiState

@Composable
fun PermissionScreen(
    permissionUiState: PermissionUiState,
    onPermissionResult: () -> Unit,
    onSetPermissionAttempted: (PermissionType) -> Unit,
    onNavigateTo: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as Activity

    // ✅ 상태 객체에서 직접 값을 가져옵니다.
    val permissions = permissionUiState.permissions
    val sessionAttemptedPermissions = permissionUiState.sessionAttemptedPermissions
    val notificationDenialCount = permissionUiState.notificationDenialCount

    val allPermissionsGranted = if (permissions.isEmpty()) false else permissions.all { it.isGranted }
    val nextPermission = permissions.firstOrNull { !it.isGranted }

    // ✅ 화면이 다시 보일 때마다 (예: 설정에서 돌아왔을 때) 권한 상태를 다시 확인합니다.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onPermissionResult()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // ✅ 모든 권한이 부여되면 자동으로 메인 화면으로 이동합니다.
    LaunchedEffect(allPermissionsGranted) {
        if (allPermissionsGranted) {
            onNavigateTo()
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
                        // ✅ 이벤트를 ViewModel에 위임합니다.
                        onSetPermissionAttempted(nextPermission.type)
                        when (nextPermission.type) {
                            PermissionType.NOTIFICATION -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    // 알림 권한을 2번 이상 거부하면 앱 설정 화면으로 직접 이동
                                    if (notificationDenialCount >= 2) {
                                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                        }
                                        context.startActivity(intent)
                                    } else {
                                        ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
                                    }
                                }
                            }
                            PermissionType.OVERLAY -> {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    "package:${context.packageName}".toUri()
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
                    .padding(top = 16.dp)
                    .border(2.dp, Color.White),
                shape = RoundedCornerShape(0.dp),
                enabled = !allPermissionsGranted && nextPermission != null,
                colors = ButtonDefaults.buttonColors().copy(
                    containerColor = Color(0xFF37A8FF)
                )
            ) {
                Text(
                    text = if (nextPermission != null) {
                        "다음 권한 설정하기"
                    } else {
                        "모든 권AN 설정 완료"
                    },
                    fontSize = 16.sp,
                    style = Typography.bodyLarge
                )
            }
        }
    }
}