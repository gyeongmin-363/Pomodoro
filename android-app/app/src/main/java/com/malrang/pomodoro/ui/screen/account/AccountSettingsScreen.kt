package com.malrang.pomodoro.ui.screen.account

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.malrang.pomodoro.R
import com.malrang.pomodoro.dataclass.ui.Screen
import com.malrang.pomodoro.ui.ModernConfirmDialog
import com.malrang.pomodoro.viewmodel.AuthViewModel

/**
 * 계정 설정 화면 컴포저블 함수입니다.
 * 로그인 상태에 따라 로그인 버튼 또는 계정 관리(로그아웃/탈퇴) 화면을 표시합니다.
 */
@Composable
fun AccountSettingsScreen(
    authViewModel: AuthViewModel,
    onNavigateTo: (Screen) -> Unit
) {
    val authState by authViewModel.authState.collectAsState()
    val context = LocalContext.current

    // UI 구성
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        // 상단 앱바 영역
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onNavigateTo(Screen.Main) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(text = "계정 설정", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.weight(1f))
        }

        // 컨텐츠 영역
        when (val state = authState) {
            is AuthViewModel.AuthState.Authenticated -> {
                // ✅ 로그인 된 상태: 기존 계정 관리 화면 표시
                AuthenticatedAccountContent(
                    userEmail = state.user?.email ?: "이메일 정보 없음",
                    onLogout = { authViewModel.signOut(context) },
                    onDeleteAccount = {
                        authViewModel.deleteUser(
                            edgeFunctionUrl = "https://inskujiwpvpknfhppmsa.supabase.co/functions/v1/bright-task",
                            activityContext = context
                        )
                    }
                )
            }
            else -> {
                // ✅ 로그인 안 된 상태: 로그인 버튼 표시
                UnauthenticatedAccountContent(
                    onLoginClick = { authViewModel.signInWithGoogle() },
                    errorMsg = (state as? AuthViewModel.AuthState.Error)?.message
                )
            }
        }
    }
}

@Composable
fun AuthenticatedAccountContent(
    userEmail: String,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    if (showLogoutConfirmDialog) {
        ModernConfirmDialog(
            title = "로그아웃",
            content = { Text("정말로 로그아웃 하시겠습니까?") },
            onConfirm = {
                onLogout()
                showLogoutConfirmDialog = false
            },
            onDismissRequest = { showLogoutConfirmDialog = false },
            confirmText = "로그아웃"
        )
    }

    if (showDeleteConfirmDialog) {
        ModernConfirmDialog(
            title = "회원 탈퇴",
            content = { Text("정말로 계정을 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.") },
            onConfirm = {
                onDeleteAccount()
                showDeleteConfirmDialog = false
            },
            onDismissRequest = { showDeleteConfirmDialog = false },
            confirmText = "확인"
        )
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "로그인된 이메일", style = MaterialTheme.typography.titleMedium)
        Text(text = userEmail, style = MaterialTheme.typography.bodyLarge)

        Spacer(modifier = Modifier.height(48.dp))

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

@Composable
fun UnauthenticatedAccountContent(
    onLoginClick: () -> Unit,
    errorMsg: String?
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "로그인이 필요합니다",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "계정을 연동하여 데이터를 안전하게 백업하세요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // LoginScreen에서 가져온 구글 로그인 버튼 스타일
        TextButton(
            onClick = onLoginClick,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(54.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.android_neutral_rd_ctn),
                contentDescription = "구글 로그인",
                modifier = Modifier.fillMaxSize()
            )
        }

        if (errorMsg != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "오류: $errorMsg",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}