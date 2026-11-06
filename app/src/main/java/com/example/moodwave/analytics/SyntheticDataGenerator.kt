package com.example.moodwave.analytics

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.random.Random

object SyntheticDataGenerator {

    fun generar90Dias(): List<MoodRecord> {
        val registros = mutableListOf<MoodRecord>()
        val random = Random(2027)

        val calendario = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, -89)
        }

        val formatoFecha = SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        )

        repeat(90) { indice ->
            val actividades: List<String>
            val estado: Int
            val energia: Int

            when (indice % 3) {
                0 -> {
                    estado = 2 + random.nextInt(0, 2)
                    energia = 2 + random.nextInt(0, 2)
                    actividades = listOf("Trabajo", "Estudio")
                }

                1 -> {
                    estado = 5 + random.nextInt(0, 2)
                    energia = 4 + random.nextInt(0, 2)
                    actividades = listOf("Estudio", "Familia")
                }

                else -> {
                    estado = 7 + random.nextInt(0, 2)
                    energia = 8 + random.nextInt(0, 2)
                    actividades = listOf("Ejercicio", "Amigos", "Descanso")
                }
            }

            registros.add(
                MoodRecord(
                    fecha = formatoFecha.format(calendario.time),
                    estado = estado,
                    energia = energia,
                    actividades = actividades
                )
            )

            calendario.add(Calendar.DAY_OF_MONTH, 1)
        }

        return registros
    }
}
