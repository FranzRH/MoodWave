
package com.example.moodwave.analytics

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

data class MoodPatternActivity(
    val nombre: String,
    val totalRegistros: Int,
    val porcentaje: Double
)

data class MoodPattern(
    val id: Int,
    val titulo: String,
    val promedioAnimo: Double,
    val promedioEnergia: Double,
    val totalRegistros: Int,
    val descripcion: String,
    val fechas: List<String>,
    val actividadesFrecuentes: List<MoodPatternActivity>
)

data class MoodPatternAnalysis(
    val listo: Boolean,
    val mensaje: String,
    val totalRegistros: Int,
    val patrones: List<MoodPattern>,
    val silhouetteScore: Double,
    val baselineScore: Double,
    val comparacion: String,
    val evaluacionDisponible: Boolean
)

object MoodPatternDiscovery {

    private const val MIN_DIAS = 14
    private const val NUMERO_GRUPOS = 3
    private const val MIN_REGISTROS_POR_PATRON = 5
    private const val MAX_ITERACIONES = 100
    private const val DIFERENCIA_MINIMA = 0.05
    private const val EPSILON = 0.000000001

    private data class Punto(
        val valores: List<Double>
    )

    fun analizar(
        registros: List<MoodRecord>
    ): MoodPatternAnalysis {

        val registrosValidos = registros
            .filter {
                it.estado in 1..8 &&
                        it.energia in 1..10 &&
                        it.fecha.isNotBlank()
            }
            .distinctBy { it.fecha }
            .sortedBy { it.fecha }

        val totalDias = registrosValidos.size

        if (totalDias < MIN_DIAS) {
            return MoodPatternAnalysis(
                listo = false,
                mensaje = "Hay $totalDias días registrados. Se necesitan al menos 14 días distintos para descubrir patrones. Mientras tanto, los resultados se mantienen pendientes.",
                totalRegistros = totalDias,
                patrones = emptyList(),
                silhouetteScore = 0.0,
                baselineScore = 0.0,
                comparacion = "Evaluación pendiente",
                evaluacionDisponible = false
            )
        }

        val nombresActividades = registrosValidos
            .flatMap { it.actividades }
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

        val pesoBase = if (nombresActividades.isEmpty()) {
            sqrt(0.5)
        } else {
            sqrt(0.4)
        }

        val pesoActividad = if (nombresActividades.isEmpty()) {
            0.0
        } else {
            sqrt(0.2 / nombresActividades.size.toDouble())
        }

        val puntos = registrosValidos.map { registro ->

            val actividadesDia = registro.actividades
                .map { it.trim().lowercase() }
                .filter { it.isNotBlank() }
                .toSet()

            val valores = mutableListOf(
                ((registro.estado - 1) / 7.0) * pesoBase,
                ((registro.energia - 1) / 9.0) * pesoBase
            )

            nombresActividades.forEach { actividad ->
                valores.add(
                    if (actividad in actividadesDia) {
                        pesoActividad
                    } else {
                        0.0
                    }
                )
            }

            Punto(valores)
        }

        val cantidadPuntosDistintos = puntos
            .map { it.valores }
            .distinct()
            .size

        if (cantidadPuntosDistintos < 2) {

            val resumen = crearPatron(
                id = 1,
                registros = registrosValidos,
                tituloForzado = "Resumen general"
            )

            return MoodPatternAnalysis(
                listo = true,
                mensaje = "Los registros presentan características muy similares. Todavía no se distinguen grupos diferentes.",
                totalRegistros = totalDias,
                patrones = listOf(resumen),
                silhouetteScore = 0.0,
                baselineScore = 0.0,
                comparacion = "No existe suficiente variación para evaluar la separación entre grupos.",
                evaluacionDisponible = false
            )
        }

        var kActual = minOf(
            NUMERO_GRUPOS,
            puntos.size / MIN_REGISTROS_POR_PATRON,
            cantidadPuntosDistintos
        )

        var asignacionesValidas: IntArray? = null

        while (kActual >= 2) {

            val asignacionesCandidatas = ejecutarKMeans(
                puntos = puntos,
                k = kActual
            )

            val tamanosGrupos = asignacionesCandidatas
                .asIterable()
                .groupingBy { it }
                .eachCount()

            val gruposValidos = tamanosGrupos.size >= 2 &&
                    tamanosGrupos.values.all {
                        it >= MIN_REGISTROS_POR_PATRON
                    }

            if (gruposValidos) {
                asignacionesValidas = asignacionesCandidatas
                break
            }

            kActual--
        }

        if (asignacionesValidas == null) {

            val resumen = crearPatron(
                id = 1,
                registros = registrosValidos,
                tituloForzado = "Resumen general"
            )

            return MoodPatternAnalysis(
                listo = true,
                mensaje = "Se analizaron $totalDias días, pero no se encontraron al menos dos grupos con cinco registros cada uno. Se muestra un resumen general.",
                totalRegistros = totalDias,
                patrones = listOf(resumen),
                silhouetteScore = 0.0,
                baselineScore = 0.0,
                comparacion = "No se puede evaluar la separación porque no se encontraron grupos con una muestra suficiente.",
                evaluacionDisponible = false
            )
        }

        val asignacionesBaseline = crearBaseline(puntos)

        val silhouette = calcularSilhouette(
            puntos,
            asignacionesValidas
        )

        val baseline = calcularSilhouette(
            puntos,
            asignacionesBaseline
        )

        val grupos = asignacionesValidas.indices
            .groupBy { asignacionesValidas[it] }

        val patrones = grupos
            .map { (id, indices) ->

                val registrosGrupo = indices.map {
                    registrosValidos[it]
                }

                crearPatron(
                    id = id + 1,
                    registros = registrosGrupo
                )
            }
            .sortedBy {
                (it.promedioAnimo + it.promedioEnergia) / 2.0
            }
            .mapIndexed { index, patron ->
                patron.copy(id = index + 1)
            }

        val comparacion = when {
            silhouette >= baseline + DIFERENCIA_MINIMA ->
                "K-means consiguió una mejor separación que el método de referencia."

            silhouette <= baseline - DIFERENCIA_MINIMA ->
                "El método de referencia consiguió una mejor separación en estos registros."

            else ->
                "Ambos métodos consiguieron resultados de separación similares."
        }

        return MoodPatternAnalysis(
            listo = true,
            mensaje = "Se analizaron $totalDias días y se identificaron ${patrones.size} grupos. Cada grupo contiene al menos cinco registros.",
            totalRegistros = totalDias,
            patrones = patrones,
            silhouetteScore = silhouette,
            baselineScore = baseline,
            comparacion = comparacion,
            evaluacionDisponible = true
        )
    }

