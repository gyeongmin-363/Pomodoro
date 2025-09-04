package com.malrang.pomodoro.dataclass.ui

import com.malrang.pomodoro.networkRepo.User
import com.malrang.pomodoro.networkRepo.StudyRoom
import com.malrang.pomodoro.networkRepo.StudyRoomMember
import com.malrang.pomodoro.networkRepo.HabitProgress
import com.malrang.pomodoro.networkRepo.Animal

/**
 * 스터디룸과 관련된 UI 상태를 나타내는 데이터 클래스입니다.
 * 주로 네트워크를 통해 받아오는 데이터를 관리합니다.
 *
 * @property currentUser 현재 로그인한 사용자의 정보입니다.
 * @property currentStudyRoom 현재 참여하고 있는 스터디룸의 정보입니다.
 * @property currentRoomMembers 현재 스터디룸의 멤버 목록입니다.
 * @property userHabitProgress 사용자의 습관 진행 상태 목록입니다.
 * @property allAnimals 데이터베이스에 있는 모든 동물 목록입니다. (앱 전역에서 사용)
 */
data class StudyRoomUiState(
    val currentUser: User? = null,
    val currentStudyRoom: StudyRoom? = null,
    val currentRoomMembers: List<StudyRoomMember> = emptyList(),
    val userHabitProgress: List<HabitProgress> = emptyList(),
    val allAnimals: List<Animal> = emptyList(),

    // [추가] UserScreen에서 사용할 상태
    val userStudyRooms: List<StudyRoom> = emptyList(), // 사용자가 속한 스터디룸 목록
    val showCreateStudyRoomDialog: Boolean = false, // 스터디룸 생성 다이얼로그 표시 여부
    val showJoinStudyRoomDialog: StudyRoom? = null // 참여할 스터디룸 정보 (null이 아니면 다이얼로그 표시)
)
