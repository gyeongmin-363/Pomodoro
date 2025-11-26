package com.malrang.pomodoro.ui.screen.stats.month

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MonthlySummaryCard(monthlyTotalMinutes: Int) {
    val hours = monthlyTotalMinutes / 60
    val minutes = monthlyTotalMinutes % 60

    val shape = RoundedCornerShape(12.dp)

    Box(modifier = Modifier.fillMaxWidth()) {
        // Shadow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(x = 4.dp, y = 4.dp)
                .height(60.dp) // 대략적인 높이
                .background(MaterialTheme.colorScheme.outline, shape)
        )

        // Content Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, shape)
                .border(2.dp, MaterialTheme.colorScheme.outline, shape)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "이번 달 총 집중",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(12.dp))

                // 시간 강조 (형광펜 효과 느낌의 색상 사용 가능)
                Text(
                    text = buildAnnotatedString {
                        if (hours > 0) {
                            withStyle(SpanStyle(fontSize = 22.sp)) { append("$hours") }
                            withStyle(SpanStyle(fontSize = 16.sp)) { append("시간 ") }
                        }
                        withStyle(SpanStyle(fontSize = 22.sp)) { append("$minutes") }
                        withStyle(SpanStyle(fontSize = 16.sp)) { append("분") }
                    },
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary // NeoBlue
                )
            }
        }
    }
}