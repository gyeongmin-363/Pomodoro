package com.malrang.pomodoro.ui.screen.stats

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DayCell(
    date: LocalDate,
    studyTimeMinutes: Int,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit // ✅ 롱클릭 추가
) {
    val dayColor = when (date.dayOfWeek) {
        DayOfWeek.SATURDAY -> Color(0xFF64B5F6)
        DayOfWeek.SUNDAY -> Color(0xFFE57373)
        else -> Color.White
    }

    val backgroundColor = when {
        studyTimeMinutes == 0 -> Color.Transparent
        studyTimeMinutes < 30 -> Color(0xFF0E4429)
        studyTimeMinutes < 60 -> Color(0xFF006D32)
        studyTimeMinutes < 120 -> Color(0xFF26A641)
        else -> Color(0xFF39D353)
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .padding(4.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .combinedClickable( // ✅ 클릭 및 롱클릭 처리
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.3f), CircleShape)
            )
        } else if (isToday) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
            )
        }

        Text(
            text = date.dayOfMonth.toString(),
            color = dayColor,
            fontWeight = FontWeight.Medium
        )
    }
}