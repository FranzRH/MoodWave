package com.example.moodwave.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        RegistroEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MoodWaveDatabase : RoomDatabase() {

    abstract fun registroDao(): RegistroDao

    companion object {

        @Volatile
        private var INSTANCE: MoodWaveDatabase? = null

        fun getDatabase(
            context: Context
        ): MoodWaveDatabase {

            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MoodWaveDatabase::class.java,
                    "moodwave_database"
                ).build()

                INSTANCE = instance

                instance
            }
        }
    }
}