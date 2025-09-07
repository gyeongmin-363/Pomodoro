package com.malrang.pomodoro.dataclass.ui

import com.malrang.pomodoro.networkRepo.User
import com.malrang.pomodoro.networkRepo.StudyRoom
import com.malrang.pomodoro.networkRepo.StudyRoomMember
import com.malrang.pomodoro.networkRepo.HabitSummary

/**
 * 챌린지룸과 관련된 UI 상태를 나타내는 데이터 클래스입니다.
 * 주로 네트워크를 통해 받아오는 데이터를 관리합니다.
 *
 * @property currentUser 현재 로그인한 사용자의 정보입니다.
 * @property currentStudyRoom 현재 참여하고 있는 챌린지룸의 정보입니다.
 * @property currentRoomMembers 현재 챌린지룸의 멤버 목록입니다.
 * @property allAnimals 데이터베이스에 있는 모든 동물 목록입니다. (앱 전역에서 사용)
 * @property habitProgressMap 현재 챌린지룸 멤버들의 월별 챌린지 현황 맵입니다. (Key: user_id, Value: HabitSummary)
 */
data class StudyRoomUiState(
    val currentUser: User? = null,
    val currentStudyRoom: StudyRoom? = null,
    val currentRoomMembers: List<StudyRoomMember> = emptyList(),
    // 멤버들의 챌린지 현황을 user_id를 키로 하여 저장하는 맵
    val habitProgressMap: Map<String, HabitSummary> = emptyMap(),

    // [UserScreen에서 사용할 상태]
    val createdStudyRooms: List<StudyRoom> = emptyList(), // 사용자가 생성한 챌린지룸 목록
    val joinedStudyRooms: List<StudyRoom> = emptyList(),  // 사용자가 참여만 한 챌린지룸 목록
    val showCreateStudyRoomDialog: Boolean = false,       // 챌린지룸 생성 다이얼로그 표시 여부
    val showJoinStudyRoomDialog: StudyRoom? = null,       // 참여할 챌린지룸 정보 (null이 아니면 다이얼로그 표시)

    val isLoading: Boolean = true, //UserScreen에서 채팅방 불러오기 로딩
    val isChatLoading: Boolean = false, // 채팅 로딩 상태 추가
)