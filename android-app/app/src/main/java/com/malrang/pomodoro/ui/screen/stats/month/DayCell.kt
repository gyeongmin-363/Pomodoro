package com.malrang.pomodoro.ui.screen.stats.month

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
    // [UI 수정] 배경이 Primary 컬러이므로 기본 텍스트는 흰색이어야 함
    val dayTextColor = when {
        isSelected -> MaterialTheme.colorScheme.primary // 선택 시 배경이 흰색이 되므로 텍스트는 파란색
        else -> when (date.dayOfWeek) {
            DayOfWeek.SATURDAY -> Color(0xFF90CAF9) // 밝은 파랑
            DayOfWeek.SUNDAY -> Color(0xFFEF9A9A) // 밝은 빨강
            else -> Color.White // 기본 흰색
        }
    }

    // 히트맵 배경색
    // 선택되었을 땐 흰색(강조)
    val backgroundColor = if (isSelected) {
        Color.White
    } else {
        when {
            studyTimeMinutes == 0 -> Color.Transparent
            // [UI 수정] 파란 배경 위에서 초록색 히트맵이 잘 보이도록 색상 조정 또는 유지
            studyTimeMinutes < 30 -> Color(0xFF9BE9A8).copy(alpha = 0.8f)
            studyTimeMinutes < 60 -> Color(0xFF40C463).copy(alpha = 0.9f)
            studyTimeMinutes < 120 -> Color(0xFF30A14E)
            else -> Color(0xFF216E39)
        }
    }

    // 오늘 날짜 테두리 (흰색으로 변경)
    val borderModifier = if (isToday && !isSelected) {
        Modifier.border(1.5.dp, Color.White.copy(alpha = 0.7f), CircleShape) // 모양도 원형으로 변경 가능
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .size(42.dp)
            .padding(4.dp) // 간격 조금 더 줌
            .clip(RoundedCornerShape(12.dp)) // 더 둥글게
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