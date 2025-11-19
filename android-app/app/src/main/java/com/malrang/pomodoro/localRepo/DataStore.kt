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

    // [추가] 커스텀 배경 관련 키
    val CUSTOM_BG_COLOR = intPreferencesKey("custom_bg_color")
    val CUSTOM_TEXT_COLOR = intPreferencesKey("custom_text_color")
    val BACKGROUND_TYPE = stringPreferencesKey("background_type") // "COLOR" or "IMAGE"
    val SELECTED_BG_IMAGE_PATH = stringPreferencesKey("selected_bg_image_path")
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
            insertNewWorkPresets(defaultPresets)
            defaultPresets
        }
    }

    suspend fun insertNewWorkPresets(presets: List<WorkPreset>) {
        dao.insertWorkPresets(presets.map { it.toEntity() })
    }

    suspend fun updateWorkPresets(presets: List<WorkPreset>) {
        presets.forEach { preset ->
            dao.updateWorkPreset(preset.toEntity())
        }
    }

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

    // [추가] 커스텀 배경 관련 메서드
    suspend fun loadCustomBgColor(): Int? = context.dataStore.data.first()[DSKeys.CUSTOM_BG_COLOR]
    suspend fun loadCustomTextColor(): Int? = context.dataStore.data.first()[DSKeys.CUSTOM_TEXT_COLOR]
    suspend fun loadBackgroundType(): String = context.dataStore.data.first()[DSKeys.BACKGROUND_TYPE] ?: "COLOR"
    suspend fun loadSelectedBgImagePath(): String? = context.dataStore.data.first()[DSKeys.SELECTED_BG_IMAGE_PATH]

    suspend fun saveCustomColors(bgColor: Int, textColor: Int) {
        context.dataStore.edit {
            it[DSKeys.CUSTOM_BG_COLOR] = bgColor
            it[DSKeys.CUSTOM_TEXT_COLOR] = textColor
        }
    }

    suspend fun saveBackgroundType(type: String) {
        context.dataStore.edit { it[DSKeys.BACKGROUND_TYPE] = type }
    }

    suspend fun saveSelectedBgImagePath(path: String) {
        context.dataStore.edit { it[DSKeys.SELECTED_BG_IMAGE_PATH] = path }
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