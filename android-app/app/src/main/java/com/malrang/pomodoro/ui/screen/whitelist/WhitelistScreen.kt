package com.malrang.pomodoro.ui.screen.whitelist

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.malrang.pomodoro.viewmodel.SettingsViewModel

@Composable
fun WhitelistScreen(
    settingsViewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by settingsViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val packageManager = context.packageManager

    var searchQuery by remember { mutableStateOf("") }

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

    val processedApps = remember(searchQuery, uiState.blockedApps, allApps) {
        val filtered = if (searchQuery.isBlank()) {
            allApps
        } else {
            allApps.filter {
                packageManager.getApplicationLabel(it).toString().contains(searchQuery, ignoreCase = true)
            }
        }

        filtered.sortedWith(
            compareByDescending<android.content.pm.ApplicationInfo> {
                uiState.blockedApps.contains(it.packageName)
            }.thenBy {
                packageManager.getApplicationLabel(it).toString().lowercase()
            }
        )
    }

    Scaffold(
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("차단 앱 관리", style = MaterialTheme.typography.titleLarge)

                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로 가기")
                }
            }

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
                singleLine = true,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 모두 차단 버튼
                Button(
                    onClick = {
                        val packagesToBlock = processedApps.map { it.packageName }
                        settingsViewModel.blockAllApps(packagesToBlock)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("모두 차단")
                }

                // [수정] 모두 사용 버튼: 현재 필터링된 앱들만 해제
                OutlinedButton(
                    onClick = {
                        val packagesToUnblock = processedApps.map { it.packageName }
                        settingsViewModel.unblockAllApps(packagesToUnblock)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("모두 사용")
                }
            }

            Text(
                "체크된 앱은 공부 중에 실행이 차단됩니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(processedApps, key = { it.packageName }) { appInfo ->
                    val appName = packageManager.getApplicationLabel(appInfo).toString()
                    val packageName = appInfo.packageName
                    val appIcon = appInfo.loadIcon(packageManager)

                    val isBlocked = uiState.blockedApps.contains(packageName)

                    AppListItem(
                        appName = appName,
                        appIcon = appIcon,
                        isWhitelisted = isBlocked,
                        onWhitelistToggle = { shouldBlock ->
                            if (shouldBlock) {
                                settingsViewModel.addToBlockList(packageName)
                            } else {
                                settingsViewModel.removeFromBlockList(packageName)
                            }
                        }
                    )
                }
            }
        }
    }
}