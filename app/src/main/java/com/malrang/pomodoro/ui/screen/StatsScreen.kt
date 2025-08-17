package com.malrang.pomodoro.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malrang.pomodoro.data.Screen
import com.malrang.pomodoro.viewmodel.PomodoroViewModel
import java.time.LocalDate

@Composable
fun StatsScreen(vm: PomodoroViewModel) {
    val state by vm.uiState.collectAsState()
    val last7 = remember(state.dailyStats) {
        val today = LocalDate.now()
        (0..6).map { i -> today.minusDays((6 - i).toLong()) }.map { d ->
            val key = d.toString()
            state.dailyStats[key]?.studySessions ?: 0
        }
    }
    val labels = (0..6).map { LocalDate.now().minusDays((6 - it).toLong()) }.map { it.dayOfWeek.name.take(3) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1B4B))
            .padding(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("📊 통계", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Button(onClick = { vm.showScreen(Screen.Main) }) { Text("← 돌아가기") }
        }
        Spacer(Modifier.height(16.dp))
        BarChart(values = last7, labels = labels)
    }
}

@Composable
fun BarChart(values: List<Int>, labels: List<String>, modifier: Modifier = Modifier) {
    val maxV = (values.maxOrNull() ?: 1).coerceAtLeast(1)
    val barW = 28.dp
    val spacing = 16.dp
    Column(modifier) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(horizontal = 12.dp)
        ) {
            val w = size.width
            val h = size.height
            val barWidthPx = barW.toPx()
            val spacingPx = spacing.toPx()
            val totalW = values.size * barWidthPx + (values.size - 1) * spacingPx
            var x = (w - totalW) / 2f
            values.forEach { v ->
                val barH = (v.toFloat() / maxV) * (h * 0.9f)
//                drawRect(
//                    topLeft = Offset(x, h - barH),
//                    size = Size(barWidthPx, barH),
//                    brush = TODO(),
//                )
                x += barWidthPx + spacingPx
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            labels.forEach { Text(it, color = Color(0xFFBDB5FF)) }
        }
    }
}
