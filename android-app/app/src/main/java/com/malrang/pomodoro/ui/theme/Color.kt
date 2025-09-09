package com.malrang.pomodoro.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.malrang.pomodoro.R
import java.util.Calendar

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

/**
 * 8시 ~ 16시 : day
 * 16시 ~ 20시 : sunset
 * 20시 ~ 04시 : night
 * 04시 ~ 8시 : sunset
 * */
val backgroundImage = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 8..15 -> R.drawable.day_background // 8시 ~ 15시 59분
    in 16..19, in 4..7 -> R.drawable.sunset_background // 16시 ~ 19시 59분, 4시 ~ 7시 59분
    else -> R.drawable.night_background // 20시 ~ 3시 59분
}

@Composable
fun SetBackgroundImage(){
    Image(
        painterResource(backgroundImage),
        contentDescription = "배경",
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
    )
}

val dialogColor = Color(0xFF1E1B4B)