// NicknameSetupScreen.kt 파일 전체를 아래 코드로 교체하세요.

package com.malrang.pomodoro.ui.screen.nickname

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.malrang.pomodoro.viewmodel.UserViewModel

@Composable
fun NicknameSetupScreen(
    userViewModel: UserViewModel,
    onNavigateToMain: () -> Unit
) {
    var nickname by remember { mutableStateOf("") }
    val userState by userViewModel.userState.collectAsState()
    val isNicknameAvailable = userState.isNicknameAvailable

    // 닉네임 설정이 완료되면 메인 화면으로 이동하는 로직은 그대로 둡니다.
    LaunchedEffect(userState.isNicknameSet) {
        if (userState.isNicknameSet) {
            onNavigateToMain()
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
            onValueChange = {
                if (it.length <= 10) {
                    nickname = it
                }
            },
            label = { Text("닉네임") },
            // isNicknameAvailable 상태가 false일 때만 에러로 표시
            isError = isNicknameAvailable == false,
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.height(24.dp)) {
            if (isNicknameAvailable == false) {
                Text("이미 사용 중인 닉네임입니다.", color = MaterialTheme.colorScheme.error)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                // ✅ 버튼 클릭 시 ViewModel의 통합된 함수를 호출
                userViewModel.submitNickname(nickname)
            },
            enabled = nickname.isNotBlank() && !userState.isLoading
        ) {
            if (userState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("시작하기")
            }
        }
    }
}