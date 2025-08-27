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
import com.malrang.pomodoro.dataclass.ui.DailyStat
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

    /** 🔽 [수정] 알림 권한 거부 '횟수'를 저장하기 위한 Int 키 */
    val NOTIFICATION_PERMISSION_DENIAL_COUNT = intPreferencesKey("notification_permission_denial_count")
}

/**
 * DataStore를 사용하여 앱의 데이터를 관리하는 클래스입니다.
 * @property context 애플리케이션 컨텍스트
 */
class PomodoroRepository(private val context: Context) {
    private val gson = Gson()

    // ... (loadSeenIds, saveSeenIds, loadDailyStats, saveDailyStats 등 기존 함수는 동일) ...
    suspend fun loadSeenIds(): Set<String> =
        context.dataStore.data.first()[DSKeys.SEEN_IDS] ?: emptySet()

    /**
     * 확인한 동물의 ID 목록을 DataStore에 저장합니다.
     * @param ids 저장할 동물의 ID 집합(Set)
     */
    suspend fun saveSeenIds(ids: Set<String>) {
        context.dataStore.edit { it[DSKeys.SEEN_IDS] = ids }
    }

    /**
     * DataStore에서 일일 통계 데이터를 불러옵니다.
     * @return 날짜(String)를 키로, DailyStat을 값으로 갖는 맵(Map)을 반환합니다. 저장된 데이터가 없으면 빈 맵을 반환합니다.
     */
    suspend fun loadDailyStats(): Map<String, DailyStat> {
        val json = context.dataStore.data.first()[DSKeys.DAILY_JSON] ?: return emptyMap()
        val type = object : TypeToken<Map<String, DailyStat>>() {}.type
        return runCatching { gson.fromJson<Map<String, DailyStat>>(json, type) }.getOrElse { emptyMap() }
    }

    /**
     * 일일 통계 데이터를 DataStore에 저장합니다.
     * @param stats 날짜(String)를 키로, DailyStat을 값으로 갖는 맵(Map) 데이터
     */
    suspend fun saveDailyStats(stats: Map<String, DailyStat>) {
        val json = gson.toJson(stats)
        context.dataStore.edit { it[DSKeys.DAILY_JSON] = json }
    }


    /**
     * DataStore에서 작업 프리셋 목록을 불러옵니다.
     * @return 저장된 프리셋이 없으면 기본 프리셋 목록을 생성하여 반환합니다.
     */
    suspend fun loadWorkPresets(): List<WorkPreset> {
        val json = context.dataStore.data.first()[DSKeys.WORK_PRESETS_JSON]
        return if (json == null) {
            createDefaultPresets()
        } else {
            val type = object : TypeToken<List<WorkPreset>>() {}.type
            runCatching { gson.fromJson<List<WorkPreset>>(json, type) }.getOrElse { createDefaultPresets() }
        }
    }

    /**
     * 작업 프리셋 목록을 DataStore에 저장합니다.
     * @param presets 저장할 WorkPreset 객체의 리스트
     */
    suspend fun saveWorkPresets(presets: List<WorkPreset>) {
        val json = gson.toJson(presets)
        context.dataStore.edit { it[DSKeys.WORK_PRESETS_JSON] = json }
    }

    /**
     * DataStore에서 현재 선택된 작업 프리셋의 ID를 불러옵니다.
     * @return 저장된 ID가 없으면 null을 반환합니다.
     */
    suspend fun loadCurrentWorkId(): String? {
        return context.dataStore.data.first()[DSKeys.CURRENT_WORK_ID]
    }

    /**
     * 현재 선택된 작업 프리셋의 ID를 DataStore에 저장합니다.
     * @param id 저장할 작업 프리셋의 ID
     */
    suspend fun saveCurrentWorkId(id: String) {
        context.dataStore.edit { it[DSKeys.CURRENT_WORK_ID] = id }
    }

