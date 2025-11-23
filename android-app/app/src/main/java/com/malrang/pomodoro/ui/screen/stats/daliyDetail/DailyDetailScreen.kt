package com.malrang.pomodoro.ui.screen.stats.daliyDetail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("${date.monthValue}월 ${date.dayOfMonth}일 기록", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // 상단 탭 (BottomBar -> TabRow 변경)
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("시간 기록") },
                    icon = { Icon(Icons.Default.List, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("체크리스트") },
                    icon = { Icon(Icons.Default.Check, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("회고") },
                    icon = { Icon(Icons.Default.Edit, contentDescription = null) }
                )
            }

            // 탭 콘텐츠
            Box(modifier = Modifier.weight(1f)) {
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