package com.malrang.pomodoro.ui.screen.nickname

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.malrang.pomodoro.viewmodel.UserViewModel
import kotlinx.coroutines.delay

@Composable
fun NicknameSetupScreen(
    userViewModel: UserViewModel,
    onNavigateToMain: () -> Unit
) {
    var nickname by remember { mutableStateOf("") }
    val userState by userViewModel.userState.collectAsState()
    val isNicknameAvailable = userState.isNicknameAvailable

    // 사용자가 타이핑을 멈추면 닉네임 중복 검사를 실행 (Debouncing)
    LaunchedEffect(nickname) {
        if (nickname.isNotBlank()) {
            // 500ms 동안 추가 입력이 없으면 검사 실행
            delay(500)
            userViewModel.checkNicknameAvailability(nickname)
        } else {
            // 입력값이 없으면 검사 상태 초기화
            userViewModel.checkNicknameAvailability("")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = nickname,
            onValueChange = { nickname = it },
            label = { Text("닉네임") },
            // isNicknameAvailable이 false일 때 (중복될 때) 에러 상태로 표시
            isError = isNicknameAvailable == false,
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        // 닉네임 유효성 검사 메시지 표시
        Box(modifier = Modifier.height(24.dp)) {
            if (nickname.isNotBlank()) {
                when (isNicknameAvailable) {
                    true -> Text("사용 가능한 닉네임입니다.", color = MaterialTheme.colorScheme.primary)
                    false -> Text("이미 사용 중인 닉네임입니다.", color = MaterialTheme.colorScheme.error)
                    null -> if (userState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp)) // 확인 중 로딩 표시
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                userViewModel.updateNickname(nickname)
            },
            // 닉네임이 비어있지 않고, 사용 가능(true)할 때만 버튼 활성화
            enabled = nickname.isNotBlank() && isNicknameAvailable == true
        ) {
            Text("완료")
        }
    }

    // 닉네임 설정이 완료되면 메인 화면으로 이동
    LaunchedEffect(userState.isNicknameSet) {
        if (userState.isNicknameSet) {
            onNavigateToMain()
        }
    }
}