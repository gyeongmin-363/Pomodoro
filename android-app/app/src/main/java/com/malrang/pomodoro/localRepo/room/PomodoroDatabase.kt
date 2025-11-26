package com.malrang.pomodoro.localRepo.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [DailyStatEntity::class, WorkPresetEntity::class],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class PomodoroDatabase : RoomDatabase() {
    abstract fun pomodoroDao(): PomodoroDao

    companion object {
        @Volatile
        private var INSTANCE: PomodoroDatabase? = null

        fun getDatabase(context: Context): PomodoroDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PomodoroDatabase::class.java,
                    "pomodoro_database"
                )
                    .fallbackToDestructiveMigration(true) // [추가] 마이그레이션 전략 (개발 중 데이터 초기화 허용 시)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}