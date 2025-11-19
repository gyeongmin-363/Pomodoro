package com.malrang.pomodoro.ui.screen.whitelist

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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

/**
 * 설치된 앱 목록을 보여주고 차단 목록(BlockList)을 관리하는 화면입니다.
 * - 체크된(차단된) 앱이 상단에 표시됩니다.
 * - 모두 차단 / 모두 사용(해제) 버튼을 제공합니다.
 */
@Composable
fun WhitelistScreen(
    settingsViewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by settingsViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val packageManager = context.packageManager

    var searchQuery by remember { mutableStateOf("") }

    // 1. 전체 앱 목록 로드 (이름순 정렬)
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

    // 2. 검색 필터링 및 "차단 여부"에 따른 정렬 로직 적용
    // blockedApps(Set)가 변경될 때마다 재정렬하여 상단으로 이동시킴
    val processedApps = remember(searchQuery, uiState.blockedApps, allApps) {
        val filtered = if (searchQuery.isBlank()) {
            allApps
        } else {
            allApps.filter {
                packageManager.getApplicationLabel(it).toString().contains(searchQuery, ignoreCase = true)
            }
        }

        // 차단된 앱(true)을 우선(내림차순) 정렬하고, 그 다음 이름순 정렬
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

            // 상단 헤더
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

            // 검색창
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

            // [추가] 일괄 처리 버튼 영역
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 모두 차단 버튼
                Button(
                    onClick = {
                        // 현재 리스트(검색된 결과 또는 전체)에 있는 앱들을 모두 차단 목록에 추가
                        val packagesToBlock = processedApps.map { it.packageName }
                        settingsViewModel.blockAllApps(packagesToBlock)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("모두 차단")
                }

                // 모두 사용(해제) 버튼
                OutlinedButton(
                    onClick = {
                        settingsViewModel.unblockAllApps()
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

            // 앱 목록 (정렬된 리스트 사용)
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
                        isWhitelisted = isBlocked, // UI 컴포넌트의 변수명은 기존 유지 (의미는 차단 여부)
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