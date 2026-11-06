
package com.example.moodwave.analytics

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

object MoodAnalytics {

    private const val MIN_DIAS_TOTALES = 14
    private const val MIN_REGISTROS_ACTIVIDAD = 5

    fun calcularEstadisticas(
        registros: List<MoodRecord>
    ): EmotionalStats {

        if (registros.isEmpty()) {
            return EmotionalStats(
                promedio = 0.0,
                mediana = 0.0,
                moda = 0,
                desviacion = 0.0,
                totalRegistros = 0,
                rachaActual = 0
            )
        }

        val estados = registros.map { it.estado }
        val promedio = estados.average()
        val ordenados = estados.sorted()

        val mediana =
            if (ordenados.size % 2 == 0) {
                val mitad = ordenados.size / 2
                (ordenados[mitad - 1] + ordenados[mitad]) / 2.0
            } else {
                ordenados[ordenados.size / 2].toDouble()
            }

        val moda =
            estados.groupingBy { it }
                .eachCount()
                .maxByOrNull { it.value }
                ?.key ?: 0

        val varianza =
            estados.map {
                (it - promedio).pow(2)
            }.average()

        val desviacion = sqrt(varianza)
        val rachaActual = calcularRachaActual(registros)

        return EmotionalStats(
            promedio = promedio,
            mediana = mediana,
            moda = moda,
            desviacion = desviacion,
            totalRegistros = registros.size,
            rachaActual = rachaActual
        )
    }

    fun calcularEstadisticasSemana(
        registros: List<MoodRecord>,
        fechaReferencia: String
    ): EmotionalStats {

        return calcularEstadisticas(
            obtenerRegistrosSemana(registros, fechaReferencia)
        )
    }

    fun calcularEstadisticasMes(
        registros: List<MoodRecord>,
        fechaReferencia: String
    ): EmotionalStats {

        return calcularEstadisticas(
            obtenerRegistrosMes(registros, fechaReferencia)
        )
    }

    fun calcularTendencia7Dias(
        registros: List<MoodRecord>,
        fechaReferencia: String
    ): List<TrendPoint> {

        return calcularTendencia(registros, fechaReferencia, 7)
    }

    fun calcularTendencia30Dias(
        registros: List<MoodRecord>,
        fechaReferencia: String
    ): List<TrendPoint> {

        return calcularTendencia(registros, fechaReferencia, 30)
    }

    fun calcularTendencia90Dias(
        registros: List<MoodRecord>,
        fechaReferencia: String
    ): List<TrendPoint> {

        return calcularTendencia(registros, fechaReferencia, 90)
    }

    private fun calcularTendencia(
        registros: List<MoodRecord>,
        fechaReferencia: String,
        dias: Int
    ): List<TrendPoint> {

        if (registros.isEmpty()) return emptyList()

        val fechaFinal =
            convertirFecha(fechaReferencia) ?: return emptyList()

        val formato = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        val fechaFinalTexto = formato.format(fechaFinal)

        val calendarioInicio =
            Calendar.getInstance().apply {
                time = fechaFinal
                limpiarHora(this)
                add(Calendar.DAY_OF_MONTH, -(dias - 1))
            }

        val fechaInicioTexto =
            formato.format(calendarioInicio.time)

        val registrosPeriodo =
            registros.filter { registro ->
                convertirFecha(registro.fecha) != null &&
                        registro.fecha >= fechaInicioTexto &&
                        registro.fecha <= fechaFinalTexto
            }.sortedBy { it.fecha }

        return registrosPeriodo.map { registro ->

            val fechaActual =
                convertirFecha(registro.fecha)!!

            val inicioMediaMovil =
                Calendar.getInstance().apply {
                    time = fechaActual
                    limpiarHora(this)
                    add(Calendar.DAY_OF_MONTH, -6)
                }

            val fechaInicioMedia =
                formato.format(inicioMediaMovil.time)

            val registrosMedia =
                registros.filter {
                    convertirFecha(it.fecha) != null &&
                            it.fecha >= fechaInicioMedia &&
                            it.fecha <= registro.fecha
                }

            val mediaMovil =
                registrosMedia.map {
                    it.estado.toDouble()
                }.average()

            TrendPoint(
                fecha = registro.fecha,
                estado = registro.estado.toDouble(),
                energia = registro.energia.toDouble(),
                mediaMovilEstado = mediaMovil
            )
        }
    }

