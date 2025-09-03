package com.malrang.pomodoro.viewmodel

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malrang.pomodoro.dataclass.animalInfo.AnimalsTable
import com.malrang.pomodoro.dataclass.sprite.SpriteState
import com.malrang.pomodoro.dataclass.ui.Mode
import com.malrang.pomodoro.dataclass.ui.PermissionInfo
import com.malrang.pomodoro.dataclass.ui.PermissionType
import com.malrang.pomodoro.dataclass.ui.PomodoroUiState
import com.malrang.pomodoro.dataclass.ui.Screen
import com.malrang.pomodoro.dataclass.ui.Settings
import com.malrang.pomodoro.dataclass.ui.StudyRoomUiState
import com.malrang.pomodoro.dataclass.ui.WorkPreset
import com.malrang.pomodoro.localRepo.PomodoroRepository
import com.malrang.pomodoro.networkRepo.HabitProgress
import com.malrang.pomodoro.networkRepo.StudyRoom
import com.malrang.pomodoro.networkRepo.StudyRoomMember
import com.malrang.pomodoro.networkRepo.StudyRoomRepository
import com.malrang.pomodoro.networkRepo.User
import com.malrang.pomodoro.service.TimerService
import com.malrang.pomodoro.service.TimerServiceProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonPrimitive
import kotlin.random.Random

