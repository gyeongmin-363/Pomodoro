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

    // [디자인 변경] 한 줄 텍스트 (Span을 이용해 스타일 혼합)
    Text(
        text = buildAnnotatedString {
            // 1. 라벨: 작고 흐린 흰색
            withStyle(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            ) {
                append("이번 달 총 집중  ") // 간격 추가
            }

            // 2. 시간 데이터: 크고 강조된 색상 (파란 배경 위 포인트 컬러)
            withStyle(
                style = SpanStyle(
                    color = Color(0xFFFFE082), // 파란 배경에 잘 어울리는 밝은 노란색/금색 계열
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
            .padding(bottom = 20.dp), // 캘린더와의 간격 혹은 하단 여백
        textAlign = TextAlign.Center // 가운데 정렬
    )
}