package com.example.moodwave.reports

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.moodwave.analytics.ActivityStats
import com.example.moodwave.analytics.CorrelationResult
import com.example.moodwave.analytics.EmotionalStats
import com.example.moodwave.analytics.MoodAnalytics
import com.example.moodwave.analytics.MoodPattern
import com.example.moodwave.analytics.MoodPatternAnalysis
import com.example.moodwave.analytics.MoodPatternDiscovery
import com.example.moodwave.analytics.MoodRecord
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object MonthlyReportPdfGenerator {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 42f

    private val azul = Color.rgb(11, 92, 142)
    private val verde = Color.rgb(30, 158, 131)
    private val texto = Color.rgb(35, 52, 67)
    private val gris = Color.rgb(100, 112, 122)
    private val grisClaro = Color.rgb(232, 239, 244)
    private val fondoTarjeta = Color.rgb(245, 249, 252)

    fun generar(
        registros: List<MoodRecord>,
        fechaReferencia: String = fechaActual(),
        esDemostracion: Boolean = false
    ): ByteArray {
        val registrosMes = filtrarMes(registros, fechaReferencia)
        val estadisticas = MoodAnalytics.calcularEstadisticas(registrosMes)
        val actividades = MoodAnalytics.calcularActividadVsAnimo(registrosMes)
        val correlaciones = MoodAnalytics.calcularCorrelacionesActividad(registrosMes)
        val patrones = MoodPatternDiscovery.analizar(registrosMes)
        val periodo = nombreMes(fechaReferencia)

        val documento = PdfDocument()
        val salida = ByteArrayOutputStream()

        try {
            val paginaUno = documento.startPage(
                PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            )
            dibujarPrimeraPagina(
                paginaUno.canvas,
                periodo,
                registrosMes,
                estadisticas,
                esDemostracion
            )
            documento.finishPage(paginaUno)

            val paginaDos = documento.startPage(
                PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 2).create()
            )
            dibujarSegundaPagina(
                paginaDos.canvas,
                periodo,
                actividades,
                correlaciones,
                esDemostracion
            )
            documento.finishPage(paginaDos)

            val paginaTres = documento.startPage(
                PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 3).create()
            )
            dibujarTerceraPagina(
                paginaTres.canvas,
                periodo,
                registrosMes,
                patrones,
                estadisticas,
                esDemostracion
            )
            documento.finishPage(paginaTres)

            documento.writeTo(salida)
            return salida.toByteArray()
        } finally {
            documento.close()
            salida.close()
        }
    }

    private fun dibujarPrimeraPagina(
        canvas: Canvas,
        periodo: String,
        registros: List<MoodRecord>,
        estadisticas: EmotionalStats,
        esDemostracion: Boolean
    ) {
        canvas.drawColor(Color.WHITE)
        dibujarEncabezado(canvas, "Reporte mensual", periodo, esDemostracion)

        var y = 154f
        dibujarTituloSeccion(canvas, "Resumen emocional", y)
        y += 26f

        if (registros.isEmpty()) {
            dibujarTextoEnvuelto(
                canvas,
                "No hay registros en el mes seleccionado. Cuando agregues registros, aquí aparecerá el resumen mensual.",
                MARGIN,
                y + 8f,
                PAGE_WIDTH - MARGIN * 2,
                13f,
                gris
            )
        } else {
            val valores = listOf(
                "Promedio" to formato(estadisticas.promedio),
                "Mediana" to formato(estadisticas.mediana),
                "Moda" to nombreEstado(estadisticas.moda),
                "Desviación" to formato(estadisticas.desviacion),
                "Registros" to estadisticas.totalRegistros.toString(),
                "Racha actual" to "${estadisticas.rachaActual} días"
            )
            y = dibujarMetricas(canvas, valores, y)
        }

        y += 18f
        dibujarTituloSeccion(canvas, "Evolución de ánimo y energía", y)
        y += 22f
        dibujarGraficaLineas(canvas, registros, MARGIN, y, PAGE_WIDTH - MARGIN * 2, 250f)
        y += 270f

        dibujarTituloSeccion(canvas, "Lectura general", y)
        y += 24f
        crearConclusionesGenerales(registros, estadisticas).forEach { conclusion ->
            y = dibujarTextoEnvuelto(
                canvas,
                "• $conclusion",
                MARGIN,
                y,
                PAGE_WIDTH - MARGIN * 2,
                11f,
                texto
            ) + 6f
        }

        dibujarPie(canvas, 1, esDemostracion)
    }

    private fun dibujarSegundaPagina(
        canvas: Canvas,
        periodo: String,
        actividades: List<ActivityStats>,
        correlaciones: List<CorrelationResult>,
        esDemostracion: Boolean
    ) {
        canvas.drawColor(Color.WHITE)
        dibujarEncabezado(canvas, "Actividad y correlaciones", periodo, esDemostracion)

        var y = 150f
        dibujarTituloSeccion(canvas, "Promedios por actividad", y)
        y += 24f

        val actividadesGrafica = actividades
            .filter { it.totalRegistros >= 1 && it.estadoPromedio.isFinite() }
            .sortedByDescending { it.estadoPromedio }
            .take(6)

        dibujarGraficaBarras(
            canvas,
            actividadesGrafica,
            MARGIN,
            y,
            PAGE_WIDTH - MARGIN * 2,
            225f
        )
        y += 248f

        dibujarTituloSeccion(canvas, "Relaciones observadas", y)
        y += 24f

        val correlacionesValidas = correlaciones
            .filter { it.datosSuficientes }
            .sortedByDescending { kotlin.math.abs(it.coeficiente) }
            .take(5)

        if (correlacionesValidas.isEmpty()) {
            y = dibujarTextoEnvuelto(
                canvas,
                "Aún no hay datos suficientes para interpretar correlaciones. Se requieren al menos 14 días y 5 registros con y 5 sin cada actividad.",
                MARGIN,
                y,
                PAGE_WIDTH - MARGIN * 2,
                11.5f,
                gris
            ) + 8f
        } else {
            correlacionesValidas.forEach { correlacion ->
                val frase = "${correlacion.actividad}: se observa una relación ${correlacion.direccion.lowercase()} ${correlacion.nivelRelacion.lowercase()} con el ánimo (r = ${formato(correlacion.coeficiente)}; n = ${correlacion.totalConActividad} con actividad y ${correlacion.totalSinActividad} sin ella)."
                y = dibujarTextoEnvuelto(
                    canvas,
                    "• $frase",
                    MARGIN,
                    y,
                    PAGE_WIDTH - MARGIN * 2,
                    10.5f,
                    texto
                ) + 7f
            }
        }

        y += 12f
        dibujarTextoEnvuelto(
            canvas,
            "Método utilizado: correlación punto-biserial. La actividad se considera presente o ausente y se compara con el estado de ánimo registrado. Las comparaciones son exploratorias; al analizar varias actividades, alguna relación puede aparecer por azar.",
            MARGIN,
            y,
            PAGE_WIDTH - MARGIN * 2,
            10.5f,
            gris
        )

        dibujarPie(canvas, 2, esDemostracion)
    }

    private fun dibujarTerceraPagina(
        canvas: Canvas,
        periodo: String,
        registros: List<MoodRecord>,
        patrones: MoodPatternAnalysis,
        estadisticas: EmotionalStats,
        esDemostracion: Boolean
    ) {
        canvas.drawColor(Color.WHITE)
        dibujarEncabezado(canvas, "Patrones y conclusiones", periodo, esDemostracion)

        var y = 150f
        dibujarTituloSeccion(canvas, "Descubrimiento automático de patrones", y)
        y += 24f

        y = dibujarTextoEnvuelto(
            canvas,
            patrones.mensaje,
            MARGIN,
            y,
            PAGE_WIDTH - MARGIN * 2,
            11f,
            texto
        ) + 8f

        if (patrones.evaluacionDisponible) {
            y = dibujarTextoEnvuelto(
                canvas,
                "Evaluación: Silhouette de K-means = ${formato(patrones.silhouetteScore)}; método de referencia = ${formato(patrones.baselineScore)}. ${patrones.comparacion}",
                MARGIN,
                y,
                PAGE_WIDTH - MARGIN * 2,
                10.5f,
                texto
            ) + 10f
        } else {
            y = dibujarTextoEnvuelto(
                canvas,
                patrones.comparacion,
                MARGIN,
                y,
                PAGE_WIDTH - MARGIN * 2,
                10.5f,
                gris
            ) + 10f
        }

        if (patrones.patrones.isEmpty()) {
            y = dibujarTextoEnvuelto(
                canvas,
                "No se presentan grupos porque la muestra del mes todavía no permite identificarlos con el umbral mínimo establecido.",
                MARGIN,
                y,
                PAGE_WIDTH - MARGIN * 2,
                10.5f,
                gris
            ) + 10f
        } else {
            patrones.patrones.take(3).forEach { patron ->
                y = dibujarPatron(canvas, patron, y)
            }
        }

        y += 12f
        dibujarTituloSeccion(canvas, "Conclusiones", y)
        y += 24f

        crearConclusionesGenerales(registros, estadisticas).forEach { conclusion ->
            y = dibujarTextoEnvuelto(
                canvas,
                "• $conclusion",
                MARGIN,
                y,
                PAGE_WIDTH - MARGIN * 2,
                10.5f,
                texto
            ) + 5f
        }

        val cierre = "Los resultados describen los registros del periodo. Las actividades que coinciden con determinados estados no demuestran causalidad. MoodWave no realiza diagnósticos médicos ni sustituye la atención profesional."
        dibujarTextoEnvuelto(
            canvas,
            cierre,
            MARGIN,
            y + 8f,
            PAGE_WIDTH - MARGIN * 2,
            9.5f,
            gris
        )

        dibujarPie(canvas, 3, esDemostracion)
    }

    private fun dibujarPatron(
        canvas: Canvas,
        patron: MoodPattern,
        yInicial: Float
    ): Float {
        val x = MARGIN
        val ancho = PAGE_WIDTH - MARGIN * 2
        val actividades = patron.actividadesFrecuentes
            .take(2)
            .joinToString(", ") { "${it.nombre} (${it.totalRegistros}/${patron.totalRegistros})" }
            .ifBlank { "Sin actividades destacadas" }
        val fechas = patron.fechas.take(4).joinToString(" · ") { formatearFecha(it) }
        val detalle = "${patron.totalRegistros} días · ánimo ${formato(patron.promedioAnimo)}/8 · energía ${formato(patron.promedioEnergia)}/10"

        val alto = 96f
        val fondo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fondoTarjeta
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(
            RectF(x, yInicial, x + ancho, yInicial + alto),
            10f,
            10f,
            fondo
        )
        dibujarTexto(canvas, patron.titulo, x + 12f, yInicial + 20f, 12f, azul, true)
        dibujarTexto(canvas, detalle, x + 12f, yInicial + 39f, 9.5f, texto, false)
        dibujarTextoEnvuelto(
            canvas,
            "Actividades frecuentes: $actividades",
            x + 12f,
            yInicial + 57f,
            ancho - 24f,
            9f,
            texto
        )
        dibujarTexto(
            canvas,
            "Fechas: ${fechas.ifBlank { "No disponibles" }}${if (patron.fechas.size > 4) " y más" else ""}",
            x + 12f,
            yInicial + 86f,
            8.5f,
            gris,
            false
        )
        return yInicial + alto + 9f
    }

    private fun dibujarEncabezado(
        canvas: Canvas,
        titulo: String,
        periodo: String,
        esDemostracion: Boolean
    ) {
        val fondo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = azul
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 118f, fondo)

        dibujarTexto(canvas, "MoodWave", MARGIN, 38f, 16f, Color.WHITE, true)
        dibujarTexto(canvas, titulo, MARGIN, 70f, 23f, Color.WHITE, true)
        dibujarTexto(canvas, periodo, MARGIN, 96f, 12f, Color.WHITE, false)

        if (esDemostracion) {
            val etiqueta = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(255, 239, 204)
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(RectF(420f, 27f, 553f, 53f), 12f, 12f, etiqueta)
            dibujarTexto(canvas, "DEMOSTRACIÓN", 432f, 44f, 9f, azul, true)
        }
    }

    private fun dibujarMetricas(
        canvas: Canvas,
        valores: List<Pair<String, String>>,
        inicioY: Float
    ): Float {
        val separacion = 9f
        val ancho = (PAGE_WIDTH - MARGIN * 2 - separacion * 2) / 3f
        val alto = 64f
        valores.take(6).forEachIndexed { indice, metrica ->
            val fila = indice / 3
            val columna = indice % 3
            val izquierda = MARGIN + columna * (ancho + separacion)
            val arriba = inicioY + fila * (alto + 8f)
            val fondo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = fondoTarjeta
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(
                RectF(izquierda, arriba, izquierda + ancho, arriba + alto),
                10f,
                10f,
                fondo
            )
            dibujarTexto(canvas, metrica.first, izquierda + 10f, arriba + 22f, 9.5f, gris, false)
            dibujarTexto(canvas, metrica.second, izquierda + 10f, arriba + 46f, 15f, texto, true)
        }
        return inicioY + alto * 2 + 8f
    }

    private fun dibujarGraficaLineas(
        canvas: Canvas,
        registros: List<MoodRecord>,
        x: Float,
        y: Float,
        ancho: Float,
        alto: Float
    ) {
        val fondo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fondoTarjeta
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(x, y, x + ancho, y + alto), 12f, 12f, fondo)

        if (registros.isEmpty()) {
            dibujarTexto(canvas, "No hay registros para graficar este mes.", x + 16f, y + 38f, 11f, gris, false)
            return
        }

        val ordenados = registros.sortedBy { it.fecha }
        val izquierda = x + 34f
        val derecha = x + ancho - 18f
        val arriba = y + 28f
        val abajo = y + alto - 42f
        val areaAncho = derecha - izquierda
        val areaAlto = abajo - arriba

        val lineaGuia = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = grisClaro
            strokeWidth = 1f
        }
        for (nivel in 0..4) {
            val py = abajo - areaAlto * nivel / 4f
            canvas.drawLine(izquierda, py, derecha, py, lineaGuia)
        }

        val paintAnimo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = azul
            strokeWidth = 2.8f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        val paintEnergia = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = verde
            strokeWidth = 2.8f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        val puntosAnimo = Path()
        val puntosEnergia = Path()

        ordenados.forEachIndexed { indice, registro ->
            val px = if (ordenados.size == 1) {
                izquierda + areaAncho / 2f
            } else {
                izquierda + areaAncho * indice / (ordenados.size - 1).toFloat()
            }
            val yAnimo = abajo - areaAlto * ((registro.estado - 1) / 7f)
            val yEnergia = abajo - areaAlto * ((registro.energia - 1) / 9f)
            if (indice == 0) {
                puntosAnimo.moveTo(px, yAnimo)
                puntosEnergia.moveTo(px, yEnergia)
            } else {
                puntosAnimo.lineTo(px, yAnimo)
                puntosEnergia.lineTo(px, yEnergia)
            }
            canvas.drawCircle(px, yAnimo, 3.2f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = azul })
            canvas.drawCircle(px, yEnergia, 3.2f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = verde })
        }
        canvas.drawPath(puntosAnimo, paintAnimo)
        canvas.drawPath(puntosEnergia, paintEnergia)

        dibujarTexto(canvas, "Ánimo (1–8)", x + 18f, y + alto - 10f, 9f, azul, true)
        dibujarTexto(canvas, "Energía (1–10)", x + 104f, y + alto - 10f, 9f, verde, true)
        dibujarTexto(canvas, "${ordenados.size} registros", derecha - 75f, y + alto - 10f, 9f, gris, false)
    }

    private fun dibujarGraficaBarras(
        canvas: Canvas,
        actividades: List<ActivityStats>,
        x: Float,
        y: Float,
        ancho: Float,
        alto: Float
    ) {
        val fondo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fondoTarjeta
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(x, y, x + ancho, y + alto), 12f, 12f, fondo)

        if (actividades.isEmpty()) {
            dibujarTexto(canvas, "No hay actividades registradas en este periodo.", x + 16f, y + 38f, 11f, gris, false)
            return
        }

        val espacioFila = 30f
        actividades.forEachIndexed { indice, actividad ->
            val filaY = y + 25f + indice * espacioFila
            dibujarTexto(canvas, actividad.actividad.take(18), x + 14f, filaY + 3f, 9.5f, texto, true)

            val inicioBarra = x + 145f
            val anchoDisponible = 155f
            val anchoAnimo = (actividad.estadoPromedio / 8.0 * anchoDisponible)
                .toFloat()
                .coerceIn(2f, anchoDisponible)
            val anchoEnergia = (actividad.energiaPromedio / 10.0 * anchoDisponible)
                .toFloat()
                .coerceIn(2f, anchoDisponible)

            val barraFondo = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = grisClaro }
            val barraAnimo = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = azul }
            val barraEnergia = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = verde }

            canvas.drawRoundRect(
                RectF(inicioBarra, filaY - 9f, inicioBarra + anchoDisponible, filaY - 2f),
                4f, 4f, barraFondo
            )
            canvas.drawRoundRect(
                RectF(inicioBarra, filaY + 1f, inicioBarra + anchoDisponible, filaY + 8f),
                4f, 4f, barraFondo
            )
            canvas.drawRoundRect(
                RectF(inicioBarra, filaY - 9f, inicioBarra + anchoAnimo, filaY - 2f),
                4f, 4f, barraAnimo
            )
            canvas.drawRoundRect(
                RectF(inicioBarra, filaY + 1f, inicioBarra + anchoEnergia, filaY + 8f),
                4f, 4f, barraEnergia
            )

            val diferencia = if (actividad.diferenciaMediaGeneral > 0) "+" else ""
            val resumen = "A ${formato(actividad.estadoPromedio)} E ${formato(actividad.energiaPromedio)} Δ$diferencia${formato(actividad.diferenciaMediaGeneral)} n=${actividad.totalRegistros}"
            dibujarTexto(canvas, resumen, inicioBarra + anchoDisponible + 7f, filaY + 3f, 7.5f, gris, false)
        }

        dibujarTexto(canvas, "Ánimo (azul, escala 1–8) · Energía (verde, escala 1–10)", x + 14f, y + alto - 12f, 8.5f, gris, false)
    }

    private fun crearConclusionesGenerales(
        registros: List<MoodRecord>,
        estadisticas: EmotionalStats
    ): List<String> {
        if (registros.isEmpty()) {
            return listOf("No se pueden obtener conclusiones porque no hay registros para este mes.")
        }

        return listOf(
            "Se registraron ${estadisticas.totalRegistros} días durante el periodo. El promedio de ánimo fue ${formato(estadisticas.promedio)} de 8.",
            "La desviación estándar fue ${formato(estadisticas.desviacion)}. Este valor resume cuánto se dispersan los estados registrados alrededor del promedio.",
            "La moda fue ${nombreEstado(estadisticas.moda)}. Este es el estado más repetido dentro de los registros del mes."
        )
    }

    private fun dibujarTituloSeccion(canvas: Canvas, titulo: String, y: Float) {
        dibujarTexto(canvas, titulo, MARGIN, y, 14f, azul, true)
        val linea = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = grisClaro
            strokeWidth = 1.2f
        }
        canvas.drawLine(MARGIN, y + 7f, PAGE_WIDTH - MARGIN, y + 7f, linea)
    }

    private fun dibujarTextoEnvuelto(
        canvas: Canvas,
        contenido: String,
        x: Float,
        yInicial: Float,
        ancho: Float,
        tamano: Float,
        color: Int
    ): Float {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = tamano
            this.color = color
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        var y = yInicial
        contenido.split('\n').forEach { parrafo ->
            var linea = ""
            parrafo.split(Regex("\\s+")).filter { it.isNotBlank() }.forEach { palabra ->
                val candidata = if (linea.isEmpty()) palabra else "$linea $palabra"
                if (paint.measureText(candidata) > ancho && linea.isNotEmpty()) {
                    canvas.drawText(linea, x, y, paint)
                    y += tamano + 4f
                    linea = palabra
                } else {
                    linea = candidata
                }
            }
            if (linea.isNotEmpty()) {
                canvas.drawText(linea, x, y, paint)
                y += tamano + 4f
            }
        }
        return y
    }

    private fun dibujarTexto(
        canvas: Canvas,
        contenido: String,
        x: Float,
        y: Float,
        tamano: Float,
        color: Int,
        negrita: Boolean
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = tamano
            this.color = color
            typeface = Typeface.create(
                Typeface.DEFAULT,
                if (negrita) Typeface.BOLD else Typeface.NORMAL
            )
        }
        canvas.drawText(contenido, x, y, paint)
    }

    private fun dibujarPie(canvas: Canvas, pagina: Int, esDemostracion: Boolean) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = grisClaro
            strokeWidth = 1f
        }
        canvas.drawLine(MARGIN, 807f, PAGE_WIDTH - MARGIN, 807f, paint)
        val nota = if (esDemostracion) {
            "Datos sintéticos de demostración · No corresponden a registros personales"
        } else {
            "Generado localmente por MoodWave · Los datos pertenecen al usuario"
        }
        dibujarTexto(canvas, nota, MARGIN, 824f, 8f, gris, false)
        dibujarTexto(canvas, "$pagina / 3", PAGE_WIDTH - MARGIN - 28f, 824f, 8f, gris, false)
    }

    private fun filtrarMes(
        registros: List<MoodRecord>,
        fechaReferencia: String
    ): List<MoodRecord> {
        val formato = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            isLenient = false
        }
        val fechaBase = runCatching { formato.parse(fechaReferencia) }.getOrNull()
            ?: Date()
        val calendarioBase = Calendar.getInstance().apply { time = fechaBase }
        val anio = calendarioBase.get(Calendar.YEAR)
        val mes = calendarioBase.get(Calendar.MONTH)

        return registros.filter { registro ->
            val fecha = runCatching { formato.parse(registro.fecha) }.getOrNull()
                ?: return@filter false
            val calendario = Calendar.getInstance().apply { time = fecha }
            calendario.get(Calendar.YEAR) == anio &&
                    calendario.get(Calendar.MONTH) == mes &&
                    registro.estado in 1..8 &&
                    registro.energia in 1..10
        }.distinctBy { it.fecha }.sortedBy { it.fecha }
    }

    private fun nombreMes(fecha: String): String {
        val formatoEntrada = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            isLenient = false
        }
        val fechaParseada = runCatching { formatoEntrada.parse(fecha) }.getOrNull() ?: Date()
        val nombre = SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("es-MX")).format(fechaParseada)
        return nombre.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.forLanguageTag("es-MX")) else it.toString()
        }
    }

    private fun formatearFecha(fecha: String): String {
        val entrada = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            isLenient = false
        }
        val salida = SimpleDateFormat("dd/MM", Locale.forLanguageTag("es-MX"))
        return runCatching { salida.format(entrada.parse(fecha)!!) }.getOrDefault(fecha)
    }

    private fun fechaActual(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun formato(valor: Double): String =
        String.format(Locale.US, "%.2f", valor)

    private fun nombreEstado(valor: Int): String = when (valor) {
        1 -> "Enojado"
        2 -> "Triste"
        3 -> "Desanimado"
        4 -> "Preocupado"
        5 -> "Neutral"
        6 -> "Bien"
        7 -> "Feliz"
        8 -> "Excelente"
        else -> "Sin datos"
    }
}
