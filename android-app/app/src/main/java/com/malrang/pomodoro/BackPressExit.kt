package com.malrang.pomodoro

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext


// 뒤로 가기 두 번 눌렀을 때 앱 종료
@Composable
fun BackPressExit() {
    val context = LocalContext.current
    var backPressedState = remember { mutableStateOf(true) }
    var backPressedTime = 0L

    BackHandler(enabled = backPressedState.value) {
        if(System.currentTimeMillis() - backPressedTime <= 900L) {
            (context as Activity).finish() // 앱 종료
        } else {
            backPressedState.value = true
            Toast.makeText(context, "한 번 더 누르시면 앱이 종료됩니다.", Toast.LENGTH_SHORT).show()
        }
        backPressedTime = System.currentTimeMillis()
    }
}

@Composable
fun BackPressMove(command : () -> Unit){
    BackHandler(enabled = true) {
        command.invoke()
    }
}