    fun calcularActividadVsAnimo(
        registros: List<MoodRecord>
    ): List<ActivityStats> {

        if (registros.isEmpty()) return emptyList()

        val mediaGeneral =
            registros.map { it.estado }.average()

        val diasTotales =
            registros.map { it.fecha }.distinct().size

        val actividades = obtenerActividades(registros)

        return actividades.map { actividad ->

            val registrosActividad =
                registros.filter {
                    tieneActividad(it, actividad)
                }

            val registrosSinActividad =
                registros.filter {
                    !tieneActividad(it, actividad)
                }

            val estadoPromedio =
                registrosActividad.map { it.estado }.average()

            val energiaPromedio =
                registrosActividad.map { it.energia }.average()

            val datosSuficientes =
                diasTotales >= MIN_DIAS_TOTALES &&
                        registrosActividad.size >= MIN_REGISTROS_ACTIVIDAD

            val tamanoEfecto =
                calcularCohensD(
                    registrosActividad.map { it.estado.toDouble() },
                    registrosSinActividad.map { it.estado.toDouble() }
                )

            ActivityStats(
                actividad = actividad,
                estadoPromedio = estadoPromedio,
                energiaPromedio = energiaPromedio,
                diferenciaMediaGeneral = estadoPromedio - mediaGeneral,
                totalRegistros = registrosActividad.size,
                datosSuficientes = datosSuficientes,
                tamanoEfecto = tamanoEfecto,
                nivelEfecto = clasificarTamanoEfecto(tamanoEfecto)
            )
        }
    }

    fun calcularCorrelacionesActividad(
        registros: List<MoodRecord>
    ): List<CorrelationResult> {

        if (registros.isEmpty()) return emptyList()

        val diasTotales =
            registros.map { it.fecha }.distinct().size

        val actividades = obtenerActividades(registros)

        return actividades.map { actividad ->

            val conActividad =
                registros.filter {
                    tieneActividad(it, actividad)
                }

            val sinActividad =
                registros.filter {
                    !tieneActividad(it, actividad)
                }

            val coeficiente =
                calcularPointBiserial(registros, actividad)

            val datosSuficientes =
                diasTotales >= MIN_DIAS_TOTALES &&
                        conActividad.size >= MIN_REGISTROS_ACTIVIDAD &&
                        sinActividad.size >= MIN_REGISTROS_ACTIVIDAD

            CorrelationResult(
                actividad = actividad,
                coeficiente = coeficiente,
                nivelRelacion = clasificarCorrelacion(coeficiente),
                direccion = obtenerDireccion(coeficiente),
                totalConActividad = conActividad.size,
                totalSinActividad = sinActividad.size,
                datosSuficientes = datosSuficientes
            )
        }
    }

    private fun calcularPointBiserial(
        registros: List<MoodRecord>,
        actividad: String
    ): Double {

        if (registros.size < 2) return 0.0

        val conActividad =
            registros.filter { tieneActividad(it, actividad) }

        val sinActividad =
            registros.filter { !tieneActividad(it, actividad) }

        if (conActividad.isEmpty() || sinActividad.isEmpty()) {
            return 0.0
        }

        val mediaCon =
            conActividad.map { it.estado }.average()

        val mediaSin =
            sinActividad.map { it.estado }.average()

        val estados =
            registros.map { it.estado.toDouble() }

        val mediaTotal = estados.average()

        val varianza =
            estados.map {
                (it - mediaTotal).pow(2)
            }.average()

        val desviacion = sqrt(varianza)

        if (desviacion == 0.0) return 0.0

        val proporcionCon =
            conActividad.size.toDouble() / registros.size

        val proporcionSin =
            sinActividad.size.toDouble() / registros.size

        return ((mediaCon - mediaSin) / desviacion) *
                sqrt(proporcionCon * proporcionSin)
    }

    private fun clasificarCorrelacion(
        coeficiente: Double
    ): String {

        return when (abs(coeficiente)) {
            in 0.0..<0.10 -> "Muy débil"
            in 0.10..<0.30 -> "Débil"
            in 0.30..<0.50 -> "Moderada"
            else -> "Fuerte"
        }
    }

    private fun obtenerDireccion(
        coeficiente: Double
    ): String {

        return when {
            coeficiente > 0.001 -> "Positiva"
            coeficiente < -0.001 -> "Negativa"
            else -> "Sin dirección"
        }
    }

    private fun obtenerActividades(
        registros: List<MoodRecord>
    ): List<String> {

        return registros.flatMap { it.actividades }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .sorted()
    }

    private fun tieneActividad(
        registro: MoodRecord,
        actividad: String
    ): Boolean {

        return registro.actividades.any {
            it.equals(actividad, ignoreCase = true)
        }
    }

