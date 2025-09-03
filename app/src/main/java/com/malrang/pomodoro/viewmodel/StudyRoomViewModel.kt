package com.malrang.pomodoro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malrang.pomodoro.dataclass.animalInfo.AnimalsTable
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
import kotlinx.serialization.json.jsonPrimitive

// ✅ ViewModel 상속 및 StudyRoomRepository 의존성 주입
class StudyRoomViewModel(
    private val networkRepo: StudyRoomRepository
) : ViewModel() {

    // ✅ UI 상태를 관리하기 위한 StateFlow 변수 추가
    private val _studyRoomUiState = MutableStateFlow(StudyRoomUiState())
    val studyRoomUiState: StateFlow<StudyRoomUiState> = _studyRoomUiState.asStateFlow()


    fun onUserAuthenticated(user: User) {
        _studyRoomUiState.update { it.copy(currentUser = user) }
        // 이 사용자의 스터디룸 목록 로드
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
            loadStudyRoomMembers(member.study_room_id!!) // 멤버 목록 새로고침
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
            loadHabitProgressForUser(progress.study_room_id!!, progress.user_id!!)
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
            val isMember = members.any { it.user_id == userId }

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
                val finalMemberInfo = memberProfile.copy(study_room_id = createdRoom.id)

                // 3. 완성된 멤버 정보를 서버에 추가합니다.
                networkRepo.addMemberToStudyRoom(finalMemberInfo)

                // 4. UI 상태를 업데이트합니다.
                studyRoom.creator_id?.let { loadUserStudyRooms(it) } // 목록 새로고침
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
            member.user_id?.let { loadUserStudyRooms(it) }
        }
    }
}