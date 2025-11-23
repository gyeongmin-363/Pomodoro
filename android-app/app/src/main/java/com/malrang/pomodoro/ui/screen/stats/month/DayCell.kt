package com.malrang.pomodoro.ui.screen.stats.month

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onLongClick: () -> Unit
) {
    // 날짜 텍스트 색상: 주말 구분
    val dayTextColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        else -> when (date.dayOfWeek) {
            DayOfWeek.SATURDAY -> Color(0xFF42A5F5)
            DayOfWeek.SUNDAY -> Color(0xFFEF5350)
            else -> MaterialTheme.colorScheme.onSurface
        }
    }

    // 히트맵 배경색 (GitHub 스타일 Green 유지하되 테마와 조화롭게)
    // 단, 선택되었을 땐 Primary 색상으로 덮어씀
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        when {
            studyTimeMinutes == 0 -> Color.Transparent
            studyTimeMinutes < 30 -> Color(0xFF9BE9A8) // Light Green
            studyTimeMinutes < 60 -> Color(0xFF40C463)
            studyTimeMinutes < 120 -> Color(0xFF30A14E)
            else -> Color(0xFF216E39) // Dark Green
        }
    }

    // 오늘 날짜 테두리
    val borderModifier = if (isToday && !isSelected) {
        Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .size(42.dp) // 터치 영역 확보
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp)) // 둥근 사각형 (Modern)
            .background(backgroundColor)
            .then(borderModifier)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            color = if (studyTimeMinutes > 60 && !isSelected) Color.White else dayTextColor,
            fontSize = 12.sp,
            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}