    private fun calcularCohensD(
        grupoActividad: List<Double>,
        grupoSinActividad: List<Double>
    ): Double {

        if (grupoActividad.size < 2 || grupoSinActividad.size < 2) {
            return 0.0
        }

        val mediaActividad = grupoActividad.average()
        val mediaSinActividad = grupoSinActividad.average()

        val varianzaActividad =
            calcularVarianzaMuestral(grupoActividad)

        val varianzaSinActividad =
            calcularVarianzaMuestral(grupoSinActividad)

        val gradosLibertad =
            grupoActividad.size + grupoSinActividad.size - 2

        if (gradosLibertad <= 0) return 0.0

        val varianzaCombinada =
            (
                    (grupoActividad.size - 1) * varianzaActividad +
                            (grupoSinActividad.size - 1) * varianzaSinActividad
                    ) / gradosLibertad

        val desviacionCombinada = sqrt(varianzaCombinada)

        if (desviacionCombinada == 0.0) return 0.0

        return (mediaActividad - mediaSinActividad) / desviacionCombinada
    }

    private fun calcularVarianzaMuestral(
        valores: List<Double>
    ): Double {

        if (valores.size < 2) return 0.0

        val media = valores.average()

        val suma =
            valores.sumOf {
                (it - media).pow(2)
            }

        return suma / (valores.size - 1)
    }

    private fun clasificarTamanoEfecto(
        valor: Double
    ): String {

        return when (abs(valor)) {
            in 0.0..<0.20 -> "Muy pequeño"
            in 0.20..<0.50 -> "Pequeño"
            in 0.50..<0.80 -> "Moderado"
            else -> "Grande"
        }
    }

    private fun obtenerRegistrosSemana(
        registros: List<MoodRecord>,
        fechaReferencia: String
    ): List<MoodRecord> {

        val fechaBase =
            convertirFecha(fechaReferencia) ?: return emptyList()

        val inicioSemana =
            Calendar.getInstance().apply {
                time = fechaBase

                val desplazamiento =
                    (get(Calendar.DAY_OF_WEEK) + 5) % 7

                add(Calendar.DAY_OF_MONTH, -desplazamiento)
                limpiarHora(this)
            }

        val finSemana =
            (inicioSemana.clone() as Calendar).apply {
                add(Calendar.DAY_OF_MONTH, 6)
            }

        return registros.filter { registro ->

            val fecha = convertirFecha(registro.fecha)

            fecha != null &&
                    !fecha.before(inicioSemana.time) &&
                    !fecha.after(finSemana.time)
        }
    }

    private fun obtenerRegistrosMes(
        registros: List<MoodRecord>,
        fechaReferencia: String
    ): List<MoodRecord> {

        val fechaBase =
            convertirFecha(fechaReferencia) ?: return emptyList()

        val calendarioBase =
            Calendar.getInstance().apply {
                time = fechaBase
            }

        val anio = calendarioBase.get(Calendar.YEAR)
        val mes = calendarioBase.get(Calendar.MONTH)

        return registros.filter { registro ->

            val fecha =
                convertirFecha(registro.fecha) ?: return@filter false

            val calendario =
                Calendar.getInstance().apply {
                    time = fecha
                }

            calendario.get(Calendar.YEAR) == anio &&
                    calendario.get(Calendar.MONTH) == mes
        }
    }

    private fun calcularRachaActual(
        registros: List<MoodRecord>
    ): Int {

        if (registros.isEmpty()) return 0

        val formato =
            SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                isLenient = false
            }

        val fechas =
            registros.mapNotNull {
                try {
                    formato.parse(it.fecha)
                } catch (_: Exception) {
                    null
                }
            }.distinct().sortedDescending()

        if (fechas.isEmpty()) return 0

        var racha = 1

        for (i in 0 until fechas.size - 1) {

            val fechaActual =
                Calendar.getInstance().apply {
                    time = fechas[i]
                }

            fechaActual.add(Calendar.DAY_OF_MONTH, -1)

            val fechaEsperada =
                formato.format(fechaActual.time)

            val fechaSiguiente =
                formato.format(fechas[i + 1])

            if (fechaEsperada == fechaSiguiente) {
                racha++
            } else {
                break
            }
        }

        return racha
    }

    private fun convertirFecha(
        fecha: String
    ): Date? {

        return try {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                isLenient = false
            }.parse(fecha)
        } catch (_: Exception) {
            null
        }
    }

    private fun limpiarHora(
        calendario: Calendar
    ) {

        calendario.set(Calendar.HOUR_OF_DAY, 0)
        calendario.set(Calendar.MINUTE, 0)
        calendario.set(Calendar.SECOND, 0)
        calendario.set(Calendar.MILLISECOND, 0)
    }
}