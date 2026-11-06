package com.example.moodwave.analytics

import com.example.moodwave.data.local.RegistroEntity

object RegistroAnalyticsMapper {

    fun convertir(
        registro: RegistroEntity
    ): MoodRecord {

        val actividades =
            registro.actividades
                .split(" · ")
                .map {
                    it.trim()
                }
                .filter {
                    it.isNotBlank()
                }

        return MoodRecord(
            fecha = registro.fecha,
            estado = registro.estadoValor,
            energia = registro.energia,
            actividades = actividades
        )
    }

    fun convertirLista(
        registros: List<RegistroEntity>
    ): List<MoodRecord> {

        return registros.map {
            convertir(it)
        }
    }
}