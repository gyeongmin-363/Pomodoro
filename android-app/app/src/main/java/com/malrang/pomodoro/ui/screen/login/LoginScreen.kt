package com.malrang.pomodoro.ui.screen.login

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.malrang.pomodoro.R
import com.malrang.pomodoro.viewmodel.AuthViewModel

@Composable
fun LoginScreen(viewModel: AuthViewModel) {
    val state by viewModel.uiState.collectAsState()

    // 로그인 성공/실패 시 로그 출력 (기존 기능 유지)
    when(state){
        is AuthViewModel.AuthState.Authenticated -> {
            Log.d("로그인 완료",(state as AuthViewModel.AuthState.Authenticated).user?.email.toString())
        }
        is AuthViewModel.AuthState.Error -> {
            Log.d("로그인 실패","로그인 실패")
        }
        else -> {}
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. 앱 로고 및 이름
            Text(
                text = "픽뽀",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "간편하게 로그인하고 시작해보세요!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(64.dp))

            // 2. Google 로그인 버튼
            // 이미지에 텍스트가 포함되어 있으므로 Icon 대신 Image 컴포저블을 사용합니다.
            TextButton(
                onClick = { viewModel.signInWithGoogle() },
                modifier = Modifier
                    .fillMaxWidth() // 너비를 최대로 채웁니다.
                    .height(54.dp), // 원하는 높이로 설정합니다.
                contentPadding = PaddingValues(0.dp) // 버튼의 내부 여백을 제거하여 이미지가 꽉 차도록 합니다.
            ) {
                Image(
                    painter = painterResource(id = R.drawable.android_neutral_rd_ctn),
                    contentDescription = "구글 로그인", // 스크린 리더 등을 위한 설명
                    modifier = Modifier.fillMaxSize() // 버튼 크기에 맞게 이미지 크기를 조정합니다.
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // 3. 에러 메시지 표시
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

