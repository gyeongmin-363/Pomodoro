package com.malrang.pomodoro.ui.screen.stats.month

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MonthlySummaryCard(monthlyTotalMinutes: Int) {
    val hours = monthlyTotalMinutes / 60
    val minutes = monthlyTotalMinutes % 60

    // 아이콘 제거, 텍스트 중심 디자인
    Text(
        text = buildAnnotatedString {
            // 라벨: 작고 약간 투명한 흰색
            withStyle(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            ) {
                append("이번 달 총 집중  ")
            }

            // 시간 값: 크고 강조된 색상 (노란색 계열)
            withStyle(
                style = SpanStyle(
                    color = Color(0xFFFFE082), // 파란 배경 위 포인트 컬러
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            ) {
                if (hours > 0) {
                    append("${hours}시간 ")
                }
                append("${minutes}분")
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        textAlign = TextAlign.Center
    )
}