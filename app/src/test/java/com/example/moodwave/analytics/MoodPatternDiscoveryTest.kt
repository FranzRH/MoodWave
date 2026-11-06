package com.example.moodwave.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MoodPatternDiscoveryTest {

    @Test
    fun requiereAlMenos14Dias() {
        val registros = (1..13).map { dia ->
            MoodRecord(
                fecha = "2026-01-${dia.toString().padStart(2, '0')}",
                estado = 6,
                energia = 7,
                actividades = listOf("Estudio")
            )
        }

        val resultado = MoodPatternDiscovery.analizar(registros)

        assertFalse(resultado.listo)
        assertTrue(resultado.patrones.isEmpty())
        assertEquals(13, resultado.totalRegistros)
        assertFalse(resultado.evaluacionDisponible)
    }

    @Test
    fun encuentraLosTresPatronesPlantadosEn90Dias() {
        val registros = SyntheticDataGenerator.generar90Dias()
        val resultado = MoodPatternDiscovery.analizar(registros)

        assertTrue(resultado.listo)
        assertEquals(90, resultado.totalRegistros)
        assertEquals(3, resultado.patrones.size)
        assertTrue(resultado.evaluacionDisponible)
        assertTrue(resultado.silhouetteScore > resultado.baselineScore)
        assertTrue(resultado.patrones.all { it.totalRegistros >= 5 })

        assertTrue(
            resultado.patrones.any {
                it.promedioAnimo < 4.0 &&
                        it.promedioEnergia < 4.0 &&
                        it.actividadesFrecuentes.any { actividad ->
                            actividad.nombre == "Trabajo"
                        }
            }
        )

        assertTrue(
            resultado.patrones.any {
                it.promedioAnimo >= 7.0 &&
                        it.promedioEnergia >= 8.0 &&
                        it.actividadesFrecuentes.any { actividad ->
                            actividad.nombre == "Ejercicio"
                        }
            }
        )
    }

    @Test
    fun noPresentaGruposConMenosDeCincoRegistros() {
        val registros = (1..14).map { dia ->
            val esDiaAtipico = dia == 14

            MoodRecord(
                fecha = "2026-02-${dia.toString().padStart(2, '0')}",
                estado = if (esDiaAtipico) 8 else 4,
                energia = if (esDiaAtipico) 10 else 5,
                actividades = if (esDiaAtipico) {
                    listOf("Ejercicio")
                } else {
                    listOf("Trabajo")
                }
            )
        }

        val resultado = MoodPatternDiscovery.analizar(registros)

        assertTrue(resultado.listo)
        assertEquals(1, resultado.patrones.size)
        assertEquals(14, resultado.patrones.first().totalRegistros)
        assertFalse(resultado.evaluacionDisponible)
        assertTrue(resultado.patrones.all { it.totalRegistros >= 5 })
    }

    @Test
    fun identificaFechasYActividadesFrecuentes() {
        val resultado = MoodPatternDiscovery.analizar(
            SyntheticDataGenerator.generar90Dias()
        )

        assertTrue(resultado.patrones.all {
            it.fechas.size == it.totalRegistros
        })

        assertTrue(resultado.patrones.all {
            it.actividadesFrecuentes.isNotEmpty()
        })

        assertTrue(resultado.patrones.all {
            it.actividadesFrecuentes.first().porcentaje >= 90.0
        })
    }

    @Test
    fun registrosIdenticosSeMuestranComoResumenGeneral() {
        val registros = (1..14).map { dia ->
            MoodRecord(
                fecha = "2026-03-${dia.toString().padStart(2, '0')}",
                estado = 7,
                energia = 8,
                actividades = listOf("Descanso")
            )
        }

        val resultado = MoodPatternDiscovery.analizar(registros)

        assertTrue(resultado.listo)
        assertEquals(1, resultado.patrones.size)
        assertEquals("Resumen general", resultado.patrones.first().titulo)
        assertFalse(resultado.evaluacionDisponible)
    }
}
