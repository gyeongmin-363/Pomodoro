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

class PomodoroRepository(private val context: Context) {

    private val database = PomodoroDatabase.getDatabase(context)
    private val dao = database.pomodoroDao()

    // --- DailyStat ---
    suspend fun loadDailyStats(): Map<String, DailyStat> {
        return dao.getAllDailyStats()
            .map { it.toDomain() }
            .associateBy { it.date }
    }

    suspend fun saveDailyStats(stats: Map<String, DailyStat>) {
        dao.insertDailyStats(stats.values.map { it.toEntity() })
    }

    // --- WorkPreset ---
    suspend fun loadWorkPresets(): List<WorkPreset> {
        val presets = dao.getActiveWorkPresets().map { it.toDomain() }
        return presets.ifEmpty {
            val defaultPresets = createDefaultPresets()
            // [수정] 초기 생성 시에는 insertNewWorkPresets 사용
            insertNewWorkPresets(defaultPresets)
            defaultPresets
        }
    }

    // [변경] 새 프리셋 삽입 (이미 존재하면 무시하여 기존 데이터/상태 보존)
    suspend fun insertNewWorkPresets(presets: List<WorkPreset>) {
        dao.insertWorkPresets(presets.map { it.toEntity() })
    }

    // [변경] 기존 프리셋 업데이트 (isDeleted 상태 유지 및 값 갱신)
    // UI에서 수정된 프리셋을 저장할 때 주로 사용됩니다.
    suspend fun updateWorkPresets(presets: List<WorkPreset>) {
        presets.forEach { preset ->
            dao.updateWorkPreset(preset.toEntity())
        }
    }

    // (하위 호환성 유지용) 단순히 저장할 때는 업데이트로 처리하되,
    // 필요하다면 insertNewWorkPresets와 병행하여 Upsert 로직을 구현할 수도 있습니다.
    // 여기서는 요청하신 대로 분리된 메서드를 제공합니다.

    suspend fun deleteWorkPreset(id: String) {
        dao.softDeleteWorkPreset(id)
    }

    // --- DataStore ---

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