    /**
     * 현재 활성화된 AnimalSprite 목록을 JSON 형태로 저장합니다.
     * @param sprites 저장할 AnimalSprite 객체의 리스트
     */
    suspend fun saveActiveSprites(sprites: List<AnimalSprite>) {
        val json = gson.toJson(sprites)
        context.dataStore.edit { it[DSKeys.ACTIVE_SPRITES_JSON] = json }
    }

    /**
     * 저장된 AnimalSprite 목록을 불러옵니다.
     * @return 저장된 데이터가 없으면 빈 리스트를 반환합니다.
     */
    suspend fun loadActiveSprites(): List<AnimalSprite> {
        val json = context.dataStore.data.first()[DSKeys.ACTIVE_SPRITES_JSON] ?: return emptyList()
        val type = object : TypeToken<List<AnimalSprite>>() {}.type
        return runCatching { gson.fromJson<List<AnimalSprite>>(json, type) }.getOrElse { emptyList() }
    }

    /**
     * DataStore에서 잔디 배경 사용 여부를 불러옵니다.
     * @return 저장된 설정이 없으면 true (기본값)를 반환합니다.
     */
    suspend fun loadUseGrassBackground(): Boolean {
        // 기본값을 true로 설정하여 처음에는 잔디 배경을 보여줍니다.
        return context.dataStore.data.first()[DSKeys.USE_GRASS_BACKGROUND] ?: true
    }

    /**
     * 잔디 배경 사용 여부를 DataStore에 저장합니다.
     * @param useGrass 잔디 배경을 사용할지 여부
     */
    suspend fun saveUseGrassBackground(useGrass: Boolean) {
        context.dataStore.edit { it[DSKeys.USE_GRASS_BACKGROUND] = useGrass }
    }

    /**
     * DataStore에서 화이트리스트 앱 목록을 불러옵니다.
     * @return 저장된 앱 패키지 이름의 집합(Set)을 반환합니다.
     */
    suspend fun loadWhitelistedApps(): Set<String> =
        context.dataStore.data.first()[DSKeys.WHITELISTED_APPS] ?: emptySet()

    /**
     * 화이트리스트 앱 목록을 DataStore에 저장합니다.
     * @param apps 저장할 앱 패키지 이름의 집합(Set)
     */
    suspend fun saveWhitelistedApps(apps: Set<String>) {
        context.dataStore.edit { it[DSKeys.WHITELISTED_APPS] = apps }
    }

    /**
     * 🔽 [추가] DataStore에서 알림 권한 거부 횟수를 불러옵니다.
     * @return 저장된 값이 없으면 0을 반환합니다.
     */
    suspend fun loadNotificationDenialCount(): Int {
        return context.dataStore.data.first()[DSKeys.NOTIFICATION_PERMISSION_DENIAL_COUNT] ?: 0
    }

    /**
     * 🔽 [추가] 알림 권한 거부 횟수를 DataStore에 저장합니다.
     * @param count 저장할 거부 횟수
     */
    suspend fun saveNotificationDenialCount(count: Int) {
        context.dataStore.edit { it[DSKeys.NOTIFICATION_PERMISSION_DENIAL_COUNT] = count }
    }

    private fun createDefaultPresets(): List<WorkPreset> {
        return listOf(
            WorkPreset(
                name = "영어 공부",
                settings = Settings(
                    studyTime = 20,
                    shortBreakTime = 5,
                    longBreakTime = 5,
                    longBreakInterval = 3,
                    soundEnabled = false,
                    vibrationEnabled = false,
                    autoStart = false
                )
            ),
            WorkPreset(
                name = "코딩 공부",
                settings = Settings(
                    studyTime = 30,
                    shortBreakTime = 2,
                    longBreakTime = 3,
                    longBreakInterval = 2,
                    soundEnabled = true,
                    vibrationEnabled = true,
                    autoStart = true
                )
            )
        )
    }
}