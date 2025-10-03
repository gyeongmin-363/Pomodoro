package com.malrang.pomodoro.ui.screen.stats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malrang.pomodoro.dataclass.ui.Screen
import com.malrang.pomodoro.ui.theme.SetBackgroundImage
import com.malrang.pomodoro.viewmodel.MainViewModel
import com.malrang.pomodoro.viewmodel.StatsViewModel
import java.time.LocalDate

@Composable
fun StatsScreen(
    statsViewModel: StatsViewModel, // ✅ PomodoroViewModel 대신 StatsViewModel 사용
    mainViewModel: MainViewModel // ✅ 화면 전환을 위해 MainViewModel 추가
) {
    // ✅ statsViewModel에서 상태를 수집
    val state by statsViewModel.uiState.collectAsState()

    var isCalendarExpanded by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    SetBackgroundImage()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📊 통계", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            // ✅ mainViewModel을 사용하여 메인 화면으로 이동
            IconButton(onClick = { mainViewModel.navigateTo(Screen.Main) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "돌아가기",
                    tint = Color.White
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        // dailyStats는 이제 state에서 직접 가져옵니다.
        ExpandableCalendarView(
            dailyStats = state.dailyStats,
            isExpanded = isCalendarExpanded,
            onToggle = { isCalendarExpanded = !isCalendarExpanded },
            selectedDate = selectedDate,
            onDateSelected = { newDate -> selectedDate = newDate }
        )

        AnimatedVisibility(visible = !isCalendarExpanded) {
            Column {
                Spacer(Modifier.height(24.dp))
                WeeklyTimeChart(
                    dailyStats = state.dailyStats,
                    displayDate = selectedDate
                )
            }
        }
    }
}