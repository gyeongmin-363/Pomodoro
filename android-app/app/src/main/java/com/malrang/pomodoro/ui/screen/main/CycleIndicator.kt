package com.malrang.pomodoro.ui.screen.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.malrang.pomodoro.dataclass.ui.Mode

@Composable
fun CycleIndicator(
    modifier: Modifier = Modifier,
    currentMode: Mode,
    totalSessions: Int,
    longBreakInterval: Int,
    borderColor: Color,
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
                modifier = Modifier.padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                rowItems.forEach { (index, mode) ->
                    val color = when (mode) {
                        Mode.STUDY -> Color(0xFFC62828)
                        Mode.SHORT_BREAK -> Color(0xFF2E7D32)
                        Mode.LONG_BREAK -> Color(0xFF1565C0)
                    }

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(16.dp)
                            .border(1.dp, borderColor, RectangleShape)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            when {
                                index < currentIndex -> drawRect(color = color)
                                else -> drawRect(color = color.copy(alpha=0.3f))
                            }
                        }
                    }
                }
            }
        }
    }
}