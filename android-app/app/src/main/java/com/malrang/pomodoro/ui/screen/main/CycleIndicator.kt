package com.malrang.pomodoro.ui.screen.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
    borderColor: Color, // 호환성을 위해 파라미터 유지하되 내부에서는 테마 색상 활용 권장
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
                modifier = Modifier.padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                rowItems.forEach { (index, mode) ->
                    // 색상 정의 (파스텔 톤으로 조금 더 부드럽게)
                    val baseColor = when (mode) {
                        Mode.STUDY -> Color(0xFFEF5350) // Red 400
                        Mode.SHORT_BREAK -> Color(0xFF66BB6A) // Green 400
                        Mode.LONG_BREAK -> Color(0xFF42A5F5) // Blue 400
                    }

                    // 상태에 따른 투명도 및 크기 조정
                    val isPast = index < currentIndex
                    val isCurrent = index == currentIndex

                    val alpha = if (isPast || isCurrent) 1f else 0.2f
                    val scale = if (isCurrent) 1.2f else 1.0f

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(width = 12.dp, height = 12.dp) // 캡슐형 점
                            .clip(CircleShape)
                            .background(baseColor.copy(alpha = alpha))
                    )
                }
            }
        }
    }
}