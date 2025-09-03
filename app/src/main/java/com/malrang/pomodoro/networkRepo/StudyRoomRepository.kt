package com.malrang.pomodoro.networkRepo

import com.malrang.pomodoro.networkRepo.SupabaseProvider.client
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns

class StudyRoomRepository(
    private val postgrest: Postgrest,
) {

    // MARK: - StudyRoom Functions

    /**
     * 새로운 스터디룸을 생성합니다.
     * @param studyRoom 생성할 스터디룸 객체
     * @return 생성된 StudyRoom 객체
     */
    suspend fun createStudyRoom(studyRoom: StudyRoom): StudyRoom? {
        return postgrest["study_rooms"]
            .insert(studyRoom)
            .decodeSingleOrNull<StudyRoom>()
    }

    /**
     * 특정 ID의 스터디룸 정보를 가져옵니다.
     * @param roomId 가져올 스터디룸의 ID
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
     * 모든 스터디룸 목록을 가져옵니다.
     * @return StudyRoom 객체 리스트
     */
    suspend fun getAllStudyRooms(): List<StudyRoom> {
        return postgrest["study_rooms"].select().decodeList<StudyRoom>()
    }

    /**
     * 특정 사용자가 생성한 모든 스터디룸 목록을 가져옵니다.
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


    /**
     * 스터디룸 정보를 업데이트합니다.
     * @param roomId 업데이트할 스터디룸의 ID
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
     * 스터디룸을 삭제합니다.
     * @param roomId 삭제할 스터디룸의 ID
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
     * 스터디룸에 새로운 멤버를 추가합니다.
     * @param member 추가할 멤버 객체
     */
    suspend fun addMemberToStudyRoom(member: StudyRoomMember) {
        postgrest["study_room_members"].insert(member)
    }

    /**
     * 특정 스터디룸의 모든 멤버 목록을 가져옵니다.
     * @param studyRoomId 멤버를 조회할 스터디룸의 ID
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
     * 스터디룸에서 멤버를 삭제합니다.
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


    // MARK: - HabitProgress Functions

    /**
     * 습관 진행 상태를 기록합니다.
     * @param progress 기록할 진행 상태 객체
     */
    suspend fun logHabitProgress(progress: HabitProgress) {
        postgrest["habit_progress"].insert(progress)
    }

    /**
     * 특정 사용자의 특정 스터디룸에서의 모든 습관 진행 상태를 가져옵니다.
     * @param studyRoomId 스터디룸 ID
     * @param userId 사용자 ID
     * @return HabitProgress 객체 리스트
     */
    suspend fun getHabitProgressForUser(studyRoomId: String, userId: String): List<HabitProgress> {
        return postgrest["habit_progress"]
            .select {
                filter {
                    eq("study_room_id", studyRoomId)
                    eq("user_id", userId)
                }
            }
            .decodeList<HabitProgress>()
    }

    /**
     * 습관 진행 상태를 업데이트합니다.
     * @param progressId 업데이트할 진행 상태의 ID
     * @param isDone 완료 여부
     */
    suspend fun updateHabitProgress(progressId: String, isDone: Boolean) {
        postgrest["habit_progress"]
            .update({ set("is_done", isDone) }) {
                filter {
                    eq("id", progressId)
                }
            }
    }

    // MARK: - Animal Functions (주로 읽기 전용)

    /**
     * 모든 동물 목록을 가져옵니다.
     * @return Animal 객체 리스트
     */
    suspend fun getAllAnimals(): List<Animal> {
        return postgrest["animals"].select().decodeList<Animal>()
    }

    /**
     * 특정 ID의 동물 정보를 가져옵니다.
     * @param animalId 가져올 동물의 ID
     * @return Animal 객체 (없으면 null)
     */
    suspend fun getAnimalById(animalId: Long): Animal? {
        return postgrest["animals"]
            .select {
                filter {
                    eq("id", animalId)
                }
            }
            .decodeSingleOrNull<Animal>()
    }
}

