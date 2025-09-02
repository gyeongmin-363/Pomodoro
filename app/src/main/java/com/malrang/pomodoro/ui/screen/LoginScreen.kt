package com.malrang.pomodoro.ui.screen

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import com.malrang.pomodoro.viewmodel.AuthViewModel

@Composable
fun LoginScreen(viewModel: AuthViewModel) {
    val state by viewModel.uiState.collectAsState()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(onClick = { viewModel.signInWithGoogle() }) {
            Text("Google로 로그인")
        }

        // 이 부분은 ViewModel의 상태가 바뀌면 자동으로 업데이트됩니다.
        when(state) {
            is AuthViewModel.AuthState.Loading -> CircularProgressIndicator() // 로딩 중일 때 표시
            is AuthViewModel.AuthState.Authenticated -> Text("로그인 완료: ${(state as AuthViewModel.AuthState.Authenticated).user?.email}")
            is AuthViewModel.AuthState.Error -> Text("오류: ${(state as AuthViewModel.AuthState.Error).message}")
            else -> {}
        }
        when(state){
            is AuthViewModel.AuthState.Authenticated -> {
                Log.d("로그인 완료",(state as AuthViewModel.AuthState.Authenticated).user?.email.toString())
            }
            is AuthViewModel.AuthState.Error -> {
                Log.d("로그인 실패","로그인 실패")
            }
            else -> {}
        }
    }
}