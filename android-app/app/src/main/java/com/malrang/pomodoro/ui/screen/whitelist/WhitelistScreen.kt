package com.malrang.pomodoro.ui.screen.whitelist

import android.content.Intent
import androidx.compose.foundation.Image
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malrang.pomodoro.viewmodel.SettingsViewModel

/**
 * 설치된 앱 목록을 보여주고 화이트리스트를 관리하는 화면입니다.
 * 모든 시스템 앱을 포함하며, 검색 기능이 있습니다.
 */
@Composable
fun WhitelistScreen(
    settingsViewModel: SettingsViewModel, // ✅ PomodoroViewModel 대신 SettingsViewModel 사용
    onNavigateBack: () -> Unit
) {
    // ✅ settingsViewModel에서 상태를 가져옵니다.
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
        containerColor = Color.Transparent,
        contentColor = Color.White
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("앱 허용 목록 (전체)", fontSize = 16.sp, color = Color.White)

                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로 가기", tint = Color.White)
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
                colors = TextFieldDefaults.colors().copy(
                    unfocusedLabelColor = Color.White,
                    focusedLabelColor = Color.White,
                    focusedLeadingIconColor = Color.White,
                    unfocusedLeadingIconColor = Color.White,
                    cursorColor = Color.White,
                    focusedTextColor = Color.White,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.White,
                    unfocusedIndicatorColor = Color.White,
                )
            )

            Text(
                "공부 중에 사용을 허용할 앱을 선택해주세요.",
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp),
                color = Color.White
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredApps, key = { it.packageName }) { appInfo ->
                    val appName = packageManager.getApplicationLabel(appInfo).toString()
                    val packageName = appInfo.packageName
                    val appIcon = appInfo.loadIcon(packageManager)
                    val isChecked = uiState.whitelistedApps.contains(packageName)

                    AppListItem(
                        appName = appName,
                        appIcon = appIcon,
                        isWhitelisted = isChecked,
                        onWhitelistToggle = {
                            // ✅ settingsViewModel의 함수를 호출합니다.
                            if (it) {
                                settingsViewModel.addToWhitelist(packageName)
                            } else {
                                settingsViewModel.removeFromWhitelist(packageName)
                            }
                        }
                    )
                }
            }
        }
    }
}