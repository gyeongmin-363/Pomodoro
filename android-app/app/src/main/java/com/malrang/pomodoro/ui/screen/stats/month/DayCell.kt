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
    // 1. 스타일 정의
    // Neo-Brutalism에서는 선택됨(Selected) 상태가 가장 강한 대비를 가짐 (Blue or Pink)
    // 공부 시간(StudyTime)은 Green 계열의 채도로 표현

    val shape = RoundedCornerShape(8.dp)

    // 배경색 결정
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primary // 선택됨: 파랑(NeoBlue)
        studyTimeMinutes == 0 -> MaterialTheme.colorScheme.surface // 0분: 흰색
        studyTimeMinutes < 30 -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f) // 연한 초록
        studyTimeMinutes < 60 -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f) // 중간 초록
        studyTimeMinutes < 120 -> MaterialTheme.colorScheme.tertiary // 진한 초록
        else -> Color(0xFF1B5E20) // 아주 진한 초록
    }

    // 텍스트 색상
    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary // 선택됨: 흰색
        studyTimeMinutes >= 60 -> Color.White // 진한 배경 위: 흰색
        else -> when (date.dayOfWeek) {
            DayOfWeek.SATURDAY -> Color(0xFF3B82F6) // 파랑
            DayOfWeek.SUNDAY -> Color(0xFFFF4848) // 빨강
            else -> MaterialTheme.colorScheme.onSurface // 기본 검정
        }
    }

    // 테두리: 기본은 얇게, 오늘이나 선택됨은 굵게
    val borderWidth = if (isSelected || isToday) 2.dp else 1.dp
    val borderColor = if (isSelected) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)

    Box(
        modifier = Modifier
            .size(42.dp)
            .padding(2.dp)
            .clip(shape)
            .background(backgroundColor)
            .border(borderWidth, borderColor, shape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (isToday || isSelected) FontWeight.Black else FontWeight.Medium
        )
    }
}