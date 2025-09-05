package com.malrang.pomodoro.ui.screen.whitelist

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun PixelSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 64.dp,
    height: Dp = 32.dp,
    borderWidth: Dp = 2.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val thumbSize = height - borderWidth * 2

    // 엄지(thumb)의 위치를 애니메이션으로 처리
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) width - thumbSize - borderWidth * 2 else 0.dp,
        animationSpec = tween(durationMillis = 150)
    )

    // 각 부분의 색상을 애니메이션으로 처리
    val trackColor by animateColorAsState(
        targetValue = if (checked) Color(0xFF4CAF50) else Color(0xFF757575), // 활성/비활성 트랙 색상
        animationSpec = tween(durationMillis = 150)
    )
    val trackBorderColor by animateColorAsState(
        targetValue = if (checked) Color(0xFF388E3C) else Color(0xFF424242), // 활성/비활성 트랙 테두리 색상
        animationSpec = tween(durationMillis = 150)
    )
    val thumbColor by animateColorAsState(
        targetValue = Color(0xFFE0E0E0), // 활성/비활성 엄지 색상
        animationSpec = tween(durationMillis = 150)
    )
    val thumbBorderColor by animateColorAsState(
        targetValue = Color(0xFF9E9E9E), // 활성/비활성 엄지 테두리 색상
        animationSpec = tween(durationMillis = 150)
    )

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .border(width = borderWidth, color = trackBorderColor, shape = RectangleShape)
            .background(color = trackColor, shape = RectangleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null // 클릭 시 물결 효과 제거
            ) {
                onCheckedChange(!checked)
            }
            .padding(borderWidth),
        contentAlignment = Alignment.CenterStart
    ) {
        // 네모난 픽셀 스타일 엄지 (Thumb)
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(thumbSize)
                .border(width = borderWidth, color = thumbBorderColor, shape = RectangleShape)
                .background(color = thumbColor, shape = RectangleShape)
        )
    }
}