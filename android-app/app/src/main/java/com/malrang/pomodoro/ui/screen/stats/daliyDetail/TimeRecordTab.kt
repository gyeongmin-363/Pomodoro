package com.malrang.pomodoro.ui.screen.stats.daliyDetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.malrang.pomodoro.dataclass.ui.DailyStat

@Composable
fun TimeRecordTab(dailyStat: DailyStat) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val allWorks = (dailyStat.studyTimeByWork?.keys ?: emptySet()) + (dailyStat.breakTimeByWork?.keys ?: emptySet())

        if (allWorks.isEmpty()) {
            EmptyStateMessage("기록된 활동이 없습니다.")
        } else {
            allWorks.forEach { work ->
                val study = dailyStat.studyTimeByWork?.get(work) ?: 0
                val breaks = dailyStat.breakTimeByWork?.get(work) ?: 0

                if (study > 0 || breaks > 0) {
                    NeoRecordCard(
                        title = work,
                        studyTime = study,
                        breakTime = breaks
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun NeoRecordCard(
    title: String,
    studyTime: Int,
    breakTime: Int
) {
    val shape = RoundedCornerShape(12.dp)

    Box(modifier = Modifier.fillMaxWidth()) {
        // Shadow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp) // Approximate height or use matchParentSize with proper layout
                .offset(x = 4.dp, y = 4.dp)
                .background(MaterialTheme.colorScheme.outline, shape)
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, shape)
                .border(2.dp, MaterialTheme.colorScheme.outline, shape)
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 시간 표시 Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 공부 시간 Box
                NeoTimeBox(
                    label = "📚 공부",
                    time = studyTime,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.weight(1f)
                )

                // 휴식 시간 Box
                NeoTimeBox(
                    label = "☕ 휴식",
                    time = breakTime,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun NeoTimeBox(
    label: String,
    time: Int,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(color.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "${time}분",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}