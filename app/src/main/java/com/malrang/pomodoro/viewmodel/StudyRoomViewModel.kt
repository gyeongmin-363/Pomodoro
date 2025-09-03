package com.malrang.pomodoro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malrang.pomodoro.dataclass.ui.StudyRoomUiState
import com.malrang.pomodoro.networkRepo.HabitProgress
import com.malrang.pomodoro.networkRepo.StudyRoom
import com.malrang.pomodoro.networkRepo.StudyRoomMember
import com.malrang.pomodoro.networkRepo.StudyRoomRepository
import com.malrang.pomodoro.networkRepo.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StudyRoomViewModel(
    private val networkRepo: StudyRoomRepository
) : ViewModel() {

    private val _studyRoomUiState = MutableStateFlow(StudyRoomUiState())
    val studyRoomUiState: StateFlow<StudyRoomUiState> = _studyRoomUiState.asStateFlow()

    /**
     * 딥링크를 통해 전달된 스터디룸 ID를 받아 참여 다이얼로그를 표시하도록 상태를 업데이트합니다.
     */
    fun handleInviteLink(studyRoomId: String) {
        viewModelScope.launch {
            // 1. 유효한 스터디룸 ID인지 서버에서 확인합니다.
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
                    // TODO: 이미 참여한 스터디룸이라는 토스트 메시지 등을 보여줄 수 있습니다.
                } else {
                    // 4. 참여 다이얼로그를 띄우도록 상태를 업데이트합니다.
                    _studyRoomUiState.update { it.copy(showJoinStudyRoomDialog = room) }
                }
            } else {
                // TODO: 존재하지 않는 스터디룸이라는 메시지를 표시할 수 있습니다.
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
                userStudyRooms = emptyList()
            )
        }
    }

    // MARK: - StudyRoom Functions

    /**
     * ✅ [수정] 새로운 스터디룸을 생성하고 목록을 새로고침합니다.
     * 기존의 createStudyRoomAndJoin 함수를 대체합니다.
     */
    fun createStudyRoom(studyRoom: StudyRoom) {
        viewModelScope.launch {
            // 1. 서버에 스터디룸을 생성합니다.
            val createdRoom = networkRepo.createStudyRoom(studyRoom)

            if (createdRoom != null) {
                // 2. 생성 성공 시, UI 상태를 업데이트합니다.
                studyRoom.creator_id?.let { loadUserStudyRooms(it) } // 목록 새로고침
                showCreateStudyRoomDialog(false) // 생성 다이얼로그 닫기
            } else {
                // 스터디룸 생성 실패 처리
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

    // MARK: - HabitProgress Functions

    fun logHabitProgress(progress: HabitProgress) {
        viewModelScope.launch {
            networkRepo.logHabitProgress(progress)
            loadHabitProgressForUser(progress.study_room_id!!, progress.user_id!!)
        }
    }

    fun loadHabitProgressForUser(studyRoomId: String, userId: String) {
        viewModelScope.launch {
            val progress = networkRepo.getHabitProgressForUser(studyRoomId, userId)
            _studyRoomUiState.update { it.copy(userHabitProgress = progress) }
        }
    }

    fun updateHabitProgress(progressId: String, isDone: Boolean, studyRoomId: String, userId: String) {
        viewModelScope.launch {
            networkRepo.updateHabitProgress(progressId, isDone)
            loadHabitProgressForUser(studyRoomId, userId)
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
        viewModelScope.launch {
            // TODO: 현재는 사용자가 '생성한' 룸 목록만 가져옵니다.
            //  실제 앱에서는 study_room_members 테이블에서 userId로 조회하여
            //  사용자가 '멤버로 있는' 모든 룸 목록을 가져오는 로직으로 수정이 필요합니다.
            val rooms = networkRepo.findStudyRoomsByCreator(userId)
            _studyRoomUiState.update { it.copy(userStudyRooms = rooms) }
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
                // navController.navigate("studyRoomDetail/${room.id}")
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