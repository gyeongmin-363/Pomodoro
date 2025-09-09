package com.malrang.pomodoro.ui.screen.stats

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate


@Composable
fun DayCell(
    date: LocalDate,
    hasRecord: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val dayColor = when (date.dayOfWeek) {
        DayOfWeek.SATURDAY -> Color(0xFF64B5F6)
        DayOfWeek.SUNDAY -> Color(0xFFE57373)
        else -> Color.White
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .padding(4.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
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
                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
            )
        }

        if (hasRecord) {
            Image(
                painterResource(com.malrang.pomodoro.R.drawable.cat_stamp_image),
                contentDescription = "고양이 스탬프",
                contentScale = ContentScale.FillBounds,
                colorFilter = ColorFilter.tint(Color(0xFF3DB046))
            )
        }
        Text(
            text = date.dayOfMonth.toString(),
            color = dayColor,
            fontWeight = FontWeight.Medium
        )
    }
}

