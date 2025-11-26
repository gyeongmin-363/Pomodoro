package com.malrang.pomodoro.ui.screen.account

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.malrang.pomodoro.R
import com.malrang.pomodoro.dataclass.ui.Screen
import com.malrang.pomodoro.ui.ModernConfirmDialog
import com.malrang.pomodoro.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsScreen(
    authViewModel: AuthViewModel,
) {
    val authState by authViewModel.authState.collectAsState()
    val backupState by authViewModel.backupState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // 백업/복원 상태 모니터링 및 피드백
    LaunchedEffect(backupState) {
        when (val state = backupState) {
            is AuthViewModel.BackupState.Success -> {
                snackbarHostState.showSnackbar(state.message)
                authViewModel.clearBackupState()
            }
            is AuthViewModel.BackupState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                authViewModel.clearBackupState()
            }
            else -> {}
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("계정 설정", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = authState) {
                is AuthViewModel.AuthState.Authenticated -> {
                    AuthenticatedAccountContent(
                        userEmail = state.user?.email ?: "이메일 정보 없음",
                        backupState = backupState,
                        onLogout = { authViewModel.signOut(context) },
                        onDeleteAccount = {
                            authViewModel.deleteUser(
                                edgeFunctionUrl = "https://inskujiwpvpknfhppmsa.supabase.co/functions/v1/bright-task",
                                activityContext = context
                            )
                        },
                        onBackupClick = { authViewModel.backupData() },
                        onRestoreClick = { authViewModel.restoreData() }
                    )
                }
                else -> {
                    UnauthenticatedAccountContent(
                        onLoginClick = { authViewModel.signInWithGoogle() },
                        errorMsg = (state as? AuthViewModel.AuthState.Error)?.message
                    )
                }
            }
        }
    }
}

@Composable
fun AuthenticatedAccountContent(
    userEmail: String,
    backupState: AuthViewModel.BackupState,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit,
    onBackupClick: () -> Unit,
    onRestoreClick: () -> Unit
) {
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var showBackupConfirmDialog by remember { mutableStateOf(false) }

    val isLoading = backupState is AuthViewModel.BackupState.Loading

    if (showLogoutConfirmDialog) {
        ModernConfirmDialog(
            title = "로그아웃",
            content = { Text("정말로 로그아웃 하시겠습니까?") },
            onConfirm = { onLogout(); showLogoutConfirmDialog = false },
            onDismissRequest = { showLogoutConfirmDialog = false },
            confirmText = "로그아웃"
        )
    }
    if (showDeleteConfirmDialog) {
        ModernConfirmDialog(
            title = "회원 탈퇴",
            content = { Text("정말로 계정을 삭제하시겠습니까?\n서버에 저장된 데이터는 없어지며, 기기에는 앱 삭제 전까지 데이터가 유지됩니다.") },
            onConfirm = { onDeleteAccount(); showDeleteConfirmDialog = false },
            onDismissRequest = { showDeleteConfirmDialog = false },
            confirmText = "탈퇴 확인"
        )
    }
    // 백업 확인 다이얼로그
    if (showBackupConfirmDialog) {
        ModernConfirmDialog(
            title = "데이터 백업",
            content = { Text("현재 기기의 데이터를 서버에 저장합니다.\n기존에 저장된 서버 데이터는 덮어씌워집니다.") },
            onConfirm = { onBackupClick(); showBackupConfirmDialog = false },
            onDismissRequest = { showBackupConfirmDialog = false },
            confirmText = "백업하기"
        )
    }
    // 복원 확인 다이얼로그 (중요!)
    if (showRestoreConfirmDialog) {
        ModernConfirmDialog(
            title = "데이터 복원",
            content = {
                Text(
                    "서버에서 데이터를 불러옵니다.\n\n⚠️ 주의: 현재 기기의 모든 데이터가 삭제되고 서버 데이터로 대체됩니다.",
                    color = MaterialTheme.colorScheme.error
                )
            },
            onConfirm = { onRestoreClick(); showRestoreConfirmDialog = false },
            onDismissRequest = { showRestoreConfirmDialog = false },
            confirmText = "복원하기"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        ProfileCard(email = userEmail)
        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // [백업 및 복원 섹션]
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "클라우드 백업",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )

            // 백업 버튼
            ActionButtonCard(
                title = "데이터 백업",
                description = "현재 데이터를 서버에 저장합니다.",
                icon = R.drawable.cloud_upload_24px,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                isLoading = isLoading,
                onClick = { showBackupConfirmDialog = true }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 복원 버튼
            ActionButtonCard(
                title = "데이터 복원",
                description = "서버에서 데이터를 불러옵니다.",
                icon = R.drawable.cloud_download_24px,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                isLoading = isLoading,
                onClick = { showRestoreConfirmDialog = true }
            )
        }

        // 계정 작업 섹션
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "계정 작업",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )
            OutlinedButton(
                onClick = { showLogoutConfirmDialog = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("로그아웃")
            }
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = { showDeleteConfirmDialog = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text(text = "회원 탈퇴", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// 재사용 가능한 액션 버튼 카드
@Composable
fun ActionButtonCard(
    title: String,
    description: String,
    @DrawableRes icon: Int,
    containerColor: Color,
    contentColor: Color,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = !isLoading, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = contentColor,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

@Composable
fun UnauthenticatedAccountContent(
    onLoginClick: () -> Unit,
    errorMsg: String?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "로그인이 필요합니다",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "계정을 연동하여 데이터를 안전하게\n백업하고 동기화하세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))

                // 구글 로그인 버튼
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clickable(onClick = onLoginClick),
                    shape = RoundedCornerShape(25.dp),
                    shadowElevation = 4.dp,
                    color = Color.White
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.android_neutral_rd_ctn),
                            contentDescription = "Google Logo",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        if (errorMsg != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "오류: $errorMsg",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun ProfileCard(email: String) {
    val initial = email.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = email,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Google 계정",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}