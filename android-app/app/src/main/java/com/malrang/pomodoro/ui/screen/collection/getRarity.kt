package com.malrang.pomodoro.ui.screen.collection

import androidx.compose.ui.graphics.Color
import com.malrang.pomodoro.dataclass.animalInfo.Rarity

fun getRarityString(rarity: Rarity): String {
    return when (rarity) {
        Rarity.COMMON -> "일반"
        Rarity.RARE -> "레어"
        Rarity.EPIC -> "에픽"
        Rarity.LEGENDARY -> "전설"
    }
}

fun getRarityColor(rarity: Rarity): Color {
    return when (rarity) {
        Rarity.COMMON -> Color.LightGray
        Rarity.RARE -> Color(0xFF67A5FF) // 파란색
        Rarity.EPIC -> Color(0xFFC56DFF) // 보라색
        Rarity.LEGENDARY -> Color(0xFFFFD700) // 금색
    }
}