
package com.example.moodwave.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MoodAnalyticsTest {

    @Test
    fun calcularEstadisticas_manual() {

        val registros = listOf(
            MoodRecord(
                fecha = "2026-01-01",
                estado = 2,
                energia = 5,
                actividades = emptyList()
            ),
            MoodRecord(
                fecha = "2026-01-02",
                estado = 4,
                energia = 6,
                actividades = emptyList()
            ),
            MoodRecord(
                fecha = "2026-01-03",
                estado = 4,
                energia = 6,
                actividades = emptyList()
            ),
            MoodRecord(
                fecha = "2026-01-04",
                estado = 6,
                energia = 7,
                actividades = emptyList()
            ),
            MoodRecord(
                fecha = "2026-01-05",
                estado = 8,
                energia = 8,
                actividades = emptyList()
            )
        )

        val resultado =
            MoodAnalytics.calcularEstadisticas(registros)

        assertEquals(4.8, resultado.promedio, 0.001)
        assertEquals(4.0, resultado.mediana, 0.001)
        assertEquals(4, resultado.moda)
        assertEquals(2.0396, resultado.desviacion, 0.001)
        assertEquals(5, resultado.totalRegistros)
        assertEquals(5, resultado.rachaActual)
    }

    @Test
    fun calcularRacha_interrumpida() {

        val registros = listOf(
            MoodRecord("2026-01-01", 5, 5, emptyList()),
            MoodRecord("2026-01-03", 6, 6, emptyList()),
            MoodRecord("2026-01-04", 6, 6, emptyList()),
            MoodRecord("2026-01-05", 7, 7, emptyList())
        )

        val resultado =
            MoodAnalytics.calcularEstadisticas(registros)

        assertEquals(3, resultado.rachaActual)
    }

    @Test
    fun calcularEstadisticasSemana_correctamente() {

        val registros = listOf(
            MoodRecord("2026-01-04", 2, 4, emptyList()),
            MoodRecord("2026-01-05", 4, 5, emptyList()),
            MoodRecord("2026-01-06", 6, 6, emptyList()),
            MoodRecord("2026-01-07", 8, 8, emptyList()),
            MoodRecord("2026-01-12", 1, 3, emptyList())
        )

        val resultado =
            MoodAnalytics.calcularEstadisticasSemana(
                registros,
                "2026-01-07"
            )

        assertEquals(3, resultado.totalRegistros)
        assertEquals(6.0, resultado.promedio, 0.001)
        assertEquals(6.0, resultado.mediana, 0.001)
        assertEquals(3, resultado.rachaActual)
    }

    @Test
    fun calcularEstadisticasMes_correctamente() {

        val registros = listOf(
            MoodRecord("2025-12-31", 2, 4, emptyList()),
            MoodRecord("2026-01-05", 4, 5, emptyList()),
            MoodRecord("2026-01-10", 6, 6, emptyList()),
            MoodRecord("2026-01-20", 8, 8, emptyList()),
            MoodRecord("2026-02-01", 1, 3, emptyList())
        )

        val resultado =
            MoodAnalytics.calcularEstadisticasMes(
                registros,
                "2026-01-15"
            )

        assertEquals(3, resultado.totalRegistros)
        assertEquals(6.0, resultado.promedio, 0.001)
        assertEquals(6.0, resultado.mediana, 0.001)
    }

    @Test
    fun calcularActividadVsAnimo_correctamente() {

        val registros = listOf(
            MoodRecord(
                "2026-01-01", 4, 5,
                listOf("Trabajo")
            ),
            MoodRecord(
                "2026-01-02", 6, 7,
                listOf("Ejercicio")
            ),
            MoodRecord(
                "2026-01-03", 8, 9,
                listOf("Ejercicio", "Amigos")
            ),
            MoodRecord(
                "2026-01-04", 2, 4,
                listOf("Trabajo")
            )
        )

        val resultado =
            MoodAnalytics.calcularActividadVsAnimo(registros)

        val ejercicio =
            resultado.first { it.actividad == "Ejercicio" }

        val trabajo =
            resultado.first { it.actividad == "Trabajo" }

        assertEquals(7.0, ejercicio.estadoPromedio, 0.001)
        assertEquals(8.0, ejercicio.energiaPromedio, 0.001)
        assertEquals(2.0, ejercicio.diferenciaMediaGeneral, 0.001)
        assertEquals(2, ejercicio.totalRegistros)

        assertEquals(3.0, trabajo.estadoPromedio, 0.001)
        assertEquals(4.5, trabajo.energiaPromedio, 0.001)
        assertEquals(-2.0, trabajo.diferenciaMediaGeneral, 0.001)
        assertEquals(2, trabajo.totalRegistros)
    }

    @Test
    fun actividadDatosInsuficientes() {

        val registros =
            (1..10).map { dia ->
                MoodRecord(
                    fecha = String.format("2026-01-%02d", dia),
                    estado = 6,
                    energia = 7,
                    actividades =
                        if (dia <= 3) listOf("Ejercicio")
                        else emptyList()
                )
            }

        val resultado =
            MoodAnalytics.calcularActividadVsAnimo(registros)

        val ejercicio =
            resultado.first { it.actividad == "Ejercicio" }

        assertEquals(3, ejercicio.totalRegistros)
        assertFalse(ejercicio.datosSuficientes)
    }

    @Test
    fun actividadDatosSuficientes() {

        val registros =
            (1..14).map { dia ->
                MoodRecord(
                    fecha = String.format("2026-01-%02d", dia),
                    estado = if (dia <= 5) 7 else 5,
                    energia = if (dia <= 5) 8 else 6,
                    actividades =
                        if (dia <= 5) listOf("Ejercicio")
                        else emptyList()
                )
            }

        val resultado =
            MoodAnalytics.calcularActividadVsAnimo(registros)

        val ejercicio =
            resultado.first { it.actividad == "Ejercicio" }

        assertEquals(5, ejercicio.totalRegistros)
        assertTrue(ejercicio.datosSuficientes)
    }

    @Test
    fun calcularTamanoEfecto_grandePositivo() {

        val registros =
            (1..14).map { dia ->

                val tieneEjercicio = dia <= 7

                val estado =
                    if (tieneEjercicio) {
                        if (dia % 2 == 0) 8 else 7
                    } else {
                        if (dia % 2 == 0) 5 else 4
                    }

                MoodRecord(
                    fecha = String.format("2026-01-%02d", dia),
                    estado = estado,
                    energia = 7,
                    actividades =
                        if (tieneEjercicio) listOf("Ejercicio")
                        else emptyList()
                )
            }

        val resultado =
            MoodAnalytics.calcularActividadVsAnimo(registros)

        val ejercicio =
            resultado.first { it.actividad == "Ejercicio" }

        assertTrue(ejercicio.tamanoEfecto > 0.80)
        assertEquals("Grande", ejercicio.nivelEfecto)
        assertTrue(ejercicio.datosSuficientes)
    }

    @Test
    fun calcularTamanoEfecto_negativo() {

        val registros =
            (1..14).map { dia ->

                val tieneTrabajo = dia <= 7

                val estado =
                    if (tieneTrabajo) {
                        if (dia % 2 == 0) 4 else 3
                    } else {
                        if (dia % 2 == 0) 7 else 6
                    }

                MoodRecord(
                    fecha = String.format("2026-02-%02d", dia),
                    estado = estado,
                    energia = 6,
                    actividades =
                        if (tieneTrabajo) listOf("Trabajo")
                        else emptyList()
                )
            }

        val resultado =
            MoodAnalytics.calcularActividadVsAnimo(registros)

        val trabajo =
            resultado.first { it.actividad == "Trabajo" }

        assertTrue(trabajo.tamanoEfecto < -0.80)
        assertEquals("Grande", trabajo.nivelEfecto)
    }

    @Test
    fun correlacionActividad_positiva() {

        val registros =
            (1..20).map { dia ->

                val ejercicio = dia <= 10

                MoodRecord(
                    fecha = String.format("2026-03-%02d", dia),
                    estado =
                        if (ejercicio) {
                            if (dia % 2 == 0) 8 else 7
                        } else {
                            if (dia % 2 == 0) 4 else 5
                        },
                    energia = 7,
                    actividades =
                        if (ejercicio) listOf("Ejercicio")
                        else emptyList()
                )
            }

        val resultado =
            MoodAnalytics.calcularCorrelacionesActividad(registros)

        val ejercicio =
            resultado.first { it.actividad == "Ejercicio" }

        assertTrue(ejercicio.coeficiente > 0.50)
        assertEquals("Positiva", ejercicio.direccion)
        assertEquals("Fuerte", ejercicio.nivelRelacion)
        assertTrue(ejercicio.datosSuficientes)
        assertEquals(10, ejercicio.totalConActividad)
        assertEquals(10, ejercicio.totalSinActividad)
    }

    @Test
    fun correlacionActividad_negativa() {

        val registros =
            (1..20).map { dia ->

                val trabajo = dia <= 10

                MoodRecord(
                    fecha = String.format("2026-04-%02d", dia),
                    estado =
                        if (trabajo) {
                            if (dia % 2 == 0) 3 else 4
                        } else {
                            if (dia % 2 == 0) 7 else 8
                        },
                    energia = 6,
                    actividades =
                        if (trabajo) listOf("Trabajo")
                        else emptyList()
                )
            }

        val resultado =
            MoodAnalytics.calcularCorrelacionesActividad(registros)

        val trabajo =
            resultado.first { it.actividad == "Trabajo" }

        assertTrue(trabajo.coeficiente < -0.50)
        assertEquals("Negativa", trabajo.direccion)
        assertEquals("Fuerte", trabajo.nivelRelacion)
        assertTrue(trabajo.datosSuficientes)
    }

    @Test
    fun correlacionActividad_datosInsuficientes() {

        val registros =
            (1..14).map { dia ->
                MoodRecord(
                    fecha = String.format("2026-05-%02d", dia),
                    estado = 6,
                    energia = 7,
                    actividades =
                        if (dia <= 3) listOf("Viaje")
                        else emptyList()
                )
            }

        val resultado =
            MoodAnalytics.calcularCorrelacionesActividad(registros)

        val viaje =
            resultado.first { it.actividad == "Viaje" }

        assertEquals(3, viaje.totalConActividad)
        assertEquals(11, viaje.totalSinActividad)
        assertFalse(viaje.datosSuficientes)
    }

    @Test
    fun generar90Dias_correctamente() {

        val registros =
            SyntheticDataGenerator.generar90Dias()

        assertEquals(90, registros.size)

        assertTrue(
            registros.all { it.estado in 1..8 }
        )

        assertTrue(
            registros.all { it.energia in 1..10 }
        )
    }

    @Test
    fun calcularTendencia7Dias_correctamente() {

        val registros =
            generarRegistros("2026-06-01", 8)

        val resultado =
            MoodAnalytics.calcularTendencia7Dias(
                registros,
                "2026-06-08"
            )

        assertEquals(7, resultado.size)

        assertEquals("2026-06-02", resultado.first().fecha)
        assertEquals("2026-06-08", resultado.last().fecha)

        assertEquals(1.5, resultado.first().mediaMovilEstado, 0.001)
        assertEquals(5.0, resultado.last().mediaMovilEstado, 0.001)
    }

    @Test
    fun calcularTendencia30Dias_correctamente() {

        val registros =
            generarRegistros("2026-05-27", 35)

        val resultado =
            MoodAnalytics.calcularTendencia30Dias(
                registros,
                "2026-06-30"
            )

        assertEquals(30, resultado.size)

        assertEquals("2026-06-01", resultado.first().fecha)
        assertEquals("2026-06-30", resultado.last().fecha)

        assertEquals(3.5, resultado.first().mediaMovilEstado, 0.001)
    }

    @Test
    fun calcularTendencia90Dias_correctamente() {

        val registros =
            generarRegistros("2026-03-23", 100)

        val resultado =
            MoodAnalytics.calcularTendencia90Dias(
                registros,
                "2026-06-30"
            )

        assertEquals(90, resultado.size)

        assertEquals("2026-04-02", resultado.first().fecha)
        assertEquals("2026-06-30", resultado.last().fecha)

        assertEquals(
            32.0 / 7.0,
            resultado.first().mediaMovilEstado,
            0.001
        )
    }

    private fun generarRegistros(
        fechaInicio: String,
        cantidad: Int
    ): List<MoodRecord> {

        val formato =
            SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.US
            ).apply {
                isLenient = false
            }

        val calendario =
            Calendar.getInstance().apply {
                time = formato.parse(fechaInicio)!!
            }

        return (0 until cantidad).map { indice ->

            val registro =
                MoodRecord(
                    fecha = formato.format(calendario.time),
                    estado = (indice % 8) + 1,
                    energia = 6,
                    actividades = emptyList()
                )

            calendario.add(
                Calendar.DAY_OF_MONTH,
                1
            )

            registro
        }
    }
}