    private fun ejecutarKMeans(
        puntos: List<Punto>,
        k: Int
    ): IntArray {

        var centroides = inicializarCentroides(
            puntos,
            k
        )

        var asignaciones = IntArray(puntos.size) {
            -1
        }

        repeat(MAX_ITERACIONES) {

            val nuevasAsignaciones = IntArray(puntos.size)

            puntos.forEachIndexed { indice, punto ->

                var grupoCercano = 0
                var distanciaMinima = Double.POSITIVE_INFINITY

                centroides.forEachIndexed { grupo, centroide ->

                    val distanciaActual = distancia(
                        punto,
                        centroide
                    )

                    if (distanciaActual < distanciaMinima) {
                        distanciaMinima = distanciaActual
                        grupoCercano = grupo
                    }
                }

                nuevasAsignaciones[indice] = grupoCercano
            }

            val nuevosCentroides = centroides.indices.map { grupo ->

                val puntosGrupo = puntos.indices
                    .filter {
                        nuevasAsignaciones[it] == grupo
                    }
                    .map {
                        puntos[it]
                    }

                if (puntosGrupo.isEmpty()) {
                    centroides[grupo]
                } else {

                    val dimensiones = puntos.first().valores.indices.map { dimension ->
                        puntosGrupo
                            .map { it.valores[dimension] }
                            .average()
                    }

                    Punto(dimensiones)
                }
            }

            val sinCambios = nuevasAsignaciones
                .contentEquals(asignaciones)

            asignaciones = nuevasAsignaciones
            centroides = nuevosCentroides

            if (sinCambios) {
                return asignaciones
            }
        }

        return asignaciones
    }

    private fun inicializarCentroides(
        puntos: List<Punto>,
        k: Int
    ): List<Punto> {

        val seleccionados = mutableListOf<Int>()

        seleccionados.add(puntos.size / 2)

        while (seleccionados.size < k) {

            val siguiente = puntos.indices
                .filterNot { it in seleccionados }
                .maxByOrNull { indice ->

                    seleccionados.minOf { seleccionado ->
                        distancia(
                            puntos[indice],
                            puntos[seleccionado]
                        )
                    }
                }

            if (siguiente == null) break

            seleccionados.add(siguiente)
        }

        return seleccionados.map {
            puntos[it]
        }
    }

