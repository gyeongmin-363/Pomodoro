package com.malrang.pomodoro.ui.screen.account

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
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
    onNavigateTo: (Screen) -> Unit // 화이트리스트 화면 이동을 위해 추가
) {
    val authState by authViewModel.authState.collectAsState()
    val backupState by authViewModel.backupState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // 백업/복원 상태 모니터링
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
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                            .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "계정 및 설정",
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
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
                        onRestoreClick = { authViewModel.restoreData() },
                        onWhitelistClick = { onNavigateTo(Screen.Whitelist) }
                    )
                }
                else -> {
                    UnauthenticatedAccountContent(
                        onLoginClick = { authViewModel.signInWithGoogle() },
                        onWhitelistClick = { onNavigateTo(Screen.Whitelist) },
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
    onRestoreClick: () -> Unit,
    onWhitelistClick: () -> Unit
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
    if (showBackupConfirmDialog) {
        ModernConfirmDialog(
            title = "데이터 백업",
            content = { Text("현재 기기의 데이터를 서버에 저장합니다.\n기존에 저장된 서버 데이터는 덮어씌워집니다.") },
            onConfirm = { onBackupClick(); showBackupConfirmDialog = false },
            onDismissRequest = { showBackupConfirmDialog = false },
            confirmText = "백업하기"
        )
    }
    if (showRestoreConfirmDialog) {
        ModernConfirmDialog(
            title = "데이터 복원",
            content = {
                Text(
                    "서버에서 데이터를 불러옵니다.\n\n⚠️ 주의: 현재 기기의 모든 데이터가 삭제되고 서버 데이터로 대체됩니다.",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
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
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        // 1. 프로필 카드
        NeoProfileCard(email = userEmail)

        // 2. 앱 설정 섹션 (화이트리스트)
        Column(modifier = Modifier.fillMaxWidth()) {
            NeoSectionTitle("앱 설정")
            Spacer(modifier = Modifier.height(12.dp))

            NeoActionCard(
                title = "차단 앱 관리",
                description = "모든 Work에 공통 적용되는 허용 앱 목록",
                icon = R.drawable.ic_skip, // 적절한 아이콘으로 변경 가능
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                isLoading = false,
                onClick = onWhitelistClick
            )
        }

        // 3. 백업 및 복원 섹션
        Column(modifier = Modifier.fillMaxWidth()) {
            NeoSectionTitle("클라우드 백업")
            Spacer(modifier = Modifier.height(12.dp))

            NeoActionCard(
                title = "데이터 백업",
                description = "현재 데이터를 서버에 저장",
                icon = R.drawable.cloud_upload_24px,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                isLoading = isLoading,
                onClick = { showBackupConfirmDialog = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            NeoActionCard(
                title = "데이터 복원",
                description = "서버 데이터 불러오기",
                icon = R.drawable.cloud_download_24px,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                isLoading = isLoading,
                onClick = { showRestoreConfirmDialog = true }
            )
        }

        // 4. 계정 작업 섹션
        Column(modifier = Modifier.fillMaxWidth()) {
            NeoSectionTitle("계정 작업")
            Spacer(modifier = Modifier.height(12.dp))

            NeoButton(
                text = "로그아웃",
                onClick = { showLogoutConfirmDialog = true },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(12.dp))

            NeoButton(
                text = "회원 탈퇴",
                onClick = { showDeleteConfirmDialog = true },
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
                enabled = !isLoading
            )
        }

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
fun UnauthenticatedAccountContent(
    onLoginClick: () -> Unit,
    onWhitelistClick: () -> Unit,
    errorMsg: String?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 비로그인 상태에서도 화이트리스트 설정 가능하도록 버튼 배치
        NeoButton(
            text = "차단 앱(화이트리스트) 설정",
            onClick = onWhitelistClick,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 로그인 안내 카드
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = 6.dp, y = 6.dp)
                    .background(MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                    .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "로그인 필요",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "계정을 연동하여 데이터를 안전하게\n백업하고 동기화하세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(32.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clickable(onClick = onLoginClick)
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .offset(x = 3.dp, y = 3.dp)
                            .background(MaterialTheme.colorScheme.outline, RoundedCornerShape(28.dp))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White, RoundedCornerShape(28.dp))
                            .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(28.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.android_neutral_rd_ctn),
                                contentDescription = "Google Logo",
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Google로 계속하기",
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        }

        if (errorMsg != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(8.dp))
                    .border(2.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(8.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "오류: $errorMsg",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun NeoSectionTitle(text: String) {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(4.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondary
        )
    }
}

@Composable
fun NeoActionCard(
    title: String,
    description: String,
    @DrawableRes icon: Int,
    containerColor: Color,
    contentColor: Color,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable(enabled = !isLoading, onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 4.dp, y = 4.dp)
                .background(MaterialTheme.colorScheme.outline, shape)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(containerColor, shape)
                .border(2.dp, MaterialTheme.colorScheme.outline, shape)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.3f), CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = icon),
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = contentColor
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = contentColor,
                        strokeWidth = 3.dp
                    )
                }
            }
        }
    }
}

@Composable
fun NeoProfileCard(email: String) {
    val initial = email.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val shape = RoundedCornerShape(16.dp)

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 6.dp, y = 6.dp)
                .background(MaterialTheme.colorScheme.outline, shape)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, shape)
                .border(2.dp, MaterialTheme.colorScheme.outline, shape)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(MaterialTheme.colorScheme.secondary, CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSecondary
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = email, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                Text(text = "Google 계정", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun NeoButton(
    text: String,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 3.dp, y = 3.dp)
                .background(MaterialTheme.colorScheme.outline, shape)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if(enabled) containerColor else Color.Gray, shape)
                .border(2.dp, MaterialTheme.colorScheme.outline, shape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}