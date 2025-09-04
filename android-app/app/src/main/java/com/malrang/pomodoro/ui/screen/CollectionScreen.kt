package com.malrang.pomodoro.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.malrang.pomodoro.dataclass.animalInfo.Animal
import com.malrang.pomodoro.dataclass.animalInfo.Rarity
import com.malrang.pomodoro.dataclass.sprite.AnimalSprite
import com.malrang.pomodoro.dataclass.sprite.SpriteMap
import com.malrang.pomodoro.dataclass.sprite.SpriteState
import com.malrang.pomodoro.dataclass.ui.Screen
import com.malrang.pomodoro.viewmodel.PomodoroViewModel

/**
 * 수집한 동물들의 목록을 보여주는 도감 화면입니다.
 *
 * @param viewModel [PomodoroViewModel]의 인스턴스입니다.
 */
@Composable
fun CollectionScreen(viewModel: PomodoroViewModel) {
    val state by viewModel.uiState.collectAsState()
    val totalAnimals = Animal.entries.size
    var selectedAnimal by remember { mutableStateOf<Animal?>(null) }
    // 희귀도 내림차순, 이름 오름차순으로 정렬
    val sortedAnimals = state.collectedAnimals.sortedWith(
        compareByDescending<Animal> { it.rarity.ordinal }
            .thenBy { it.displayName }
    )


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1B4B))
            .padding(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🐾 동물 도감", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            IconButton(onClick = { viewModel.navigateTo(Screen.Main) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "돌아가기",
                    tint = Color.White
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = "${state.collectedAnimals.size}/${totalAnimals}",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.align(Alignment.End)
        )

        Spacer(Modifier.height(16.dp))

        if (state.collectedAnimals.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("아직 수집한 동물이 없어요.\n공부를 시작해보세요! 📚", color = Color.LightGray, textAlign = TextAlign.Center)
            }
        } else {
            LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.fillMaxSize()) {
                items(sortedAnimals) { animal -> // 정렬된 리스트를 사용합니다.
                    Card(
                        modifier = Modifier
                            .padding(4.dp)
                            .height(160.dp)
                            .fillMaxWidth()
                            .clickable { selectedAnimal = animal },
                        colors = CardDefaults.cardColors(containerColor = Color(0x33FFFFFF))
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceAround,
                            modifier = Modifier
                                .padding(8.dp)
                                .fillMaxSize()
                        ) {
                            SpriteItem(animal = animal, size = 64f)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ){
                                Text(
                                    text = animal.displayName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = getRarityString(animal.rarity),
                                    fontSize = 10.sp,
                                    color = getRarityColor(animal.rarity)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedAnimal != null) {
        Dialog(onDismissRequest = { selectedAnimal = null }) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                AnimalDetailModal(animal = selectedAnimal!!, onDismissRequest = { selectedAnimal = null })
                Text(
                    text = "빈 공간을 터치하여 창 닫기",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }
    }
}

/**
 * 동물의 상세 정보를 보여주는 모달 창입니다.
 */
@Composable
fun AnimalDetailModal(animal: Animal, onDismissRequest: () -> Unit) {
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2E2A5C))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                SpriteItem(animal = animal, size = 128f)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = animal.displayName,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = getRarityString(animal.rarity),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = getRarityColor(animal.rarity)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = animal.description,
                    fontSize = 14.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}


@Composable
fun SpriteItem(animal: Animal, size: Float) {
    val spriteData = SpriteMap.map[animal]
    if (spriteData == null) return
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
            sizeDp = size
        )
    }

    SpriteSheetImage(
        sprite = tempSprite,
        onJumpFinished = {},
        modifier = Modifier.size(size.dp)
    )
}

@Composable
private fun getRarityString(rarity: Rarity): String {
    return when (rarity) {
        Rarity.COMMON -> "일반"
        Rarity.RARE -> "레어"
        Rarity.EPIC -> "에픽"
        Rarity.LEGENDARY -> "전설"
    }
}

@Composable
private fun getRarityColor(rarity: Rarity): Color {
    return when (rarity) {
        Rarity.COMMON -> Color.LightGray
        Rarity.RARE -> Color(0xFF67A5FF) // 파란색
        Rarity.EPIC -> Color(0xFFC56DFF) // 보라색
        Rarity.LEGENDARY -> Color(0xFFFFD700) // 금색
    }
}