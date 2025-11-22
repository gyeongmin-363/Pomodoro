package com.malrang.pomodoro.ui.screen.permission

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.malrang.pomodoro.dataclass.ui.PermissionType
import com.malrang.pomodoro.viewmodel.PermissionUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionScreen(
    permissionUiState: PermissionUiState,
    onPermissionResult: () -> Unit,
    onSetPermissionAttempted: (PermissionType) -> Unit,
    onNavigateTo: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val permissions = permissionUiState.permissions
    val sessionAttemptedPermissions = permissionUiState.sessionAttemptedPermissions
    val notificationDenialCount = permissionUiState.notificationDenialCount

    val allPermissionsGranted = if (permissions.isEmpty()) false else permissions.all { it.isGranted }
    val nextPermission = permissions.firstOrNull { !it.isGranted }

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

    LaunchedEffect(allPermissionsGranted) {
        if (allPermissionsGranted) {
            onNavigateTo()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("권한 설정", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // 하단 고정 버튼 영역
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding() // 네비게이션 바 겹침 방지
            ) {
                Button(
                    onClick = {
                        if (nextPermission != null) {
                            onSetPermissionAttempted(nextPermission.type)
                            when (nextPermission.type) {
                                PermissionType.NOTIFICATION -> {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        if (notificationDenialCount >= 2) {
                                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                            }
                                            context.startActivity(intent)
                                        } else {
                                            activity?.let {
                                                ActivityCompat.requestPermissions(
                                                    it,
                                                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                                                    1001
                                                )
                                            }
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
                        .height(56.dp), // 버튼 높이 확보
                    enabled = !allPermissionsGranted && nextPermission != null,
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(
                        text = nextPermission?.let { "${it.title} 허용하기" } ?: "모든 권한 설정 완료",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            Text(
                text = "앱의 원활한 기능을 위해\n다음 권한들이 필요합니다.",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "필수 권한을 허용하지 않으면\n일부 기능을 사용할 수 없습니다.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(32.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(permissions) { permission ->
                    // 현재 차례인 권한인지 확인 (아직 허용 안됨 && 순서상 첫 번째)
                    val isNext = nextPermission == permission

                    PermissionItem(
                        permission = permission,
                        hasBeenAttempted = sessionAttemptedPermissions.contains(permission.type),
                        isNextAction = isNext
                    )
                }
            }
        }
    }
}