    private fun crearBaseline(
        puntos: List<Punto>
    ): IntArray {

        val puntuaciones = puntos.map {
            it.valores[0] + it.valores[1]
        }

        val minimo = puntuaciones.minOrNull() ?: return IntArray(puntos.size)
        val maximo = puntuaciones.maxOrNull() ?: return IntArray(puntos.size)

        if (abs(maximo - minimo) < EPSILON) {
            return IntArray(puntos.size)
        }

        val umbral = puntuaciones.sorted()[
            puntuaciones.size / 2
        ]

        return IntArray(puntos.size) { indice ->
            if (puntuaciones[indice] <= umbral) 0 else 1
        }
    }

    private fun calcularSilhouette(
        puntos: List<Punto>,
        asignaciones: IntArray
    ): Double {

        val grupos = asignaciones.indices
            .groupBy { asignaciones[it] }

        if (grupos.size < 2) return 0.0

        val valores = puntos.indices.map { indice ->

            val grupoPropio = asignaciones[indice]

            val otrosDelGrupo = grupos[grupoPropio]
                .orEmpty()
                .filter { it != indice }

            if (otrosDelGrupo.isEmpty()) {
                return@map 0.0
            }

            val distanciaInterna = otrosDelGrupo
                .map {
                    distancia(puntos[indice], puntos[it])
                }
                .average()

            val distanciaExterna = grupos
                .filterKeys { it != grupoPropio }
                .values
                .map { grupo ->
                    grupo.map {
                        distancia(puntos[indice], puntos[it])
                    }.average()
                }
                .minOrNull() ?: return@map 0.0

            val divisor = max(
                distanciaInterna,
                distanciaExterna
            )

            if (divisor <= EPSILON) {
                0.0
            } else {
                (distanciaExterna - distanciaInterna) / divisor
            }
        }

        val promedio = valores.average()

        return if (promedio.isFinite()) {
            promedio.coerceIn(-1.0, 1.0)
        } else {
            0.0
        }
    }

    private fun distancia(
        a: Punto,
        b: Punto
    ): Double {

        var suma = 0.0

        a.valores.indices.forEach { indice ->

            suma += (
                    a.valores[indice] - b.valores[indice]
                    ).pow(2)
        }

        return sqrt(suma)
    }

    private fun crearPatron(
        id: Int,
        registros: List<MoodRecord>,
        tituloForzado: String? = null
    ): MoodPattern {

        val promedioAnimo = registros
            .map { it.estado }
            .average()

        val promedioEnergia = registros
            .map { it.energia }
            .average()

        val nombresActividades = registros
            .flatMap { it.actividades }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }

        val actividadesFrecuentes = nombresActividades
            .map { nombre ->

                val total = registros.count { registro ->
                    registro.actividades.any {
                        it.equals(nombre, ignoreCase = true)
                    }
                }

                MoodPatternActivity(
                    nombre = nombre,
                    totalRegistros = total,
                    porcentaje = total.toDouble() / registros.size * 100.0
                )
            }
            .sortedWith(
                compareByDescending<MoodPatternActivity> { it.totalRegistros }
                    .thenBy { it.nombre }
            )
            .take(3)

        val tituloAnimo = when {
            promedioAnimo < 3.5 -> "Ánimo bajo"
            promedioAnimo < 5.8 -> "Ánimo intermedio"
            else -> "Ánimo alto"
        }

        val tituloEnergia = when {
            promedioEnergia < 4.0 -> "energía baja"
            promedioEnergia < 7.0 -> "energía moderada"
            else -> "energía alta"
        }

        val titulo = tituloForzado ?: "$tituloAnimo y $tituloEnergia"

        val descripcion = if (tituloForzado != null) {

            "Este resumen reúne los ${registros.size} días disponibles. No se separaron en grupos porque los datos todavía no permiten distinguir patrones con una muestra mínima adecuada."

        } else {

            "Este grupo reúne ${registros.size} días con $titulo. La agrupación considera similitudes de ánimo, energía y actividades registradas. Las actividades describen el contexto de esos días, no las causas del estado de ánimo."
        }

        return MoodPattern(
            id = id,
            titulo = titulo,
            promedioAnimo = promedioAnimo,
            promedioEnergia = promedioEnergia,
            totalRegistros = registros.size,
            descripcion = descripcion,
            fechas = registros.map { it.fecha }.distinct().sorted(),
            actividadesFrecuentes = actividadesFrecuentes
        )
    }
}
