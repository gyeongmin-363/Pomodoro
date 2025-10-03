package com.malrang.pomodoro.ui.screen.login

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malrang.pomodoro.R
import com.malrang.pomodoro.dataclass.animalInfo.Animal
import com.malrang.pomodoro.dataclass.sprite.AnimalSprite
import com.malrang.pomodoro.dataclass.sprite.SpriteMap
import com.malrang.pomodoro.ui.screen.main.SpriteSheetImage
import com.malrang.pomodoro.viewmodel.AuthViewModel
import java.util.UUID

@Composable
fun LoginScreen(viewModel: AuthViewModel) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        LoginScreenLandscape(viewModel) // 가로 모드 함수 호출
    } else {
        LoginScreenPortrait(viewModel) // 세로 모드 함수 호출
    }
}

// 기존의 세로 모드 UI를 분리한 함수입니다.
@Composable
private fun LoginScreenPortrait(viewModel: AuthViewModel) {
    val state by viewModel.uiState.collectAsState()
    var animals by remember { mutableStateOf(emptyList<AnimalSprite>()) }

    // 기존 LaunchedEffect 로직
    LaunchedEffect(Unit) {
        val allAnimals = Animal.entries
        animals = allAnimals.map { animal ->
            val spriteData = SpriteMap.map[animal]
            if (spriteData == null) return@LaunchedEffect
            AnimalSprite(
                id = UUID.randomUUID().toString(),
                animalId = animal.id,
                idleSheetRes = spriteData.idleRes,
                idleCols = spriteData.idleCols,
                idleRows = spriteData.idleRows,
                jumpSheetRes = spriteData.jumpRes,
                jumpCols = spriteData.jumpCols,
                jumpRows = spriteData.jumpRows,
                x = 0f, y = 0f, vx = 0f, vy = 0f, sizeDp = 100f
            )
        }.filterNotNull()
    }

    // 기존의 세로 모드 UI
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Image(
            painterResource(R.drawable.grass_background),
            contentDescription = "배경",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 동물 스프라이트
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            animals.forEach { sprite ->
                SpriteSheetImage(
                    sprite = sprite,
                    modifier = Modifier.size(100.dp)
                )
            }
        }

        // 중앙 UI
        Column(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.launcher_image),
                contentDescription = "앱 로고",
                modifier = Modifier
                    .height(180.dp)
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            )
            Text(
                text = "픽뽀",
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                fontSize = 30.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "픽셀아트 스타일의 뽀모도로 앱!",
                color = Color.Black,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "간편하게 로그인하고 시작해보세요!",
                color = Color.Black,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(64.dp))
            TextButton(
                onClick = { viewModel.signInWithGoogle() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.android_neutral_rd_ctn),
                    contentDescription = "구글 로그인",
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (state is AuthViewModel.AuthState.Error) {
                Text(
                    text = "로그인 중 오류가 발생했습니다.\n${(state as AuthViewModel.AuthState.Error).message}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

// 가로 모드 UI를 위한 새로운 함수입니다.
@Composable
private fun LoginScreenLandscape(viewModel: AuthViewModel) {
    val state by viewModel.uiState.collectAsState()
    var animals by remember { mutableStateOf(emptyList<AnimalSprite>()) }

    // 기존 LaunchedEffect 로직
    LaunchedEffect(Unit) {
        val allAnimals = Animal.entries
        animals = allAnimals.map { animal ->
            val spriteData = SpriteMap.map[animal]
            if (spriteData == null) return@LaunchedEffect
            AnimalSprite(
                id = UUID.randomUUID().toString(),
                animalId = animal.id,
                idleSheetRes = spriteData.idleRes,
                idleCols = spriteData.idleCols,
                idleRows = spriteData.idleRows,
                jumpSheetRes = spriteData.jumpRes,
                jumpCols = spriteData.jumpCols,
                jumpRows = spriteData.jumpRows,
                x = 0f, y = 0f, vx = 0f, vy = 0f, sizeDp = 100f
            )
        }.filterNotNull()
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Image(
            painterResource(R.drawable.grass_background),
            contentDescription = "배경",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 동물 스프라이트
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.Center,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ){
                animals.forEach { sprite ->
                    SpriteSheetImage(
                        sprite = sprite,
                        modifier = Modifier.size(50.dp)
                    )
                }
            }
        }

        // 가로 모드 레이아웃: Row를 사용하여 좌우로 분할합니다.
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 왼쪽: 런처 아이콘
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.launcher_image),
                    contentDescription = "앱 로고",
                    modifier = Modifier
                        .size(180.dp)
                )
            }

            // 오른쪽: 제목, 설명, 로그인 버튼
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "픽뽀",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 30.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "픽셀아트 스타일의 뽀모도로 앱!",
                    color = Color.Black,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "간편하게 로그인하고 시작해보세요!",
                    color = Color.Black,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(32.dp))
                TextButton(
                    onClick = { viewModel.signInWithGoogle() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.android_neutral_rd_ctn),
                        contentDescription = "구글 로그인",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (state is AuthViewModel.AuthState.Error) {
                    Text(
                        text = "로그인 중 오류가 발생했습니다.\n${(state as AuthViewModel.AuthState.Error).message}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}