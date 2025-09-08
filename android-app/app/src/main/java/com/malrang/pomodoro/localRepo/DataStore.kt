package com.malrang.pomodoro.localRepo

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.malrang.pomodoro.dataclass.sprite.AnimalSprite
import com.malrang.pomodoro.dataclass.ui.BlockMode
import com.malrang.pomodoro.dataclass.ui.DailyStat
import com.malrang.pomodoro.dataclass.ui.Mode
import com.malrang.pomodoro.dataclass.ui.Settings
import com.malrang.pomodoro.dataclass.ui.WorkPreset
import kotlinx.coroutines.flow.first

/**
 * Preferences DataStore 인스턴스를 생성하는 확장 프로퍼티입니다.
 * "pomodoro_ds"라는 이름으로 DataStore가 생성됩니다.
 */
val Context.dataStore by preferencesDataStore(name = "pomodoro_ds")

/**
 * DataStore에서 사용할 키들을 정의하는 객체입니다.
 */
object DSKeys {
    /** 사용자가 확인한 동물의 ID 목록을 저장하기 위한 키 */
    val SEEN_IDS = stringSetPreferencesKey("seen_animal_ids")
    /** 일일 통계 데이터를 JSON 형태로 저장하기 위한 키 */
    val DAILY_JSON = stringPreferencesKey("daily_stats_json")
    /** 일반 설정 데이터를 JSON 형태로 저장하기 위한 키 */
    val SETTINGS_JSON = stringPreferencesKey("settings_json")
    /** 작업 프리셋 목록을 JSON 형태로 저장하기 위한 키 */
    val WORK_PRESETS_JSON = stringPreferencesKey("work_presets_json")
    /** 현재 선택된 작업 프리셋의 ID를 저장하기 위한 키 */
    val CURRENT_WORK_ID = stringPreferencesKey("current_work_id")
    /** 현재 화면에 표시되는 활성 스프라이트(동물) 목록을 JSON 형태로 저장하기 위한 키 */
    val ACTIVE_SPRITES_JSON = stringPreferencesKey("active_sprites_json")
    /** 잔디 배경화면 사용 여부를 저장하기 위한 키 */
    val USE_GRASS_BACKGROUND = booleanPreferencesKey("use_grass_background")
    /** ✅ 화이트리스트 앱 목록을 저장하기 위한 키 */
    val WHITELISTED_APPS = stringSetPreferencesKey("whitelisted_apps")
    /** 알림 권한 거부 '횟수'를 저장하기 위한 Int 키 */
    val NOTIFICATION_PERMISSION_DENIAL_COUNT = intPreferencesKey("notification_permission_denial_count")

    //  서비스 종료 시 복원을 위한 타이머 상태 저장 키
    val SAVED_TIME_LEFT = intPreferencesKey("saved_time_left")
    val SAVED_CURRENT_MODE = stringPreferencesKey("saved_current_mode")
    val SAVED_TOTAL_SESSIONS = intPreferencesKey("saved_total_sessions")
}

//  불러온 타이머 상태를 담기 위한 데이터 클래스
data class SavedTimerState(val timeLeft: Int, val currentMode: Mode, val totalSessions: Int)


/**
 * DataStore를 사용하여 앱의 데이터를 관리하는 클래스입니다.
 * @property context 애플리케이션 컨텍스트
 */
class PomodoroRepository(private val context: Context) {
    private val gson = Gson()

