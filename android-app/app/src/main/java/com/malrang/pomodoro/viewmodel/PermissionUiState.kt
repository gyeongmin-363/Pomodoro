package com.malrang.pomodoro.viewmodel

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malrang.pomodoro.dataclass.ui.PermissionInfo
import com.malrang.pomodoro.dataclass.ui.PermissionType
import com.malrang.pomodoro.localRepo.PomodoroRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PermissionUiState(
    val permissions: List<PermissionInfo> = emptyList(),
    val sessionAttemptedPermissions: Set<PermissionType> = emptySet(),
    val notificationDenialCount: Int = 0
)

class PermissionViewModel(
    private val localRepo: PomodoroRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PermissionUiState())
    val uiState: StateFlow<PermissionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(notificationDenialCount = localRepo.loadNotificationDenialCount()) }
        }
    }

    fun onPermissionRequestResult(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            checkAndUpdatePermissions(context)
            return
        }

        val notificationPermissionInfo = _uiState.value.permissions.find { it.type == PermissionType.NOTIFICATION }
        if (notificationPermissionInfo == null) {
            checkAndUpdatePermissions(context)
            return
        }

        val wasAttempted = _uiState.value.sessionAttemptedPermissions.contains(PermissionType.NOTIFICATION)
        val isGrantedNow = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (wasAttempted && !notificationPermissionInfo.isGranted && !isGrantedNow) {
            val newCount = _uiState.value.notificationDenialCount + 1
            viewModelScope.launch {
                localRepo.saveNotificationDenialCount(newCount)
                _uiState.update { it.copy(notificationDenialCount = newCount) }
            }
        }
        checkAndUpdatePermissions(context)
    }

    fun setPermissionAttemptedInSession(permissionType: PermissionType) {
        _uiState.update { it.copy(sessionAttemptedPermissions = it.sessionAttemptedPermissions + permissionType) }
    }

    fun checkAndUpdatePermissions(context: Context): Boolean {
        val permissionList = mutableListOf<PermissionInfo>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionList.add(
                PermissionInfo(
                    type = PermissionType.NOTIFICATION, title = "알림",
                    description = "타이머 진행 상황을 알림으로 표시하기 위해 필요합니다.",
                    isGranted = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                )
            )
        }
        permissionList.add(
            PermissionInfo(
                type = PermissionType.OVERLAY, title = "다른 앱 위에 표시",
                description = "공부 중 다른 앱 사용 시 경고창을 띄우기 위해 필요합니다.",
                isGranted = android.provider.Settings.canDrawOverlays(context)
            )
        )
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        permissionList.add(
            PermissionInfo(
                type = PermissionType.USAGE_STATS, title = "사용 정보 접근",
                description = "공부에 방해되는 앱 사용을 감지하기 위해 필요합니다.",
                isGranted = mode == AppOpsManager.MODE_ALLOWED
            )
        )

        val alreadyGrantedTypes = permissionList.filter { it.isGranted }.map { it.type }
        _uiState.update {
            it.copy(
                permissions = permissionList,
                sessionAttemptedPermissions = it.sessionAttemptedPermissions + alreadyGrantedTypes
            )
        }
        return permissionList.all { it.isGranted }
    }
}