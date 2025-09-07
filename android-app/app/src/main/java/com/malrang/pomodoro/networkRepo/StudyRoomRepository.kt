package com.malrang.pomodoro.networkRepo

import android.R.attr.order
import android.util.Log
import com.malrang.pomodoro.networkRepo.SupabaseProvider.client
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperation
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.PostgresChangeFilter
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.selectAsFlow
import kotlinx.coroutines.flow.Flow

class StudyRoomRepository(
    private val postgrest: Postgrest,
) {

    // MARK: - StudyRoom Functions

    /**
     * 새로운 챌린지룸을 생성합니다.
     * @param studyRoom 생성할 챌린지룸 객체
     * @return 생성된 StudyRoom 객체
     */
    suspend fun createStudyRoom(studyRoom: StudyRoom): StudyRoom? {
        return postgrest["study_rooms"]
            .insert(studyRoom) {
                select() // 👈 반드시 붙여야 JSON이 반환됨
            }
            .decodeSingleOrNull<StudyRoom>()
    }

    /**
     * 특정 ID의 챌린지룸 정보를 가져옵니다.
     * @param roomId 가져올 챌린지룸의 ID
     * @return StudyRoom 객체 (없으면 null)
     */
    suspend fun getStudyRoomById(roomId: String): StudyRoom? {
        return postgrest["study_rooms"]
            .select {
                filter {
                    eq("id", roomId)
                }
            }
            .decodeSingleOrNull<StudyRoom>()
    }

    /**
     * 모든 챌린지룸 목록을 가져옵니다.
     * @return StudyRoom 객체 리스트
     */
    suspend fun getAllStudyRooms(): List<StudyRoom> {
        return postgrest["study_rooms"].select().decodeList<StudyRoom>()
    }

    /**
     * 특정 사용자가 생성한 모든 챌린지룸 목록을 가져옵니다.
     * @param creatorId 생성자의 사용자 ID
     * @return StudyRoom 객체 리스트
     */
    suspend fun findStudyRoomsByCreator(creatorId: String): List<StudyRoom> {
        return postgrest["study_rooms"]
            .select {
                filter {
                    eq("creator_id", creatorId)
                }
            }
            .decodeList<StudyRoom>()
    }

    suspend fun findStudyRoomsByMemberExcludingCreator(userId: String): List<StudyRoom> {
        // 1. 내가 속한 study_room_id 가져오기
        val memberRecords = postgrest.from("study_room_members")
            .select(columns = Columns.list("study_room_id"))
            {
                filter { eq("user_id", userId) }
            }
            .decodeList<StudyRoomMemberRef>()

        val roomIds = memberRecords.map { it.study_room_id }
        if (roomIds.isEmpty()) return emptyList()

        // 2. 생성자가 나인 방 제외하고 조회
        return postgrest.from("study_rooms")
            .select()
            {
                filter {
                    isIn("id", roomIds)
                    neq("creator_id", userId)
                }
            }
            .decodeList<StudyRoom>()
    }


    /**
     * 챌린지룸 정보를 업데이트합니다.
     * @param roomId 업데이트할 챌린지룸의 ID
     * @param updatedStudyRoom 업데이트될 정보가 담긴 객체
     */
    suspend fun updateStudyRoom(roomId: String, updatedStudyRoom: StudyRoom) {
        postgrest["study_rooms"]
            .update(updatedStudyRoom) {
                filter {
                    eq("id", roomId)
                }
            }
    }

    /**
     * 챌린지룸의 방장을 변경합니다.
     * @param roomId 변경할 챌린지룸의 ID
     * @param newCreatorId 새로운 방장의 사용자 ID
     */
    suspend fun updateRoomCreator(roomId: String, newCreatorId: String) {
        postgrest["study_rooms"]
            .update({ set("creator_id", newCreatorId) }) {
                filter {
                    eq("id", roomId)
                }
            }
    }

    /**
     * 챌린지룸을 삭제합니다.
     * @param roomId 삭제할 챌린지룸의 ID
     */
    suspend fun deleteStudyRoom(roomId: String) {
        postgrest["study_rooms"]
            .delete {
                filter {
                    eq("id", roomId)
                }
            }
    }

    // MARK: - StudyRoomMember Functions

    /**
     * 챌린지룸에 새로운 멤버를 추가합니다.
     * @param member 추가할 멤버 객체
     */
    suspend fun addMemberToStudyRoom(member: StudyRoomMember) {
        postgrest["study_room_members"].insert(member)
    }

    /**
     * 특정 챌린지룸의 모든 멤버 목록을 가져옵니다.
     * @param studyRoomId 멤버를 조회할 챌린지룸의 ID
     * @return StudyRoomMember 객체 리스트
     */
    suspend fun getStudyRoomMembers(studyRoomId: String): List<StudyRoomMember> {
        return postgrest["study_room_members"]
            .select {
                filter {
                    eq("study_room_id", studyRoomId)
                }
            }
            .decodeList<StudyRoomMember>()
    }

    /**
     * 특정 멤버의 정보를 가져옵니다.
     * @param memberId 정보를 조회할 멤버의 ID
     * @return StudyRoomMember 객체 (없으면 null)
     */
    suspend fun getMemberInfo(memberId: String): StudyRoomMember? {
        return postgrest["study_room_members"]
            .select {
                filter {
                    eq("id", memberId)
                }
            }
            .decodeSingleOrNull<StudyRoomMember>()
    }

    /**
     * 멤버의 닉네임을 변경합니다.
     * @param memberId 변경할 멤버의 ID
     * @param newNickname 새로운 닉네임
     */
    suspend fun updateMemberNickname(memberId: String, newNickname: String) {
        postgrest["study_room_members"]
            .update({ set("nickname", newNickname) }) {
                filter {
                    eq("id", memberId)
                }
            }
    }

    /**
     * 챌린지룸에서 멤버를 삭제합니다. (memberId 기반)
     * @param memberId 삭제할 멤버의 ID
     */
    suspend fun removeMemberFromStudyRoom(memberId: String) {
        postgrest["study_room_members"]
            .delete {
                filter {
                    eq("id", memberId)
                }
            }
    }

    /**
     * 챌린지룸에서 특정 사용자를 삭제하고, 관련 habit_summary 데이터도 함께 삭제합니다.
     * @param roomId 챌린지룸의 ID
     * @param userId 삭제할 사용자의 ID
     */
    suspend fun removeMemberFromStudyRoomByUserId(roomId: String, userId: String) {
        // 1. 해당 사용자의 habit_summary 데이터 먼저 삭제
        postgrest["habit_summary"]
            .delete {
                filter {
                    eq("study_room_id", roomId)
                    eq("user_id", userId)
                }
            }

        // 2. study_room_members 테이블에서 사용자 삭제
        postgrest["study_room_members"]
            .delete {
                filter {
                    eq("study_room_id", roomId)
                    eq("user_id", userId)
                }
            }
    }


    // MARK: - HabitSummary Functions

    /**
     * 특정 챌린지룸의 특정 월에 대한 모든 멤버의 습관 진행 상황을 가져옵니다.
     * @param studyRoomId 챌린지룸 ID
     * @param yearMonth "YYYY-MM" 형식의 조회할 연월
     * @return HabitSummary 객체 리스트
     */
    suspend fun getHabitProgressForMonth(studyRoomId: String, yearMonth: String): List<HabitSummary> {
        return postgrest["habit_summary"]
            .select {
                filter {
                    eq("study_room_id", studyRoomId)
                    eq("year_month", yearMonth)
                }
            }
            .decodeList()
    }

    /**
     * 습관 진행 상황을 추가하거나 업데이트합니다. (Upsert)
     * @param progress 업데이트할 HabitSummary 객체
     */
    suspend fun upsertHabitProgress(progress: HabitSummary) {
        postgrest["habit_summary"].upsert(progress)
    }

    // MARK: - Chat

    /**
     *
     * 채팅 메시지 목록을 실시간 Flow로 가져옵니다.
     * 데이터베이스에 변경이 생길 때마다 새로운 목록을 emit합니다.
     * @param studyRoomId 메시지를 조회할 챌린지룸의 ID
     * @return ChatMessage 흐름
     */
    @OptIn(SupabaseExperimental::class)
    fun getChatMessagesFlow(studyRoomId: String): Flow<List<ChatMessage>> {
        return postgrest["chat_messages"]
            .selectAsFlow(
                primaryKey = ChatMessage::id, // 데이터 구분을 위한 기본 키
                filter = FilterOperation("study_room_id", FilterOperator.EQ, studyRoomId)
            )

    }

    /**
     * 새로운 채팅 메시지를 전송합니다.
     * @param studyRoomId 메시지를 보낼 챌린지룸 ID
     * @param userId 메시지를 보내는 사용자 ID
     * @param message 보낼 메시지 내용
     */
    suspend fun sendChatMessage(studyRoomId: String, userId: String, message: String, nickname : String) {
        val chatMessage = mapOf(
            "study_room_id" to studyRoomId,
            "user_id" to userId,
            "message" to message,
            "nickname" to nickname
        )
        // created_at은 데이터베이스에서 default 값으로 자동 생성되도록 값을 보내지 않습니다.
        postgrest["chat_messages"].insert(chatMessage)
    }
}