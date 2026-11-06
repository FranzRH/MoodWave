package com.example.moodwave.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.diaryDataStore by preferencesDataStore(
    name = "moodwave_diary"
)

class DiaryPreferences(
    private val context: Context
) {

    companion object {

        private val MAX_REFLECTION_CHARS =
            intPreferencesKey("max_reflection_chars")
    }

    val maxReflectionChars: Flow<Int> =
        context.diaryDataStore.data.map { preferences ->

            preferences[MAX_REFLECTION_CHARS] ?: 300
        }

    suspend fun cambiarLimiteReflexion(
        limite: Int
    ) {

        context.diaryDataStore.edit { preferences ->

            preferences[MAX_REFLECTION_CHARS] =
                limite
        }
    }
}