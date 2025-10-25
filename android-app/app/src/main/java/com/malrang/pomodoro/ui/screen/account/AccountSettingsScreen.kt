package com.malrang.pomodoro.ui.screen.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.malrang.pomodoro.dataclass.ui.Screen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.malrang.pomodoro.ui.ModernConfirmDialog
import com.malrang.pomodoro.viewmodel.AuthViewModel

/**
 * 계정 설정 화면 컴포저블 함수입니다.
 * 로그인된 사용자 이메일 표시, 로그아웃, 회원 탈퇴 기능을 제공합니다.
 * @param authViewModel 인증 관련 로직을 처리하는 ViewModel입니다.
 * @param onNavigateTo 화면 전환을 처리하는 함수입니다.
 */
@Composable
fun AccountSettingsScreen(
    authViewModel: AuthViewModel,
    onNavigateTo: (Screen) -> Unit
) {
    // ✅ uiState가 아닌 authState를 구독하도록 수정합니다.
    val authState by authViewModel.authState.collectAsState()
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val userEmail = when (val state = authState) {
        is AuthViewModel.AuthState.Authenticated -> state.user?.email ?: "이메일 정보 없음"
        else -> "이메일 정보 없음"
    }

    if (showLogoutConfirmDialog) {
        ModernConfirmDialog(
            title = "로그아웃",
            content = { Text("정말로 로그아웃 하시겠습니까?") },
            onConfirm = {
                authViewModel.signOut(context)
                showLogoutConfirmDialog = false
            },
            onDismissRequest = {
                showLogoutConfirmDialog = false
            },
            confirmText = "로그아웃"
        )
    }

    if (showDeleteConfirmDialog) {
        ModernConfirmDialog(
            title = "회원 탈퇴",
            content = { Text("정말로 계정을 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.") },
            onConfirm = {
                authViewModel.deleteUser(
                    edgeFunctionUrl = "https://inskujiwpvpknfhppmsa.supabase.co/functions/v1/bright-task",
                    activityContext = context
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
    ) {
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
}