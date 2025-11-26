package com.malrang.pomodoro.ui.screen.stats.daliyDetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.malrang.pomodoro.dataclass.ui.DailyStat
import com.malrang.pomodoro.viewmodel.StatsViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyDetailScreen(
    dateString: String?,
    statsViewModel: StatsViewModel,
    onNavigateBack: () -> Unit
) {
    val date = try {
        LocalDate.parse(dateString)
    } catch (e: Exception) {
        LocalDate.now()
    }

    val uiState by statsViewModel.uiState.collectAsState()
    val dailyStat = uiState.dailyStats[date.toString()] ?: DailyStat(date.toString())
    var selectedTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background, // NeoBackground
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    // 타이틀 배지 스타일
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                            .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "${date.monthValue}월 ${date.dayOfMonth}일 기록",
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                },
                navigationIcon = {
                    // 뒤로가기 버튼 스타일
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                            .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            Spacer(modifier = Modifier.height(16.dp))

            // Custom Neo Tab Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(50.dp)
            ) {
                NeoTabItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = "시간 기록",
                    icon = Icons.Default.List,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp)) // 탭 사이 간격
                NeoTabItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = "체크리스트",
                    icon = Icons.Default.Check,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                NeoTabItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = "회고",
                    icon = Icons.Default.Edit,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 탭 콘텐츠
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (selectedTab) {
                    0 -> TimeRecordTab(dailyStat)
                    1 -> ChecklistTab(dailyStat, statsViewModel)
                    2 -> RetrospectTab(dailyStat) { newRetrospect ->
                        statsViewModel.saveRetrospect(dailyStat.date, newRetrospect)
                        scope.launch { snackbarHostState.showSnackbar("회고가 저장되었습니다.") }
                    }
                }
            }
        }
    }
}

// Custom Tab Component
@Composable
fun NeoTabItem(
    selected: Boolean,
    onClick: () -> Unit,
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val borderWidth = if (selected) 2.dp else 1.5.dp
    val elevation = if (selected) 4.dp else 0.dp // 선택된 탭만 그림자 처리 등 차별화 가능

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .border(borderWidth, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 공간 부족 시 아이콘만 보이거나 텍스트 줄임 처리
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}