package com.malrang.pomodoro.localRepo

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.malrang.pomodoro.dataclass.ui.BlockMode
import com.malrang.pomodoro.dataclass.ui.DailyStat
import com.malrang.pomodoro.dataclass.ui.Mode
import com.malrang.pomodoro.dataclass.ui.Settings
import com.malrang.pomodoro.dataclass.ui.WorkPreset
import com.malrang.pomodoro.localRepo.room.PomodoroDatabase
import com.malrang.pomodoro.localRepo.room.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Preferences DataStore 인스턴스
 * (설정값 등 간단한 데이터 관리)
 */
val Context.dataStore by preferencesDataStore(name = "pomodoro_ds")

object DSKeys {
    val CURRENT_WORK_ID = stringPreferencesKey("current_work_id")
    val BLOCKED_APPS = stringSetPreferencesKey("blocked_apps")
    val NOTIFICATION_PERMISSION_DENIAL_COUNT = intPreferencesKey("notification_permission_denial_count")
    val SAVED_TIME_LEFT = intPreferencesKey("saved_time_left")
    val SAVED_CURRENT_MODE = stringPreferencesKey("saved_current_mode")
    val SAVED_TOTAL_SESSIONS = intPreferencesKey("saved_total_sessions")
    val ACTIVE_BLOCK_MODE = stringPreferencesKey("active_block_mode")
}

data class SavedTimerState(val timeLeft: Int, val currentMode: Mode, val totalSessions: Int)

/**
 * Repository: DataStore와 Room Database를 통합 관리
 */
class PomodoroRepository(private val context: Context) {

    // Room Database 연결
    private val database = PomodoroDatabase.getDatabase(context)
    private val dao = database.pomodoroDao()

    // ------------------------------------------------------------------
    // [DailyStat] 일일 통계 - Room 사용
    // ------------------------------------------------------------------
    suspend fun loadDailyStats(): Map<String, DailyStat> {
        return dao.getAllDailyStats()
            .map { it.toDomain() }
            .associateBy { it.date }
    }

    suspend fun saveDailyStats(stats: Map<String, DailyStat>) {
        dao.insertDailyStats(stats.values.map { it.toEntity() })
    }

    // ------------------------------------------------------------------
    // [WorkPreset] 작업 프리셋 - Room (Sync 지원) 사용
    // ------------------------------------------------------------------
    suspend fun loadWorkPresets(): List<WorkPreset> {
        // 삭제되지 않은(Active) 프리셋만 로드
        val presets = dao.getActiveWorkPresets().map { it.toDomain() }

        return if (presets.isEmpty()) {
            val defaultPresets = createDefaultPresets()
            saveWorkPresets(defaultPresets)
            defaultPresets
        } else {
            presets
        }
    }

    suspend fun saveWorkPresets(presets: List<WorkPreset>) {
        // 주의: insertWorkPresets는 Replace 전략을 사용하므로,
        // 기존의 isDeleted 상태를 덮어쓸 수 있습니다.
        // 전체 리스트 저장 시에는 보통 활성 상태인 것들만 저장하므로 문제없으나,
        // 개별 수정 로직이 필요할 경우 별도 함수를 분리하는 것이 좋습니다.
        dao.insertWorkPresets(presets.map { it.toEntity() })
    }

    // [추가] 프리셋 삭제 (Soft Delete)
    suspend fun deleteWorkPreset(id: String) {
        dao.softDeleteWorkPreset(id)
    }

    // ------------------------------------------------------------------
    // [DataStore] 기타 설정 데이터 (기존 유지)
    // ------------------------------------------------------------------

    suspend fun loadCurrentWorkId(): String? {
        return context.dataStore.data.first()[DSKeys.CURRENT_WORK_ID]
    }
    suspend fun saveCurrentWorkId(id: String) {
        context.dataStore.edit { it[DSKeys.CURRENT_WORK_ID] = id }
    }

    suspend fun loadBlockedApps(): Set<String> =
        context.dataStore.data.first()[DSKeys.BLOCKED_APPS] ?: emptySet()

    suspend fun saveBlockedApps(apps: Set<String>) {
        context.dataStore.edit { it[DSKeys.BLOCKED_APPS] = apps }
    }

    suspend fun loadNotificationDenialCount(): Int {
        return context.dataStore.data.first()[DSKeys.NOTIFICATION_PERMISSION_DENIAL_COUNT] ?: 0
    }
    suspend fun saveNotificationDenialCount(count: Int) {
        context.dataStore.edit { it[DSKeys.NOTIFICATION_PERMISSION_DENIAL_COUNT] = count }
    }

    private fun createDefaultPresets(): List<WorkPreset> {
        return listOf(
            WorkPreset(
                name = "기본 뽀모도로",
                settings = Settings(
                    studyTime = 25,
                    shortBreakTime = 5,
                    longBreakTime = 15,
                    longBreakInterval = 4,
                    soundEnabled = true,
                    vibrationEnabled = true,
                    autoStart = true,
                    blockMode = BlockMode.PARTIAL
                )
            ),
            WorkPreset(
                name = "집중 몰입",
                settings = Settings(
                    studyTime = 50,
                    shortBreakTime = 10,
                    longBreakTime = 30,
                    longBreakInterval = 4,
                    soundEnabled = true,
                    vibrationEnabled = true,
                    autoStart = true,
                    blockMode = BlockMode.PARTIAL
                )
            )
        )
    }

    suspend fun saveTimerState(timeLeft: Int, currentMode: Mode, totalSessions: Int) {
        context.dataStore.edit { preferences ->
            preferences[DSKeys.SAVED_TIME_LEFT] = timeLeft
            preferences[DSKeys.SAVED_CURRENT_MODE] = currentMode.name
            preferences[DSKeys.SAVED_TOTAL_SESSIONS] = totalSessions
        }
    }

    suspend fun loadTimerState(): SavedTimerState? {
        val preferences = context.dataStore.data.first()
        val timeLeft = preferences[DSKeys.SAVED_TIME_LEFT]
        val currentModeName = preferences[DSKeys.SAVED_CURRENT_MODE]
        val totalSessions = preferences[DSKeys.SAVED_TOTAL_SESSIONS]

        return if (timeLeft != null && currentModeName != null && totalSessions != null) {
            SavedTimerState(timeLeft, Mode.valueOf(currentModeName), totalSessions)
        } else {
            null
        }
    }

    suspend fun clearTimerState() {
        context.dataStore.edit { preferences ->
            preferences.remove(DSKeys.SAVED_TIME_LEFT)
            preferences.remove(DSKeys.SAVED_CURRENT_MODE)
            preferences.remove(DSKeys.SAVED_TOTAL_SESSIONS)
        }
    }

    val activeBlockModeFlow: Flow<BlockMode> = context.dataStore.data.map { preferences ->
        preferences[DSKeys.ACTIVE_BLOCK_MODE]?.let {
            runCatching { BlockMode.valueOf(it) }.getOrNull()
        } ?: BlockMode.NONE
    }

    val blockedAppsFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[DSKeys.BLOCKED_APPS] ?: emptySet()
    }

    suspend fun saveActiveBlockMode(mode: BlockMode) {
        context.dataStore.edit { it[DSKeys.ACTIVE_BLOCK_MODE] = mode.name }
    }
}