class PomodoroViewModel(
    private val localRepo: PomodoroRepository,
    private val timerService: TimerServiceProvider,
    private val networkRepo: StudyRoomRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PomodoroUiState())
    val uiState: StateFlow<PomodoroUiState> = _uiState.asStateFlow()

    // 네트워크(스터디룸) 관련 상태 관리
    private val _studyRoomUiState = MutableStateFlow(StudyRoomUiState())
    val studyRoomUiState: StateFlow<StudyRoomUiState> = _studyRoomUiState.asStateFlow()

    private val _sessionAttemptedPermissions = MutableStateFlow<Set<PermissionType>>(emptySet())
    val sessionAttemptedPermissions: StateFlow<Set<PermissionType>> = _sessionAttemptedPermissions.asStateFlow()
    private val _notificationDenialCount = MutableStateFlow(0)
    val notificationDenialCount: StateFlow<Int> = _notificationDenialCount.asStateFlow()
    private val _editingWorkPreset = MutableStateFlow<WorkPreset?>(null)
    val editingWorkPreset: StateFlow<WorkPreset?> = _editingWorkPreset.asStateFlow()
    private val _draftSettings = MutableStateFlow<Settings?>(null)
    val draftSettings: StateFlow<Settings?> = _draftSettings.asStateFlow()

    init {
        viewModelScope.launch {
            _notificationDenialCount.value = localRepo.loadNotificationDenialCount()

            val seenIds = localRepo.loadSeenIds()
            val daily = localRepo.loadDailyStats()
            val presets = localRepo.loadWorkPresets()
            val whitelistedApps = localRepo.loadWhitelistedApps()
            val currentWorkId = localRepo.loadCurrentWorkId() ?: presets.firstOrNull()?.id
            val sprites = localRepo.loadActiveSprites()
            val useGrassBackground = localRepo.loadUseGrassBackground()

            val seenAnimals = seenIds.mapNotNull { id -> AnimalsTable.byId(id) }
            val currentWork = presets.find { it.id == currentWorkId }
            val currentSettings = currentWork?.settings ?: Settings()

            _uiState.update {
                it.copy(
                    collectedAnimals = seenAnimals.toSet(),
                    dailyStats = daily,
                    settings = currentSettings,
                    workPresets = presets,
                    currentWorkId = currentWorkId,
                    activeSprites = sprites,
                    useGrassBackground = useGrassBackground,
                    whitelistedApps = whitelistedApps
                )
            }

            if (TimerService.isServiceActive()) {
                timerService.requestStatus()
            } else {
                // [수정] 서비스가 비활성 상태일 때, 저장된 타이머 상태를 불러옵니다.
                val savedState = localRepo.loadTimerState()
                if (savedState != null) {
                    _uiState.update {
                        it.copy(
                            timeLeft = savedState.timeLeft,
                            currentMode = savedState.currentMode,
                            totalSessions = savedState.totalSessions,
                            isRunning = false,
                            isPaused = true, // 저장된 상태는 항상 '일시정지' 상태로 로드
                            isTimerStartedOnce = savedState.timeLeft < (it.settings.studyTime * 60)
                        )
                    }
                } else {
                    // 저장된 상태가 없으면 초기화
                    reset()
                }
            }
        }
    }

    /**
     * AuthViewModel의 인증 상태를 관찰하고,
     * 인증이 완료되면 currentUser를 설정하고 관련 데이터를 로드합니다.
     */
    fun observeAuthState(authViewModel: AuthViewModel) {
        viewModelScope.launch {
            authViewModel.uiState.collect { authState ->
                when (authState) {
                    is AuthViewModel.AuthState.Authenticated -> {
                        // 인증 성공! UserInfo 객체에서 필요한 정보를 추출합니다.
                        val userInfo = authState.user
                        if (userInfo != null) {
                            // UserInfo에서 이름 추출 (userMetadata는 JSON이므로 파싱 필요)
                            val userName = userInfo.userMetadata?.get("name")?.jsonPrimitive?.content ?: "사용자"

                            // 앱 내부용 User 모델 객체 생성
                            val appUser = User(id = userInfo.id, name = userName)

                            // UI 상태 업데이트
                            _studyRoomUiState.update { it.copy(currentUser = appUser) }

                            // 이 사용자의 스터디룸 목록 로드
                            loadUserStudyRooms(appUser.id)
                            loadAllAnimals()

                        } else {
                            // 사용자는 인증되었지만 정보가 없는 예외적인 경우
                            // 로그아웃 처리 또는 기본값 설정
                            clearUserRelatedState()
                        }
                    }
                    is AuthViewModel.AuthState.NotAuthenticated -> {
                        // 로그아웃 상태이므로 사용자 관련 모든 정보를 초기화합니다.
                        clearUserRelatedState()
                    }
                    else -> {
                        // Idle, Loading, Error 등의 상태 처리
                    }
                }
            }
        }
    }

    private fun clearUserRelatedState() {
        _studyRoomUiState.update {
            it.copy(
                currentUser = null,
                userStudyRooms = emptyList()
                // ... 기타 사용자 관련 상태 초기화
            )
        }
    }


    // MARK: - StudyRoom Functions

    /**
     * 새로운 스터디룸을 생성합니다.
     */
    fun createStudyRoom(studyRoom: StudyRoom) {
        viewModelScope.launch {
            val createdRoom = networkRepo.createStudyRoom(studyRoom)
            if (createdRoom != null) {
                _studyRoomUiState.update { it.copy(currentStudyRoom = createdRoom) }
            }
        }
    }

    /**
     * 특정 ID의 스터디룸 정보를 불러옵니다.
     */
    fun loadStudyRoomById(roomId: String) {
        viewModelScope.launch {
            val room = networkRepo.getStudyRoomById(roomId)
            _studyRoomUiState.update { it.copy(currentStudyRoom = room) }
        }
    }

    /**
     * 스터디룸 정보를 업데이트합니다.
     */
    fun updateStudyRoom(roomId: String, updatedStudyRoom: StudyRoom) {
        viewModelScope.launch {
            networkRepo.updateStudyRoom(roomId, updatedStudyRoom)
            loadStudyRoomById(roomId) // 정보 업데이트 후 새로고침
        }
    }

    /**
     * 스터디룸을 삭제합니다.
     */
    fun deleteStudyRoom(roomId: String) {
        viewModelScope.launch {
            networkRepo.deleteStudyRoom(roomId)
            if (_studyRoomUiState.value.currentStudyRoom?.id == roomId) {
                _studyRoomUiState.update { it.copy(currentStudyRoom = null, currentRoomMembers = emptyList()) }
            }
        }
    }

    // MARK: - StudyRoomMember Functions

    /**
     * 스터디룸에 멤버를 추가합니다.
     */
    fun addMemberToStudyRoom(member: StudyRoomMember) {
        viewModelScope.launch {
            networkRepo.addMemberToStudyRoom(member)
            loadStudyRoomMembers(member.studyRoomId!!) // 멤버 목록 새로고침
        }
    }

    /**
     * 특정 스터디룸의 모든 멤버 목록을 불러옵니다.
     */
    fun loadStudyRoomMembers(studyRoomId: String) {
        viewModelScope.launch {
            val members = networkRepo.getStudyRoomMembers(studyRoomId)
            _studyRoomUiState.update { it.copy(currentRoomMembers = members) }
        }
    }

    /**
     * 스터디룸에서 멤버를 제거합니다.
     */
    fun removeMemberFromStudyRoom(memberId: String, studyRoomId: String) {
        viewModelScope.launch {
            networkRepo.removeMemberFromStudyRoom(memberId)
            loadStudyRoomMembers(studyRoomId) // 멤버 목록 새로고침
        }
    }

    // MARK: - HabitProgress Functions

    /**
     * 습관 진행 상태를 기록합니다.
     */
    fun logHabitProgress(progress: HabitProgress) {
        viewModelScope.launch {
            networkRepo.logHabitProgress(progress)
            loadHabitProgressForUser(progress.studyRoomId!!, progress.userId!!)
        }
    }

    /**
     * 특정 사용자의 습관 진행 상태를 불러옵니다.
     */
    fun loadHabitProgressForUser(studyRoomId: String, userId: String) {
        viewModelScope.launch {
            val progress = networkRepo.getHabitProgressForUser(studyRoomId, userId)
            _studyRoomUiState.update { it.copy(userHabitProgress = progress) }
        }
    }

    /**
     * 습관 진행 상태를 업데이트합니다.
     */
    fun updateHabitProgress(progressId: String, isDone: Boolean, studyRoomId: String, userId: String) {
        viewModelScope.launch {
            networkRepo.updateHabitProgress(progressId, isDone)
            loadHabitProgressForUser(studyRoomId, userId) // 상태 업데이트 후 새로고침
        }
    }

    // MARK: - Animal Functions

    /**
     * 모든 동물 목록을 불러옵니다. (주로 앱 초기화 시 사용)
     */
    fun loadAllAnimals() {
        viewModelScope.launch {
            val animals = networkRepo.getAllAnimals()
            _studyRoomUiState.update { it.copy(allAnimals = animals) }
        }
    }

    // MARK: - StudyRoom Functions

    /**
     * 특정 사용자가 속한 스터디룸 목록을 불러옵니다.
     */
    fun loadUserStudyRooms(userId: String) {
        viewModelScope.launch {
            // TODO: 현재는 사용자가 '생성한' 룸 목록만 가져옵니다.
            //  실제 앱에서는 study_room_members 테이블에서 userId로 조회하여
            //  사용자가 '멤버로 있는' 모든 룸 목록을 가져오는 로직으로 수정이 필요합니다.
            val rooms = networkRepo.findStudyRoomsByCreator(userId)
            _studyRoomUiState.update { it.copy(userStudyRooms = rooms) }
        }
    }

    /**
     * 스터디룸 생성 다이얼로그의 표시 여부를 설정합니다.
     */
    fun showCreateStudyRoomDialog(show: Boolean) {
        _studyRoomUiState.update { it.copy(showCreateStudyRoomDialog = show) }
    }

    /**
     * 사용자가 특정 스터디룸을 클릭했을 때 참여 다이얼로그를 표시합니다.
     */
    fun onJoinStudyRoom(room: StudyRoom) {
        viewModelScope.launch {
            // 이미 참여한 멤버인지 확인하는 로직 추가 (선택사항)
            val userId = _studyRoomUiState.value.currentUser?.id ?: return@launch
            val members = networkRepo.getStudyRoomMembers(room.id)
            val isMember = members.any { it.userId == userId }

            if (isMember) {
                // 이미 멤버라면 상세 화면으로 바로 이동
                // navController.navigate("studyRoomDetail/${room.id}")
            } else {
                // 멤버가 아니면 참여 다이얼로그 표시
                _studyRoomUiState.update { it.copy(showJoinStudyRoomDialog = room) }
            }
        }
    }

    /**
     * 스터디룸 참여 다이얼로그를 닫습니다.
     */
    fun dismissJoinStudyRoomDialog() {
        _studyRoomUiState.update { it.copy(showJoinStudyRoomDialog = null) }
    }

    /**
     * 새로운 스터디룸을 생성하고 사용자를 멤버로 추가합니다.
     * 이 함수는 이제 StudyRoom 정보와 첫 멤버의 프로필 정보를 모두 받습니다.
     */
    fun createStudyRoomAndJoin(studyRoom: StudyRoom, memberProfile: StudyRoomMember) {
        viewModelScope.launch {
            // 1. 서버에 스터디룸을 생성합니다.
            val createdRoom = networkRepo.createStudyRoom(studyRoom)

            if (createdRoom != null) {
                // 2. 생성 성공 시, 반환된 스터디룸의 ID를 멤버 정보에 할당합니다.
                val finalMemberInfo = memberProfile.copy(studyRoomId = createdRoom.id)

                // 3. 완성된 멤버 정보를 서버에 추가합니다.
                networkRepo.addMemberToStudyRoom(finalMemberInfo)

                // 4. UI 상태를 업데이트합니다.
                studyRoom.creatorId?.let { loadUserStudyRooms(it) } // 목록 새로고침
                dismissJoinStudyRoomDialog() // 참여 다이얼로그 닫기
                showCreateStudyRoomDialog(false) // 생성 다이얼로그 닫기

                // 5. (선택사항) 생성된 스터디룸의 상세 화면으로 바로 이동할 수 있습니다.
                // navController.navigate("studyRoomDetail/${createdRoom.id}")
            } else {
                // 스터디룸 생성 실패 처리
            }
        }
    }

    /**
     * 사용자를 특정 스터디룸의 멤버로 추가합니다. (프로필 설정 완료)
     */
    fun joinStudyRoom(member: StudyRoomMember) {
        viewModelScope.launch {
            networkRepo.addMemberToStudyRoom(member)
            // 참여 완료 후 다이얼로그를 닫고, 스터디룸 목록을 새로고침합니다.
            dismissJoinStudyRoomDialog()
            member.userId?.let { loadUserStudyRooms(it) }
        }
    }


    /**
     * [추가] 권한을 확인하고, 필요하다면 권한 화면으로 이동시키는 함수
     */
    fun checkPermissionsAndNavigateIfNeeded(context: Context) {
        val allGranted = checkAndupdatePermissions(context)
        if (!allGranted) {
            // 권한이 하나라도 없으면 권한 화면으로 보냅니다.
            showScreen(Screen.Permission)
        } else {
            // 모든 권한이 있고, 현재 화면이 혹시 권한 화면이었다면 메인으로 보냅니다.
            if (_uiState.value.currentScreen == Screen.Permission) {
                showScreen(Screen.Main)
            }
            // 이미 메인 화면이라면 아무것도 하지 않습니다.
        }
    }

    fun onPermissionRequestResult(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            checkAndupdatePermissions(context)
            return
        }

        val notificationPermissionInfo = uiState.value.permissions.find { it.type == PermissionType.NOTIFICATION }
        if (notificationPermissionInfo == null) {
            checkAndupdatePermissions(context)
            return
        }

        val wasAttempted = sessionAttemptedPermissions.value.contains(PermissionType.NOTIFICATION)
        val isGrantedNow = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (wasAttempted && !notificationPermissionInfo.isGranted && !isGrantedNow) {
            val newCount = _notificationDenialCount.value + 1
            viewModelScope.launch {
                localRepo.saveNotificationDenialCount(newCount)
                _notificationDenialCount.value = newCount
            }
        }
        checkAndupdatePermissions(context)
    }
    fun setPermissionAttemptedInSession(permissionType: PermissionType) {
        _sessionAttemptedPermissions.update { it + permissionType }
    }
    fun checkAndupdatePermissions(context: Context): Boolean {
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
        _uiState.update { it.copy(permissions = permissionList) }
        val alreadyGrantedTypes = permissionList.filter { it.isGranted }.map { it.type }
        _sessionAttemptedPermissions.update { currentAttempts -> currentAttempts + alreadyGrantedTypes }
        return permissionList.all { it.isGranted }
    }
    fun initializeDraftSettings() {
        val settingsToEdit = _editingWorkPreset.value?.settings ?: _uiState.value.settings
        _draftSettings.value = settingsToEdit
    }
    fun refreshActiveSprites() {
        viewModelScope.launch {
            val updatedSprites = localRepo.loadActiveSprites()
            _uiState.update { it.copy(activeSprites = updatedSprites) }
        }
    }
    fun requestTimerStatus() {
        if (TimerService.isServiceActive()) {
            timerService.requestStatus()
        }
    }


    private suspend fun performResetLogic(settings: Settings) {
        // [수정] 저장된 타이머 상태와 활성 스프라이트를 모두 삭제합니다.
        localRepo.clearTimerState()
        localRepo.saveActiveSprites(emptyList())

        _uiState.update {
            it.copy(
                timeLeft = settings.studyTime * 60,
                totalSessions = 0,
                isRunning = false,
                isPaused = true,
                isTimerStartedOnce = false,
                currentMode = Mode.STUDY,
                settings = settings,
                activeSprites = emptyList()
            )
        }
        timerService.resetCompletely(settings)
    }

    fun reset() {
        viewModelScope.launch {
            val currentSettings = _uiState.value.workPresets.find { it.id == _uiState.value.currentWorkId }?.settings ?: Settings()
            performResetLogic(currentSettings)
        }
    }

    // ... (saveSettingsAndReset 이하 나머지 코드는 동일) ...

    fun startTimer() {
        if (_uiState.value.isRunning) return
        val s = _uiState.value
        _uiState.update { it.copy(isRunning = true, isPaused = false, isTimerStartedOnce = true) }
        timerService.start(s.timeLeft, s.settings, s.currentMode, s.totalSessions)
    }

    fun pauseTimer() {
        // [수정] 일시정지 시 동물들의 현재 상태(위치 등)를 저장합니다.
        viewModelScope.launch {
            localRepo.saveActiveSprites(uiState.value.activeSprites)
        }
        _uiState.update { it.copy(isRunning = false, isPaused = true) }
        timerService.pause()
    }

    // ... (이하 나머지 코드는 동일하여 생략) ...
    fun saveSettingsAndReset() {
        viewModelScope.launch {
            val settingsToSave = _draftSettings.value ?: return@launch
            val editingId = _editingWorkPreset.value?.id
            val currentId = _uiState.value.currentWorkId
            val presetIdToUpdate = editingId ?: currentId
            val updatedPresets = _uiState.value.workPresets.map { preset ->
                if (preset.id == presetIdToUpdate) {
                    preset.copy(settings = settingsToSave)
                } else {
                    preset
                }
            }
            localRepo.saveWorkPresets(updatedPresets)
            _uiState.update { it.copy(workPresets = updatedPresets) }
            val newActiveSettings = updatedPresets.find { it.id == currentId }?.settings ?: Settings()
            performResetLogic(newActiveSettings)
            clearDraftSettings()
        }
    }
    fun clearDraftSettings() {
        _draftSettings.value = null
        _editingWorkPreset.value = null
    }
    private fun updateDraftSettings(transform: Settings.() -> Settings) {
        _draftSettings.update { it?.transform() }
    }
    fun addToWhitelist(packageName: String) {
        viewModelScope.launch {
            val updatedWhitelist = _uiState.value.whitelistedApps + packageName
            localRepo.saveWhitelistedApps(updatedWhitelist)
            _uiState.update { it.copy(whitelistedApps = updatedWhitelist) }
        }
    }
    fun removeFromWhitelist(packageName: String) {
        viewModelScope.launch {
            val updatedWhitelist = _uiState.value.whitelistedApps - packageName
            localRepo.saveWhitelistedApps(updatedWhitelist)
            _uiState.update { it.copy(whitelistedApps = updatedWhitelist) }
        }
    }
    fun selectWorkPreset(presetId: String) {
        viewModelScope.launch {
            localRepo.saveCurrentWorkId(presetId)
            _uiState.update {
                it.copy(currentWorkId = presetId)
            }
            reset()
        }
    }
    fun addWorkPreset() {
        viewModelScope.launch {
            val newPreset = WorkPreset(name = "새 Work", settings = Settings())
            val updatedPresets = _uiState.value.workPresets + newPreset
            localRepo.saveWorkPresets(updatedPresets)
            _uiState.update { it.copy(workPresets = updatedPresets) }
        }
    }
    fun deleteWorkPreset(id: String) {
        viewModelScope.launch {
            val updatedPresets = _uiState.value.workPresets.filterNot { it.id == id }
            localRepo.saveWorkPresets(updatedPresets)
            _uiState.update { it.copy(workPresets = updatedPresets) }
            if (_uiState.value.currentWorkId == id) {
                selectWorkPreset(updatedPresets.firstOrNull()?.id ?: "")
            }
        }
    }
    fun updateWorkPresetName(id: String, newName: String) {
        viewModelScope.launch {
            val updatedPresets = _uiState.value.workPresets.map {
                if (it.id == id) it.copy(name = newName) else it
            }
            localRepo.saveWorkPresets(updatedPresets)
            _uiState.update { it.copy(workPresets = updatedPresets) }
        }
    }
    fun startEditingWorkPreset(id: String) {
        val preset = _uiState.value.workPresets.find { it.id == id }
        _editingWorkPreset.value = preset
    }
    fun stopEditingWorkPreset() {
        _editingWorkPreset.value = null
        val currentSettings = _uiState.value.workPresets.find { it.id == _uiState.value.currentWorkId }?.settings ?: Settings()
        _uiState.update { it.copy(settings = currentSettings) }
    }
    fun updateBlockMode(mode: com.malrang.pomodoro.dataclass.ui.BlockMode) = updateDraftSettings { copy(blockMode = mode) }
    fun updateStudyTime(v: Int) = updateDraftSettings { copy(studyTime = v) }
    fun updateShortBreakTime(v: Int) = updateDraftSettings { copy(shortBreakTime = v) }
    fun updateLongBreakTime(v: Int) = updateDraftSettings { copy(longBreakTime = v) }
    fun updateLongBreakInterval(v: Int) = updateDraftSettings { copy(longBreakInterval = v) }
    fun toggleSound(b: Boolean) = updateDraftSettings { copy(soundEnabled = b) }
    fun toggleVibration(b: Boolean) = updateDraftSettings { copy(vibrationEnabled = b) }
    fun toggleAutoStart(b: Boolean) = updateDraftSettings { copy(autoStart = b) }
    fun toggleBackground() {
        viewModelScope.launch {
            val newPreference = !_uiState.value.useGrassBackground
            localRepo.saveUseGrassBackground(newPreference)
            _uiState.update { it.copy(useGrassBackground = newPreference) }
        }
    }
    fun skipSession() {
        val s = _uiState.value
        val nextMode: Mode
        val nextTime: Int
        var newTotalSessions = s.totalSessions
        if (s.currentMode == Mode.STUDY) {
            newTotalSessions++
            val isLongBreakTime = newTotalSessions > 0 && newTotalSessions % s.settings.longBreakInterval == 0
            nextMode = if (isLongBreakTime) Mode.LONG_BREAK else Mode.SHORT_BREAK
            nextTime = if (isLongBreakTime) s.settings.longBreakTime else s.settings.shortBreakTime
        } else {
            nextMode = Mode.STUDY
            nextTime = s.settings.studyTime
        }
        _uiState.update {
            it.copy(
                currentMode = nextMode, totalSessions = newTotalSessions, timeLeft = nextTime * 60,
                isRunning = false, isPaused = false
            )
        }
        timerService.skip(s.currentMode, newTotalSessions)
    }
    fun updateTimerStateFromService(timeLeft: Int, isRunning: Boolean, currentMode: Mode, totalSessions: Int) {
        _uiState.update {
            it.copy(
                timeLeft = timeLeft, isRunning = isRunning, isPaused = !isRunning && timeLeft > 0,
                currentMode = currentMode, totalSessions = totalSessions
            )
        }
    }
    fun showScreen(s: Screen) { _uiState.update { it.copy(currentScreen = s) } }
    fun updateSprites(deltaSec: Float, widthPx: Int, heightPx: Int) {
        _uiState.update { s ->
            val updated = s.activeSprites.map { sp ->
                var nx = sp.x + sp.vx * deltaSec
                var ny = sp.y + sp.vy * deltaSec
                var vx = sp.vx
                var vy = sp.vy
                val margin = 16f
                if (nx < margin) { nx = margin; vx = -vx }
                if (ny < margin) { ny = margin; vy = -vy }
                if (nx > widthPx - margin) { nx = widthPx - margin; vx = -vx }
                if (ny > heightPx - margin) { ny = heightPx - margin; vy = -vy }
                val nextState = if (sp.spriteState == SpriteState.IDLE && Random.nextFloat() < 0.003f) {
                    SpriteState.JUMP
                } else {
                    sp.spriteState
                }
                sp.copy(x = nx, y = ny, vx = vx, vy = vy, spriteState = nextState)
            }
            s.copy(activeSprites = updated)
        }
    }
    fun onJumpFinished(spriteId: String) {
        _uiState.update { s ->
            val updated = s.activeSprites.map { sp ->
                if (sp.id == spriteId) sp.copy(spriteState = SpriteState.IDLE) else sp
            }
            s.copy(activeSprites = updated)
        }
    }
}