    suspend fun loadSeenIds(): Set<String> =
        context.dataStore.data.first()[DSKeys.SEEN_IDS] ?: emptySet()
    suspend fun saveSeenIds(ids: Set<String>) {
        context.dataStore.edit { it[DSKeys.SEEN_IDS] = ids }
    }
    suspend fun loadDailyStats(): Map<String, DailyStat> {
        val json = context.dataStore.data.first()[DSKeys.DAILY_JSON] ?: return emptyMap()
        val type = object : TypeToken<Map<String, DailyStat>>() {}.type
        return runCatching { gson.fromJson<Map<String, DailyStat>>(json, type) }.getOrElse { emptyMap() }
    }
    suspend fun saveDailyStats(stats: Map<String, DailyStat>) {
        val json = gson.toJson(stats)
        context.dataStore.edit { it[DSKeys.DAILY_JSON] = json }
    }
    suspend fun loadWorkPresets(): List<WorkPreset> {
        val json = context.dataStore.data.first()[DSKeys.WORK_PRESETS_JSON]
        return if (json == null) {
            createDefaultPresets()
        } else {
            val type = object : TypeToken<List<WorkPreset>>() {}.type
            runCatching { gson.fromJson<List<WorkPreset>>(json, type) }.getOrElse { createDefaultPresets() }
        }
    }
    suspend fun saveWorkPresets(presets: List<WorkPreset>) {
        val json = gson.toJson(presets)
        context.dataStore.edit { it[DSKeys.WORK_PRESETS_JSON] = json }
    }
    suspend fun loadCurrentWorkId(): String? {
        return context.dataStore.data.first()[DSKeys.CURRENT_WORK_ID]
    }
    suspend fun saveCurrentWorkId(id: String) {
        context.dataStore.edit { it[DSKeys.CURRENT_WORK_ID] = id }
    }
    suspend fun saveActiveSprites(sprites: List<AnimalSprite>) {
        val json = gson.toJson(sprites)
        context.dataStore.edit { it[DSKeys.ACTIVE_SPRITES_JSON] = json }
    }
    suspend fun loadActiveSprites(): List<AnimalSprite> {
        val json = context.dataStore.data.first()[DSKeys.ACTIVE_SPRITES_JSON] ?: return emptyList()
        val type = object : TypeToken<List<AnimalSprite>>() {}.type
        return runCatching { gson.fromJson<List<AnimalSprite>>(json, type) }.getOrElse { emptyList() }
    }
    suspend fun loadUseGrassBackground(): Boolean {
        return context.dataStore.data.first()[DSKeys.USE_GRASS_BACKGROUND] ?: true
    }
    suspend fun saveUseGrassBackground(useGrass: Boolean) {
        context.dataStore.edit { it[DSKeys.USE_GRASS_BACKGROUND] = useGrass }
    }
    suspend fun loadWhitelistedApps(): Set<String> =
        context.dataStore.data.first()[DSKeys.WHITELISTED_APPS] ?: emptySet()
    suspend fun saveWhitelistedApps(apps: Set<String>) {
        context.dataStore.edit { it[DSKeys.WHITELISTED_APPS] = apps }
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
                    autoStart = true, // 긴 세션에서는 수동 시작을 선호할 수 있습니다.
                    blockMode = BlockMode.PARTIAL
                )
            )
        )
    }

    /**
     *  일시정지 시 타이머 상태를 DataStore에 저장합니다.
     */
    suspend fun saveTimerState(timeLeft: Int, currentMode: Mode, totalSessions: Int) {
        context.dataStore.edit { preferences ->
            preferences[DSKeys.SAVED_TIME_LEFT] = timeLeft
            preferences[DSKeys.SAVED_CURRENT_MODE] = currentMode.name
            preferences[DSKeys.SAVED_TOTAL_SESSIONS] = totalSessions
        }
    }

    /**
     *  저장된 타이머 상태를 DataStore에서 불러옵니다.
     * @return 저장된 상태가 있으면 SavedTimerState 객체를, 없으면 null을 반환합니다.
     */
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

    /**
     *  저장된 타이머 상태를 DataStore에서 삭제합니다. (리셋 시 호출)
     */
    suspend fun clearTimerState() {
        context.dataStore.edit { preferences ->
            preferences.remove(DSKeys.SAVED_TIME_LEFT)
            preferences.remove(DSKeys.SAVED_CURRENT_MODE)
            preferences.remove(DSKeys.SAVED_TOTAL_SESSIONS)
        }
    }
}