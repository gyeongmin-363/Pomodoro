package com.malrang.pomodoro.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malrang.pomodoro.dataclass.ui.Screen
import com.malrang.pomodoro.viewmodel.PomodoroViewModel
import java.time.LocalDate
import java.util.Locale

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
    // 요일 레이블을 한국어로 변경
    val labels = (0..6).map { LocalDate.now().minusDays((6 - it).toLong()) }.map {
        it.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.KOREAN)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1B4B))
            .padding(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📊 통계", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            IconButton(onClick = { vm.showScreen(Screen.Main) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "돌아가기",
                    tint = Color.White
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        // BarChart에 labels와 last7 값을 전달
        BarChart(values = last7, labels = labels)
    }
}

@Composable
fun BarChart(values: List<Int>, labels: List<String>, modifier: Modifier = Modifier) {
    val maxV = (values.maxOrNull() ?: 1).coerceAtLeast(1)
    var animationPlayed by remember { mutableStateOf(false) }
    val animatedValues by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 1000)
    )

    // 화면 진입 시 애니메이션 실행
    LaunchedEffect(Unit) { animationPlayed = true }

    Column(modifier) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(horizontal = 12.dp)
        ) {
            val w = size.width
            val h = size.height
            val barCount = values.size
            val barW = 28.dp.toPx()
            val spacingPx = (w - (barCount * barW)) / (barCount - 1)

            values.forEachIndexed { i, v ->
                val barHeight = (v.toFloat() / maxV) * (h * 0.9f) * animatedValues
                val x = (i * (barW + spacingPx))
                val barColor = when {
                    v >= 5 -> Color.Green
                    v >= 3 -> Color.Yellow
                    else -> Color.Red
                }

                drawRect(
                    color = barColor,
                    topLeft = Offset(x, h - barHeight),
                    size = Size(barW, barHeight)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        // Layout 컴포저블을 사용하여 막대그래프와 레이블을 정확하게 정렬
        Layout(
            content = { labels.forEach { Text(it, color = Color(0xFFBDB5FF)) } },
            modifier = Modifier.fillMaxWidth()
        ) { measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints) }
            layout(constraints.maxWidth, placeables.maxOfOrNull { it.height } ?: 0) {
                val barCount = values.size
                val barW = 28.dp.toPx()
                val spacingPx = (constraints.maxWidth - (barCount * barW)) / (barCount - 1)

                placeables.forEachIndexed { i, placeable ->
                    val x = (i * (barW + spacingPx) + barW / 2 - placeable.width / 2).toInt()
                    placeable.placeRelative(x = x, y = 0)
                }
            }
        }
    }
}