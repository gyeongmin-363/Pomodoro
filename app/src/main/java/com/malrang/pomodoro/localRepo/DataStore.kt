package com.malrang.pomodoro.localRepo

import android.content.Context
import androidx.datastore.preferences.core.edit
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

val Context.dataStore by preferencesDataStore(name = "pomodoro_ds")

object DSKeys {
    val SEEN_IDS = stringSetPreferencesKey("seen_animal_ids")
    val DAILY_JSON = stringPreferencesKey("daily_stats_json")
    val SETTINGS_JSON = stringPreferencesKey("settings_json")
    val WORK_PRESETS_JSON = stringPreferencesKey("work_presets_json")
    val CURRENT_WORK_ID = stringPreferencesKey("current_work_id")
    // --- ▼▼▼ 추가된 부분 ▼▼▼ ---
    val ACTIVE_SPRITES_JSON = stringPreferencesKey("active_sprites_json") // 활성 스프라이트 목록 저장
    // --- ▲▲▲ 추가된 부분 ▲▲▲ ---
}

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

    suspend fun loadSettings() : Settings {
        val json = context.dataStore.data.first()[DSKeys.SETTINGS_JSON] ?: return Settings()
        val type = object : TypeToken<Settings>() {}.type
        return runCatching { gson.fromJson<Settings>(json, type) }.getOrElse { Settings() }

    }

    suspend fun saveSettings(settings: Settings) {
        val json = gson.toJson(settings)
        context.dataStore.edit { it[DSKeys.SETTINGS_JSON] = json }
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

    // --- ▼▼▼ 추가된 함수들 ▼▼▼ ---

    /**
     * 현재 활성화된 AnimalSprite 목록을 JSON 형태로 저장합니다.
     */
    suspend fun saveActiveSprites(sprites: List<AnimalSprite>) {
        val json = gson.toJson(sprites)
        context.dataStore.edit { it[DSKeys.ACTIVE_SPRITES_JSON] = json }
    }

    /**
     * 저장된 AnimalSprite 목록을 불러옵니다. 저장된 데이터가 없으면 빈 리스트를 반환합니다.
     */
    suspend fun loadActiveSprites(): List<AnimalSprite> {
        val json = context.dataStore.data.first()[DSKeys.ACTIVE_SPRITES_JSON] ?: return emptyList()
        val type = object : TypeToken<List<AnimalSprite>>() {}.type
        return runCatching { gson.fromJson<List<AnimalSprite>>(json, type) }.getOrElse { emptyList() }
    }

    // --- ▲▲▲ 추가된 함수들 ▲▲▲ ---

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