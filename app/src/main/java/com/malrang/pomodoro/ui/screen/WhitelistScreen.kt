package com.malrang.pomodoro.ui.screen

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
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

/**
 * 설치된 앱 목록을 보여주고 화이트리스트를 관리하는 화면입니다.
 * 모든 시스템 앱을 포함하며, 검색 기능이 있습니다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhitelistScreen(viewModel: PomodoroViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val packageManager = context.packageManager

    // ✅ 검색어 상태를 관리합니다.
    var searchQuery by remember { mutableStateOf("") }

// ✅ 시스템 앱을 포함한 모든 설치된 앱 목록을 가져옵니다.
    val allApps = remember {
        packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            },
            0
        )
            .map { it.activityInfo.applicationInfo }
            .sortedBy {
                packageManager.getApplicationLabel(it).toString().lowercase()
            }
    }




    // ✅ 검색어에 따라 앱 목록을 필터링합니다.
    val filteredApps = remember(searchQuery, allApps) {
        if (searchQuery.isBlank()) {
            allApps
        } else {
            allApps.filter {
                packageManager.getApplicationLabel(it).toString().contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("앱 허용 목록 (전체)") },
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
                .padding(horizontal = 16.dp)
        ) {
            // ✅ 검색창 UI 추가
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                label = { Text("앱 이름 검색") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "검색 아이콘")
                },
                singleLine = true
            )

            Text(
                "공부 중에 사용을 허용할 앱을 선택해주세요.",
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                // ✅ 필터링된 앱 목록을 사용합니다.
                items(filteredApps, key = { it.packageName }) { appInfo ->
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