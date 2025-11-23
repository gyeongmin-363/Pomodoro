package com.malrang.pomodoro.ui.screen.stats.month

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.malrang.pomodoro.dataclass.ui.Screen
import com.malrang.pomodoro.viewmodel.StatsViewModel
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    statsViewModel: StatsViewModel,
    onNavigateTo: (Screen) -> Unit,
    onNavigateToDetail: (LocalDate) -> Unit
) {
    val state by statsViewModel.uiState.collectAsState()
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    val monthlyTotalMinutes = remember(selectedDate, state.dailyStats) {
        val targetMonth = YearMonth.from(selectedDate)
        state.dailyStats.values.filter { stat ->
            try {
                val statDate = LocalDate.parse(stat.date)
                YearMonth.from(statDate) == targetMonth
            } catch (e: Exception) {
                false
            }
        }.sumOf { it.totalStudyTimeInMinutes }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("통계", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { onNavigateTo(Screen.Main) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 월간 요약 카드
            MonthlySummaryCard(monthlyTotalMinutes = monthlyTotalMinutes)

            // 캘린더 섹션
            MonthlyStatsCalendar(
                dailyStats = state.dailyStats,
                selectedDate = selectedDate,
                onDateSelected = { newDate -> selectedDate = newDate },
                onDetailRequested = onNavigateToDetail
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}



