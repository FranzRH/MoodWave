package com.example.moodwave.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.reminderDataStore by preferencesDataStore(
    name = "reminder_preferences"
)

class ReminderPreferences(
    private val context: Context
) {

    companion object {
        private val RECORDATORIO_ACTIVADO =
            booleanPreferencesKey("recordatorio_activado")

        private val HORA_RECORDATORIO =
            intPreferencesKey("hora_recordatorio")
    }

    val recordatorioActivado: Flow<Boolean> =
        context.reminderDataStore.data.map { preferences ->
            preferences[RECORDATORIO_ACTIVADO] ?: false
        }

    val horaRecordatorio: Flow<Int> =
        context.reminderDataStore.data.map { preferences ->
            preferences[HORA_RECORDATORIO] ?: 20
        }

    suspend fun cambiarRecordatorioActivado(
        activado: Boolean
    ) {
        context.reminderDataStore.edit { preferences ->
            preferences[RECORDATORIO_ACTIVADO] = activado
        }
    }

    suspend fun cambiarHoraRecordatorio(
        hora: Int
    ) {
        context.reminderDataStore.edit { preferences ->
            preferences[HORA_RECORDATORIO] = hora.coerceIn(0, 23)
        }
    }
}