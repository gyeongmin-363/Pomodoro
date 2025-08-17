//package com.malrang.pomodoro.ui.screen
//
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.material3.Button
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.collectAsState
//import androidx.compose.runtime.getValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Brush
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import com.malrang.pomodoro.data.Rarity
//import com.malrang.pomodoro.data.Screen
//import com.malrang.pomodoro.viewmodel.PomodoroViewModel
//
///**
// * 새로운 동물을 획득했을 때 표시되는 화면입니다.
// *
// * @param viewModel [PomodoroViewModel]의 인스턴스입니다.
// */
//@Composable
//fun AnimalScreen(viewModel: PomodoroViewModel) {
//    val state by viewModel.uiState.collectAsState()
//    val animal = state.collectedAnimals.lastOrNull()
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(Brush.verticalGradient(listOf(Color(0xFF6D28D9), Color(0xFFDB2777))))
//            .padding(16.dp),
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        if (animal != null) {
//            Text("새로운 친구를 만났어요! 🎉", fontSize = 20.sp, fontWeight = FontWeight.Medium)
//            Text(animal.emoji, fontSize = 64.sp, modifier = Modifier.padding(8.dp))
//            Text(animal.name, fontSize = 24.sp, fontWeight = FontWeight.Bold)
//            Text(
//                animal.rarity.name,
//                color = when (animal.rarity) {
//                    Rarity.COMMON -> Color.Green
//                    Rarity.RARE -> Color.Blue
//                    Rarity.EPIC -> Color.Magenta
//                    Rarity.LEGENDARY -> Color.Yellow
//                }
//            )
//        }
//
//        Spacer(Modifier.height(24.dp))
//
//        Button(onClick = { viewModel.showScreen(Screen.Main) }) {
//            Text("계속하기")
//        }
//    }
//}
