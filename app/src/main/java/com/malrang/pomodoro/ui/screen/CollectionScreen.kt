package com.malrang.pomodoro.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malrang.pomodoro.data.Animal
import com.malrang.pomodoro.data.AnimalSprite
import com.malrang.pomodoro.data.Rarity
import com.malrang.pomodoro.data.Screen
import com.malrang.pomodoro.data.SpriteMap
import com.malrang.pomodoro.data.SpriteState
import com.malrang.pomodoro.viewmodel.PomodoroViewModel

/**
 * 수집한 동물들의 목록을 보여주는 도감 화면입니다.
 *
 * @param viewModel [PomodoroViewModel]의 인스턴스입니다.
 */
@Composable
fun CollectionScreen(viewModel: PomodoroViewModel) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1B4B))
            .padding(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("🐾 움직이는 동물 도감", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Button(onClick = { viewModel.showScreen(Screen.Main) }) { Text("← 돌아가기") }
        }

        Spacer(Modifier.height(16.dp))

        if (state.collectedAnimals.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("아직 수집한 동물이 없어요.\n공부를 시작해보세요! 📚", color = Color.LightGray, textAlign = TextAlign.Center)
            }
        } else {
            LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.fillMaxSize()) {
                items(state.collectedAnimals) { animal ->
                    Card(
                        modifier = Modifier
                            .padding(4.dp)
                            .fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0x33FFFFFF))
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
                            SpriteItem(animal = animal)
                            Text(animal.name, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(
                                when (animal.rarity) {
                                    Rarity.COMMON -> "일반"
                                    Rarity.RARE -> "레어"
                                    Rarity.EPIC -> "에픽"
                                    Rarity.LEGENDARY -> "전설"
                                },
                                fontSize = 12.sp,
                                color = Color.LightGray
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpriteItem(animal: Animal) {
    val spriteData = SpriteMap.map[animal.id]
    if(spriteData == null) return
    val tempSprite = remember(animal.id) {
        AnimalSprite(
            id = animal.id + "-collection",
            animalId = animal.id,
            idleSheetRes = spriteData.idleRes,
            idleCols = spriteData.idleCols,
            idleRows = spriteData.idleRows,
            jumpSheetRes = spriteData.jumpRes,
            jumpCols = spriteData.jumpCols,
            jumpRows = spriteData.jumpRows,
            spriteState = SpriteState.IDLE,
            x = 0f,
            y = 0f,
            vx = 0f,
            vy = 0f,
            sizeDp = 64f
        )
    }

    SpriteSheetImage(
        sprite = tempSprite,
        onJumpFinished = {},
        modifier = Modifier.size(64.dp)
    )
}