package com.malrang.pomodoro.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Message
import com.malrang.pomodoro.dataclass.ui.StudyRoomUiState
import com.malrang.pomodoro.networkRepo.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.UUID

class StudyRoomViewModel(
    private val networkRepo: StudyRoomRepository
) : ViewModel() {

    private val _studyRoomUiState = MutableStateFlow(StudyRoomUiState())
    val studyRoomUiState: StateFlow<StudyRoomUiState> = _studyRoomUiState.asStateFlow()

    // 네비게이션 이벤트를 위한 SharedFlow
    private val _navigationEvents = MutableSharedFlow<String>()
    val navigationEvents = _navigationEvents.asSharedFlow()

    // MARK: - Chat

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private var chatJob: Job? = null

    fun disSubscribeMessage(){
        chatJob?.cancel()
        _chatMessages.value = emptyList<ChatMessage>()
    }

    fun subscribeToMessages(studyRoomId: String) {
        chatJob?.cancel() // 이전 구독이 있다면 취소

        // 로딩 시작
        _studyRoomUiState.update { it.copy(isChatLoading = true) }

        chatJob = viewModelScope.launch {
            networkRepo.getChatMessagesFlow(studyRoomId)
                .catch {
                    // 에러 처리
                    _studyRoomUiState.update { it.copy(isChatLoading = false) }
                }
                .collect { messages ->
                    _chatMessages.value = messages
                    // 데이터 수집 완료 후 로딩 종료
                    _studyRoomUiState.update { it.copy(isChatLoading = false) }
                }
        }
    }

    fun sendChatMessage(studyRoomId: String, userId: String, message: String, nickname : String) {
        viewModelScope.launch {
            try {
                networkRepo.sendChatMessage(studyRoomId, userId, message, nickname)
            } catch (e: Exception) {
                // 에러 처리
            }
        }
    }

    /**
     * 딥링크를 통해 전달된 챌린지룸 ID를 받아 참여 다이얼로그를 표시하도록 상태를 업데이트합니다.
     */
    fun handleInviteLink(studyRoomId: String) {
        viewModelScope.launch {
            // 1. 유효한 챌린지룸 ID인지 서버에서 확인합니다.
            val room = networkRepo.getStudyRoomById(studyRoomId)
            if (room != null) {
                // 2. 현재 로그인한 사용자인지 확인합니다.
                val userId = _studyRoomUiState.value.currentUser?.id
                if (userId == null) {
                    // TODO: 로그인이 필요하다는 메시지를 표시하거나 로그인 화면으로 안내할 수 있습니다.
                    return@launch
                }

                // 3. 이미 멤버인지 확인합니다.
                val members = networkRepo.getStudyRoomMembers(studyRoomId)
                val isMember = members.any { it.user_id == userId }

                if (isMember) {
                    // TODO: 이미 참여한 챌린지룸이라는 토스트 메시지 등을 보여줄 수 있습니다.
                } else {
                    // 4. 참여 다이얼로그를 띄우도록 상태를 업데이트합니다.
                    _studyRoomUiState.update { it.copy(showJoinStudyRoomDialog = room) }
                }
            } else {
                // TODO: 존재하지 않는 챌린지룸이라는 메시지를 표시할 수 있습니다.
            }
        }
    }

    fun onUserAuthenticated(user: User) {
        _studyRoomUiState.update { it.copy(currentUser = user) }
        loadUserStudyRooms(user.id)
        loadAllAnimals()
    }

    fun onUserNotAuthenticated() {
        clearUserRelatedState()
    }

    private fun clearUserRelatedState() {
        _studyRoomUiState.update {
            it.copy(
                currentUser = null,
                createdStudyRooms = emptyList(),
                joinedStudyRooms = emptyList(),
            )
        }
    }

    // MARK: - StudyRoom Functions

    /**
     * 새로운 챌린지룸을 생성하고 목록을 새로고침합니다.
     * 기존의 createStudyRoomAndJoin 함수를 대체합니다.
     */
    fun createStudyRoom(studyRoom: StudyRoom) {
        viewModelScope.launch {
            // 1. 서버에 챌린지룸을 생성합니다.
            val createdRoom = networkRepo.createStudyRoom(studyRoom)

            if (createdRoom != null) {
                // 2. 생성 성공 시, UI 상태를 업데이트합니다.
                studyRoom.creator_id?.let { loadUserStudyRooms(it) } // 목록 새로고침
                showCreateStudyRoomDialog(false) // 생성 다이얼로그 닫기
            } else {
                // 챌린지룸 생성 실패 처리
            }
        }
    }

    fun loadStudyRoomById(roomId: String) {
        viewModelScope.launch {
            val room = networkRepo.getStudyRoomById(roomId)
            _studyRoomUiState.update { it.copy(currentStudyRoom = room) }
        }
    }

    fun updateStudyRoom(roomId: String, updatedStudyRoom: StudyRoom) {
        viewModelScope.launch {
            networkRepo.updateStudyRoom(roomId, updatedStudyRoom)
            loadStudyRoomById(roomId)
        }
    }

    fun deleteStudyRoom(roomId: String) {
        viewModelScope.launch {
            networkRepo.deleteStudyRoom(roomId)
            if (_studyRoomUiState.value.currentStudyRoom?.id == roomId) {
                _studyRoomUiState.update { it.copy(currentStudyRoom = null, currentRoomMembers = emptyList()) }
            }
        }
    }

    // MARK: - StudyRoomMember Functions

    fun addMemberToStudyRoom(member: StudyRoomMember) {
        viewModelScope.launch {
            networkRepo.addMemberToStudyRoom(member)
            loadStudyRoomMembers(member.study_room_id!!)
        }
    }

    fun loadStudyRoomMembers(studyRoomId: String) {
        viewModelScope.launch {
            val members = networkRepo.getStudyRoomMembers(studyRoomId)
            _studyRoomUiState.update { it.copy(currentRoomMembers = members) }
        }
    }

    fun removeMemberFromStudyRoom(memberId: String, studyRoomId: String) {
        viewModelScope.launch {
            networkRepo.removeMemberFromStudyRoom(memberId)
            loadStudyRoomMembers(studyRoomId)
        }
    }

    // MARK: - HabitSummary Functions
    /**
     * 특정 월의 챌린지룸 멤버들의 챌린지 수행 현황을 불러옵니다.
     */
    fun loadHabitSummaryForMonth(studyRoomId: String, date: LocalDate) {
        viewModelScope.launch {
            val yearMonth = date.format(DateTimeFormatter.ofPattern("yyyy-MM"))
            val progressList = networkRepo.getHabitProgressForMonth(studyRoomId, yearMonth)
            // user_id를 키로 사용하는 맵으로 변환하여 UI 상태 업데이트
            _studyRoomUiState.update { it.copy(habitProgressMap = progressList.associateBy { p -> p.user_id!! }) }
        }
    }

    /**
     * '오늘 챌린지 완료하기' 버튼을 눌렀을 때 호출됩니다.
     */
    fun completeTodayChallenge(studyRoomId: String) {
        viewModelScope.launch {
            val userId = _studyRoomUiState.value.currentUser?.id ?: return@launch
            val today = LocalDate.now()
            val yearMonth = today.format(DateTimeFormatter.ofPattern("yyyy-MM"))
            val dayOfMonth = today.dayOfMonth // 1-based index

            // 1. 기존 진행 상황 가져오기
            val currentProgress = _studyRoomUiState.value.habitProgressMap[userId]
            val daysInMonth = YearMonth.from(today).lengthOfMonth()

            // 2. 오늘 날짜에 해당하는 progress 문자열 업데이트
            val newDailyProgress = currentProgress?.daily_progress?.let {
                val progressChars = it.toCharArray()
                progressChars[dayOfMonth - 1] = '1' // 0-based index
                String(progressChars)
            } ?: buildString {
                // 기존 데이터가 없으면 새로 생성
                repeat(daysInMonth) { day ->
                    append(if (day == dayOfMonth - 1) '1' else '0')
                }
            }

            // 3. Supabase에 upsert 요청
            val progressToUpsert = HabitSummary(
                id = currentProgress?.id ?: UUID.randomUUID().toString(),
                study_room_id = studyRoomId,
                user_id = userId,
                year_month = yearMonth,
                daily_progress = newDailyProgress
            )
            networkRepo.upsertHabitProgress(progressToUpsert)

            // 4. UI 상태 새로고침
            loadHabitSummaryForMonth(studyRoomId, today)
        }
    }


    // MARK: - Animal Functions

    fun loadAllAnimals() {
        viewModelScope.launch {
            val animals = networkRepo.getAllAnimals()
            _studyRoomUiState.update { it.copy(allAnimals = animals) }
        }
    }

    // MARK: - UI Control Functions

    fun loadUserStudyRooms(userId: String) {
        // 1. 로딩 시작: isLoading을 true로 설정
        _studyRoomUiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            val createdRooms = networkRepo.findStudyRoomsByCreator(userId)
            val joinedRooms = networkRepo.findStudyRoomsByMemberExcludingCreator(userId)

            _studyRoomUiState.update {
                it.copy(
                    createdStudyRooms = createdRooms,
                    joinedStudyRooms = joinedRooms,
                    isLoading = false // 로딩 완료
                )
            }
        }
    }

    fun showCreateStudyRoomDialog(show: Boolean) {
        _studyRoomUiState.update { it.copy(showCreateStudyRoomDialog = show) }
    }

    fun onJoinStudyRoom(room: StudyRoom) {
        viewModelScope.launch {
            val userId = _studyRoomUiState.value.currentUser?.id ?: return@launch
            val members = networkRepo.getStudyRoomMembers(room.id)
            val isMember = members.any { it.user_id == userId }

            if (isMember) {
                // ✅ [수정] NavController 직접 호출 대신, 네비게이션 이벤트를 발생시킵니다.
                _navigationEvents.emit("studyRoomDetail/${room.id}")
            } else {
                _studyRoomUiState.update { it.copy(showJoinStudyRoomDialog = room) }
            }
        }
    }

    fun dismissJoinStudyRoomDialog() {
        _studyRoomUiState.update { it.copy(showJoinStudyRoomDialog = null) }
    }

    fun joinStudyRoom(member: StudyRoomMember) {
        viewModelScope.launch {
            networkRepo.addMemberToStudyRoom(member)
            dismissJoinStudyRoomDialog()
            member.user_id?.let { loadUserStudyRooms(it) }
        }
    }
}