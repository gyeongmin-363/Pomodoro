package com.malrang.pomodoro.ui.screen

import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.rememberAsyncImagePainter
import com.malrang.pomodoro.dataclass.ui.Screen
import com.malrang.pomodoro.viewmodel.PomodoroViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhitelistScreen(viewModel: PomodoroViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val packageManager = context.packageManager

    // ✅ 사용자가 직접 실행할 수 있는 앱만 필터링하도록 로직 수정
    val installedApps = remember {
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        packageManager.queryIntentActivities(mainIntent, 0)
            .map { it.activityInfo.applicationInfo }
            .distinctBy { it.packageName } // 중복 패키지 제거
            .sortedBy { packageManager.getApplicationLabel(it).toString().lowercase() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("앱 허용 목록 (화이트리스트)") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.showScreen(Screen.Main) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로 가기")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                "공부 중에 사용을 허용할 앱을 선택해주세요.",
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(installedApps, key = { it.packageName }) { appInfo ->
                    val appName = packageManager.getApplicationLabel(appInfo).toString()
                    val packageName = appInfo.packageName
                    val appIcon = appInfo.loadIcon(packageManager)
                    val isChecked = uiState.whitelistedApps.contains(packageName)

                    AppListItem(
                        appName = appName,
                        appIcon = {
                            Image(
                                painter = rememberAsyncImagePainter(model = appIcon),
                                contentDescription = appName,
                                modifier = Modifier.size(40.dp)
                            )
                        },
                        isChecked = isChecked,
                        onCheckedChange = {
                            if (it) {
                                viewModel.addToWhitelist(packageName)
                            } else {
                                viewModel.removeFromWhitelist(packageName)
                            }
                        }
                    )
                }
            }
        }
    }
}


@Composable
fun AppListItem(
    appName: String,
    appIcon: @Composable () -> Unit,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        appIcon()
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = appName,
            modifier = Modifier.weight(1f),
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange
        )
    }
}