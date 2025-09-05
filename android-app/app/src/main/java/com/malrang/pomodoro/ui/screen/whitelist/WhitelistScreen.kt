package com.malrang.pomodoro.ui.screen.whitelist

import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
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

//    // ✅ 시스템 뒤로가기 버튼을 눌렀을 때 SettingsScreen으로 이동하도록 설정
//    BackPressMove {
//        viewModel.navigateTo(Screen.Settings)
//    }

    Scaffold(
        containerColor = Color(0xFF1E1B4B),
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
                Text("앱 허용 목록 (전체)", fontSize = 16.sp)

                IconButton(onClick = { viewModel.navigateTo(Screen.Settings) }) {
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
                modifier = Modifier.padding(bottom = 8.dp)
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
        PixelSwitch(
            checked = isChecked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun PixelSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 64.dp,
    height: Dp = 32.dp,
    borderWidth: Dp = 2.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val thumbSize = height - borderWidth * 2

    // 엄지(thumb)의 위치를 애니메이션으로 처리
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) width - thumbSize - borderWidth * 2 else 0.dp,
        animationSpec = tween(durationMillis = 150)
    )

    // 각 부분의 색상을 애니메이션으로 처리
    val trackColor by animateColorAsState(
        targetValue = if (checked) Color(0xFF4CAF50) else Color(0xFF757575), // 활성/비활성 트랙 색상
        animationSpec = tween(durationMillis = 150)
    )
    val trackBorderColor by animateColorAsState(
        targetValue = if (checked) Color(0xFF388E3C) else Color(0xFF424242), // 활성/비활성 트랙 테두리 색상
        animationSpec = tween(durationMillis = 150)
    )
    val thumbColor by animateColorAsState(
        targetValue = Color(0xFFE0E0E0), // 활성/비활성 엄지 색상
        animationSpec = tween(durationMillis = 150)
    )
    val thumbBorderColor by animateColorAsState(
        targetValue = Color(0xFF9E9E9E), // 활성/비활성 엄지 테두리 색상
        animationSpec = tween(durationMillis = 150)
    )

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .border(width = borderWidth, color = trackBorderColor, shape = RectangleShape)
            .background(color = trackColor, shape = RectangleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null // 클릭 시 물결 효과 제거
            ) {
                onCheckedChange(!checked)
            }
            .padding(borderWidth),
        contentAlignment = Alignment.CenterStart
    ) {
        // 네모난 픽셀 스타일 엄지 (Thumb)
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(thumbSize)
                .border(width = borderWidth, color = thumbBorderColor, shape = RectangleShape)
                .background(color = thumbColor, shape = RectangleShape)
        )
    }
}