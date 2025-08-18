package com.malrang.pomodoro.localRepo

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.malrang.pomodoro.dataclass.ui.DailyStat
import com.malrang.pomodoro.dataclass.ui.Settings
import kotlinx.coroutines.flow.first

// DataStore.kt
val Context.dataStore by preferencesDataStore(name = "pomodoro_ds")

object DSKeys {
    val SEEN_IDS = stringSetPreferencesKey("seen_animal_ids")            // 도감: 본 동물 id 집합
    val DAILY_JSON = stringPreferencesKey("daily_stats_json")            // 일별 기록 JSON (간단히 직렬화)
    val SETTINGS_JSON = stringPreferencesKey("settings_json")            // 일별 기록 JSON (간단히 직렬화)
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
}
