package com.example.moodwave.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Locale

private val Context.activityDataStore by preferencesDataStore(
    name = "moodwave_activities"
)

class ActivityPreferences(
    private val context: Context
) {

    companion object {
        private val ACTIVIDADES_PERSONALIZADAS =
            stringSetPreferencesKey("actividades_personalizadas")
    }

    val actividadesPersonalizadas: Flow<Set<String>> =
        context.activityDataStore.data.map { preferences ->
            preferences[ACTIVIDADES_PERSONALIZADAS] ?: emptySet()
        }

    suspend fun agregarActividad(
        nombre: String
    ): Boolean {

        val actividad = nombre.trim()

        if (actividad.isBlank()) {
            return false
        }

        val preferences =
            context.activityDataStore.data.first()

        val actuales =
            preferences[ACTIVIDADES_PERSONALIZADAS] ?: emptySet()

        val yaExiste =
            actuales.any {
                it.equals(
                    actividad,
                    ignoreCase = true
                )
            }

        if (yaExiste) {
            return false
        }

        context.activityDataStore.edit { prefs ->

            val nuevas =
                (prefs[ACTIVIDADES_PERSONALIZADAS]
                    ?: emptySet()) + actividad

            prefs[ACTIVIDADES_PERSONALIZADAS] =
                nuevas
        }

        return true
    }

    suspend fun reemplazarActividades(
        actividades: Set<String>
    ) {

        val normalizadas =
            linkedMapOf<String, String>()

        actividades.forEach { nombre ->

            val actividad = nombre.trim()

            if (actividad.isNotBlank()) {

                normalizadas[
                    actividad.lowercase(
                        Locale.getDefault()
                    )
                ] = actividad
            }
        }

        context.activityDataStore.edit { prefs ->
            prefs[ACTIVIDADES_PERSONALIZADAS] =
                normalizadas.values.toSet()
        }
    }
}
