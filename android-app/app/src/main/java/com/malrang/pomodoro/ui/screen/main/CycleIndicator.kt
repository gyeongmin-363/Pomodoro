package com.malrang.pomodoro.ui.screen.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.malrang.pomodoro.dataclass.ui.Mode

@Composable
fun CycleIndicator(
    modifier: Modifier = Modifier,
    currentMode: Mode,
    totalSessions: Int,
    longBreakInterval: Int,
    borderColor: Color, // 호환성을 위해 유지하되, 내부적으로는 테마의 outline 색상 사용 권장
    itemsPerRow: Int
) {
    if (longBreakInterval <= 0) return

    val cycleSequence = remember(longBreakInterval) {
        buildList {
            for (i in 1 until longBreakInterval) {
                add(Mode.STUDY)
                add(Mode.SHORT_BREAK)
            }
            add(Mode.STUDY)
            add(Mode.LONG_BREAK)
        }
    }

    // 현재 진행 중인 인덱스 계산
    val currentIndex = remember(currentMode, totalSessions, longBreakInterval) {
        val cycleLength = longBreakInterval * 2
        if (currentMode == Mode.STUDY && totalSessions > 0 && totalSessions % longBreakInterval == 0) {
            cycleLength
        } else {
            val cyclePosition = (totalSessions - 1).coerceAtLeast(0) % longBreakInterval
            when (currentMode) {
                Mode.STUDY -> (totalSessions % longBreakInterval) * 2
                else -> cyclePosition * 2 + 1
            }
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        cycleSequence.withIndex().chunked(itemsPerRow).forEach { rowItems ->
            Row(
                modifier = Modifier.padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                rowItems.forEach { (index, mode) ->
                    // 색상 정의 (테마 색상 활용)
                    val baseColor = when (mode) {
                        Mode.STUDY -> MaterialTheme.colorScheme.primary // Blue
                        Mode.SHORT_BREAK -> MaterialTheme.colorScheme.tertiary // Green
                        Mode.LONG_BREAK -> MaterialTheme.colorScheme.secondary // Pink
                    }

                    val isCurrent = index == currentIndex
                    val isPast = index < currentIndex

                    // 스타일 결정
                    val size = if (isCurrent) 16.dp else 12.dp
                    val borderWidth = 1.5.dp
                    val outlineColor = MaterialTheme.colorScheme.outline

                    // 채우기 색상: 과거/현재는 색상 표시, 미래는 비움(흰색/배경색)
                    val fillColor = if (isPast || isCurrent) baseColor else Color.Transparent

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(size)
                            .clip(CircleShape)
                            .background(fillColor)
                            .border(borderWidth, outlineColor, CircleShape)
                    )
                }
            }
        }
    }
}