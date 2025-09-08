package com.malrang.pomodoro.ui.screen.account

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.malrang.pomodoro.ui.PixelArtConfirmDialog
import com.malrang.pomodoro.viewmodel.AuthViewModel

/**
 * 계정 설정 화면 컴포저블 함수입니다.
 * 로그인된 사용자 이메일 표시, 로그아웃, 회원 탈퇴 기능을 제공합니다.
 * @param authViewModel 인증 관련 로직을 처리하는 ViewModel입니다.
 */
@Composable
fun AccountSettingsScreen(authViewModel: AuthViewModel = viewModel()) {
    val authState by authViewModel.uiState.collectAsState()
    // 1. 로그아웃 및 회원탈퇴 다이얼로그 상태를 관리할 변수 선언
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    val context = LocalActivity.current

    // 현재 인증 상태에 따라 사용자 이메일을 가져옵니다.
    val userEmail = when (val state = authState) {
        is AuthViewModel.AuthState.Authenticated -> state.user!!.email
        else -> "이메일 정보 없음"
    }

    // 2. 로그아웃 확인 다이얼로그
    if (showLogoutConfirmDialog) {
        PixelArtConfirmDialog(
            title = "로그아웃",
            content = { Text("정말로 로그아웃 하시겠습니까?") },
            onConfirm = {
                authViewModel.signOut(context!!)
                showLogoutConfirmDialog = false
            },
            onDismissRequest = {
                showLogoutConfirmDialog = false
            },
            confirmText = "로그아웃"
        )
    }

    // 회원 탈퇴 확인 다이얼로그
    if (showDeleteConfirmDialog) {
        PixelArtConfirmDialog(
            title = "회원 탈퇴",
            content = { Text("정말로 계정을 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.") },
            onConfirm = {
                authViewModel.deleteUser(
                    edgeFunctionUrl = "https://inskujiwpvpknfhppmsa.supabase.co/functions/v1/bright-task",
                    activityContext = context!!
                )
                showDeleteConfirmDialog = false
            },
            onDismissRequest = {
                showDeleteConfirmDialog = false
            },
            confirmText = "확인"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "계정 정보", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "로그인된 이메일", style = MaterialTheme.typography.titleMedium)
        Text(text = userEmail ?: "이메일 정보 없음", style = MaterialTheme.typography.bodyLarge)

        Spacer(modifier = Modifier.height(48.dp))

        // 3. 로그아웃 버튼 클릭 시 ViewModel을 직접 호출하는 대신 다이얼로그를 띄우도록 수정
        Button(onClick = { showLogoutConfirmDialog = true }) {
            Text("로그아웃")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { showDeleteConfirmDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("회원탈퇴", color = Color.White)
        }
    }
}