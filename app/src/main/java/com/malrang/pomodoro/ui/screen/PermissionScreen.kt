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
import com.malrang.pomodoro.viewmodel.PomodoroViewModel

/**
 * 앱 실행에 필요한 권한을 설정하는 화면입니다.
 */
@Composable
fun PermissionScreen(vm: PomodoroViewModel) {
    val uiState by vm.uiState.collectAsState()
    val permissions = uiState.permissions
    val context = LocalContext.current

    val grantedCount = permissions.count { it.isGranted }
    val totalCount = permissions.size

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
                    PermissionItem(permission = permission)
                }
            }

            Button(
                onClick = {
                    // 🔑 아직 허용되지 않은 권한을 하나씩만 처리
                    val nextPermission = permissions.firstOrNull { !it.isGranted }

                    if (nextPermission != null) {
                        when (nextPermission.type) {
                            com.malrang.pomodoro.dataclass.ui.PermissionType.NOTIFICATION -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    ActivityCompat.requestPermissions(
                                        context as Activity,
                                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                                        1001
                                    )
                                }
                            }
                            com.malrang.pomodoro.dataclass.ui.PermissionType.OVERLAY -> {
                                if (!Settings.canDrawOverlays(context)) {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                }
                            }
                            com.malrang.pomodoro.dataclass.ui.PermissionType.USAGE_STATS -> {
                                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                context.startActivity(intent)
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                enabled = grantedCount < totalCount // 모든 권한이 허용되면 버튼 비활성화
            ) {
                val nextPermission = permissions.firstOrNull { !it.isGranted }
                Text(
                    text = if (nextPermission != null) {
                        // 어떤 권한을 요청할 차례인지 표시
                        "${nextPermission.title} 권한 설정하기 ($grantedCount/$totalCount)"
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
fun PermissionItem(permission: PermissionInfo) {
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
            Text(
                text = if (permission.isGranted) "O" else "X",
                color = if (permission.isGranted) Color(0xFF4CAF50) else Color(0xFFF44336),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
