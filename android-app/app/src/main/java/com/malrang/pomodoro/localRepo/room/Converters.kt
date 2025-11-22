package com.malrang.pomodoro.localRepo.room

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.malrang.pomodoro.dataclass.ui.Settings

class Converters {
    private val gson = Gson()

    // Map<String, Int> 변환
    @TypeConverter
    fun fromStringIntMap(value: String): Map<String, Int> {
        val type = object : TypeToken<Map<String, Int>>() {}.type
        return runCatching { gson.fromJson<Map<String, Int>>(value, type) }.getOrElse { emptyMap() }
    }

    @TypeConverter
    fun fromMapStringInt(map: Map<String, Int>): String {
        return gson.toJson(map)
    }

    // [추가] Map<String, Boolean> 변환 (체크리스트용)
    @TypeConverter
    fun fromStringBooleanMap(value: String): Map<String, Boolean> {
        val type = object : TypeToken<Map<String, Boolean>>() {}.type
        return runCatching { gson.fromJson<Map<String, Boolean>>(value, type) }.getOrElse { emptyMap() }
    }

    @TypeConverter
    fun fromMapStringBoolean(map: Map<String, Boolean>): String {
        return gson.toJson(map)
    }

    // Settings 객체 변환
    @TypeConverter
    fun fromSettingsJson(value: String): Settings {
        return runCatching { gson.fromJson(value, Settings::class.java) }
            .getOrElse { Settings() } // 실패 시 기본값
    }

    @TypeConverter
    fun toSettingsJson(settings: Settings): String {
        return gson.toJson(settings)
    }
}