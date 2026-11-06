package com.example.moodwave.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodwave.analytics.ActivityStats
import com.example.moodwave.analytics.EmotionalStats
import com.example.moodwave.analytics.MoodAnalytics
import com.example.moodwave.reports.MonthlyReportPdfGenerator
import com.example.moodwave.analytics.MoodRecord
import com.example.moodwave.analytics.MoodPattern
import com.example.moodwave.analytics.MoodPatternAnalysis
import com.example.moodwave.analytics.MoodPatternDiscovery
import com.example.moodwave.analytics.RegistroAnalyticsMapper
import com.example.moodwave.analytics.SyntheticDataGenerator
import com.example.moodwave.analytics.TrendPoint
import com.example.moodwave.data.local.RegistroEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun StatisticsScreen(
    registros: List<RegistroEntity>,
    onBack: () -> Unit
) {

    val azulPrincipal = Color(0xFF0B5C8E)
    val azulClaro = Color(0xFFE3F4FC)

    var periodoSeleccionado by remember {
        mutableStateOf(7)
    }

    var modoDemostracion by remember {
        mutableStateOf(false)
    }

    val registrosReales = remember(registros) {
        RegistroAnalyticsMapper.convertirLista(registros)
    }

    val registrosDemostracion = remember {
        SyntheticDataGenerator.generar90Dias()
    }

    val registrosAnalisis = if (modoDemostracion) {
        registrosDemostracion
    } else {
        registrosReales
    }

    val fechaReferencia = obtenerFechaActual()
    val contexto = LocalContext.current
    var mensajeReporte by remember { mutableStateOf<String?>(null) }

    val reportePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri == null) {
            mensajeReporte = "No se guardó el reporte."
        } else {
            val resultado = runCatching {
                val contenidoPdf = MonthlyReportPdfGenerator.generar(
                    registros = registrosAnalisis,
                    fechaReferencia = fechaReferencia,
                    esDemostracion = modoDemostracion
                )

                contexto.contentResolver.openOutputStream(uri)?.use { salida ->
                    salida.write(contenidoPdf)
                } ?: throw IllegalStateException("No se pudo abrir el archivo de destino.")
            }

            mensajeReporte = if (resultado.isSuccess) {
                "Reporte mensual guardado correctamente."
            } else {
                "No se pudo generar el reporte. Inténtalo nuevamente."
            }
        }
    }

    val estadisticasSemana = remember(registrosAnalisis, fechaReferencia) {
        MoodAnalytics.calcularEstadisticasSemana(
            registrosAnalisis,
            fechaReferencia
        )
    }

    val estadisticasMes = remember(registrosAnalisis, fechaReferencia) {
        MoodAnalytics.calcularEstadisticasMes(
            registrosAnalisis,
            fechaReferencia
        )
    }

    val analisisPatrones = remember(registrosAnalisis) {
        MoodPatternDiscovery.analizar(registrosAnalisis)
    }

    val puntosTendencia = remember(
        registrosAnalisis,
        fechaReferencia,
        periodoSeleccionado
    ) {
        when (periodoSeleccionado) {
            7 -> MoodAnalytics.calcularTendencia7Dias(
                registrosAnalisis,
                fechaReferencia
            )

            30 -> MoodAnalytics.calcularTendencia30Dias(
                registrosAnalisis,
                fechaReferencia
            )

            else -> MoodAnalytics.calcularTendencia90Dias(
                registrosAnalisis,
                fechaReferencia
            )
        }
    }

    val actividadStats = remember(registrosAnalisis) {
        MoodAnalytics.calcularActividadVsAnimo(registrosAnalisis)
    }

    val correlaciones = remember(registrosAnalisis) {
        MoodAnalytics.calcularCorrelacionesActividad(registrosAnalisis)
    }

    val diasTotales = remember(registrosAnalisis) {
        registrosAnalisis.map { it.fecha }.distinct().size
    }

    val mediaGeneralEnergia = remember(registrosAnalisis) {
        registrosAnalisis.map { it.energia }.average().takeIf {
            it.isFinite()
        } ?: 0.0
    }

    val actividadesAnalizadas = remember(
        actividadStats,
        correlaciones,
        mediaGeneralEnergia
    ) {

        actividadStats.mapNotNull { actividad ->

            val correlacion = correlaciones.firstOrNull {
                it.actividad.equals(actividad.actividad, ignoreCase = true)
            } ?: return@mapNotNull null

            ActividadInsightUi(
                actividad = actividad.actividad,
                estadoPromedio = actividad.estadoPromedio,
                energiaPromedio = actividad.energiaPromedio,
                diferenciaEstado = actividad.diferenciaMediaGeneral,
                diferenciaEnergia = actividad.energiaPromedio - mediaGeneralEnergia,
                totalConActividad = actividad.totalRegistros,
                totalSinActividad = correlacion.totalSinActividad,
                datosSuficientes = actividad.datosSuficientes &&
                        correlacion.datosSuficientes,
                tamanoEfecto = actividad.tamanoEfecto,
                nivelEfecto = actividad.nivelEfecto,
                coeficiente = correlacion.coeficiente,
                direccion = correlacion.direccion,
                nivelRelacion = correlacion.nivelRelacion
            )
        }.sortedWith(
            compareByDescending<ActividadInsightUi> { it.datosSuficientes }
                .thenByDescending { abs(it.tamanoEfecto) }
                .thenByDescending { it.totalConActividad }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(azulClaro)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {

        Spacer(Modifier.height(30.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {

            TextButton(onClick = onBack) {
                Text(
                    text = "←",
                    fontSize = 28.sp,
                    color = azulPrincipal
                )
            }

            Text(
                text = "Estadísticas",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = azulPrincipal
            )
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = "Descubre tus patrones",
            fontSize = 17.sp,
            color = Color(0xFF506070)
        )

        Spacer(Modifier.height(14.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = if (modoDemostracion) {
                        "Modo de demostración activado"
                    } else {
                        "¿Quieres explorar las estadísticas?"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF183B56)
                )

                Spacer(Modifier.height(5.dp))

                Text(
                    text = if (modoDemostracion) {
                        "Estás viendo 90 registros simulados con diferentes patrones de ánimo, energía y actividades. Estos datos no se guardan en tu diario ni modifican tus registros personales."
                    } else {
                        "Puedes cargar una demostración con 90 días de datos simulados para explorar las gráficas y el descubrimiento de patrones sin modificar tu diario."
                    },
                    fontSize = 13.sp,
                    color = Color(0xFF59636D)
                )

                Spacer(Modifier.height(8.dp))

                TextButton(
                    onClick = {
                        modoDemostracion = !modoDemostracion
                        periodoSeleccionado = 90
                    }
                ) {
                    Text(
                        text = if (modoDemostracion) {
                            "Volver a mis registros"
                        } else {
                            "Ver demostración de 90 días"
                        },
                        color = Color(0xFF0B5C8E),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Reporte mensual en PDF",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF183B56)
                )

                Spacer(Modifier.height(5.dp))

                Text(
                    text = if (modoDemostracion) {
                        "Puedes guardar un reporte de demostración. El documento indicará claramente que utiliza datos sintéticos."
                    } else {
                        "Guarda un resumen del mes actual con estadísticas, gráficas, actividades, correlaciones y patrones disponibles."
                    },
                    fontSize = 13.sp,
                    color = Color(0xFF59636D)
                )

                Spacer(Modifier.height(8.dp))

                TextButton(
                    onClick = {
                        mensajeReporte = null
                        val nombreArchivo = if (modoDemostracion) {
                            "MoodWave_Reporte_Mensual_DEMO.pdf"
                        } else {
                            "MoodWave_Reporte_Mensual.pdf"
                        }
                        reportePdfLauncher.launch(nombreArchivo)
                    }
                ) {
                    Text(
                        text = "Exportar reporte PDF",
                        color = Color(0xFF0B5C8E),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                mensajeReporte?.let { mensaje ->
                    Text(
                        text = mensaje,
                        fontSize = 12.sp,
                        color = if (mensaje.startsWith("Reporte mensual guardado")) {
                            Color(0xFF1E806D)
                        } else {
                            Color(0xFF68737D)
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(22.dp))

        EstadisticasPeriodoCard(
            titulo = "Resumen de esta semana",
            estadisticas = estadisticasSemana
        )

        Spacer(Modifier.height(16.dp))

        EstadisticasPeriodoCard(
            titulo = "Resumen de este mes",
            estadisticas = estadisticasMes
        )

        Spacer(Modifier.height(22.dp))

        TendenciaCard(
            puntos = puntosTendencia,
            periodoSeleccionado = periodoSeleccionado,
            onPeriodoChange = {
                periodoSeleccionado = it
            }
        )

        Spacer(Modifier.height(22.dp))

        ActividadBarrasCard(
            actividades = actividadStats,
            diasTotales = diasTotales
        )

        Spacer(Modifier.height(22.dp))

        DispersionAnimoEnergiaCard(
            registros = registrosAnalisis,
            diasTotales = diasTotales
        )

        Spacer(Modifier.height(22.dp))

        MoodPatternDiscoveryCard(
            analisis = analisisPatrones
        )

        Spacer(Modifier.height(22.dp))

        ActividadesAnalisisCard(
            actividades = actividadesAnalizadas,
            diasTotales = diasTotales
        )

        Spacer(Modifier.height(22.dp))

        InterpretacionesInteligentesCard(
            actividades = actividadesAnalizadas,
            diasTotales = diasTotales
        )

        Spacer(Modifier.height(18.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {

            Text(
                text = "MoodWave no realiza diagnósticos médicos ni sustituye la atención profesional. Los resultados describen tus registros y no demuestran que una actividad cause un cambio en tu estado de ánimo.",
                modifier = Modifier.padding(18.dp),
                fontSize = 13.sp,
                color = Color(0xFF59636D),
                textAlign = TextAlign.Start
            )
        }

        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun EstadisticasPeriodoCard(
    titulo: String,
    estadisticas: EmotionalStats
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {

        Column(
            modifier = Modifier.padding(20.dp)
        ) {

            Text(
                text = titulo,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0B5C8E)
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Estadísticas de ánimo · ${estadisticas.totalRegistros} registros",
                fontSize = 13.sp,
                color = Color.Gray
            )

            if (estadisticas.totalRegistros == 0) {

                Spacer(Modifier.height(18.dp))

                Text(
                    text = "Aún no hay datos suficientes para este periodo.",
                    fontSize = 14.sp,
                    color = Color.Gray
                )

            } else {

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    MetricaCard(
                        modifier = Modifier.weight(1f),
                        titulo = "Promedio",
                        valor = formatoNumero(estadisticas.promedio)
                    )

                    MetricaCard(
                        modifier = Modifier.weight(1f),
                        titulo = "Mediana",
                        valor = formatoNumero(estadisticas.mediana)
                    )
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    MetricaCard(
                        modifier = Modifier.weight(1f),
                        titulo = "Moda",
                        valor = nombreEstado(estadisticas.moda)
                    )

                    MetricaCard(
                        modifier = Modifier.weight(1f),
                        titulo = "Desviación",
                        valor = formatoNumero(estadisticas.desviacion)
                    )
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    MetricaCard(
                        modifier = Modifier.weight(1f),
                        titulo = "Racha registrada",
                        valor = "${estadisticas.rachaActual} días"
                    )

                    MetricaCard(
                        modifier = Modifier.weight(1f),
                        titulo = "Muestra",
                        valor = "${estadisticas.totalRegistros}"
                    )
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "La desviación muestra cuánto varían tus estados de ánimo respecto al promedio.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun MetricaCard(
    modifier: Modifier = Modifier,
    titulo: String,
    valor: String
) {

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF4F9FC)
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {

            Text(
                text = titulo,
                fontSize = 12.sp,
                color = Color.Gray
            )

            Spacer(Modifier.height(5.dp))

            Text(
                text = valor,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF183B56)
            )
        }
    }
}

@Composable
private fun TendenciaCard(
    puntos: List<TrendPoint>,
    periodoSeleccionado: Int,
    onPeriodoChange: (Int) -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {

        Column(
            modifier = Modifier.padding(20.dp)
        ) {

            Text(
                text = "Evolución emocional",
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0B5C8E)
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "¿Cómo han cambiado tu ánimo y energía?",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                listOf(7, 30, 90).forEach { dias ->

                    FilterChip(
                        selected = periodoSeleccionado == dias,
                        onClick = {
                            onPeriodoChange(dias)
                        },
                        label = {
                            Text("$dias días")
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (puntos.isEmpty()) {

                MensajeSinDatos(
                    texto = "Aún no hay registros en este periodo. Cuando registres tus emociones, aquí aparecerá tu evolución."
                )

            } else {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    LeyendaTendencia(
                        Color(0xFF0B5C8E),
                        "Ánimo"
                    )

                    LeyendaTendencia(
                        Color(0xFF1E9E83),
                        "Media móvil"
                    )

                    LeyendaTendencia(
                        Color(0xFFE5A13A),
                        "Energía"
                    )
                }

                Spacer(Modifier.height(12.dp))

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                ) {

                    val left = 8.dp.toPx()
                    val right = 8.dp.toPx()
                    val top = 12.dp.toPx()
                    val bottom = 12.dp.toPx()

                    val width = size.width - left - right
                    val height = size.height - top - bottom

                    for (i in 0..5) {

                        val y = top + height * (1f - i / 5f)

                        drawLine(
                            color = Color(0xFFE7EDF2),
                            start = Offset(left, y),
                            end = Offset(left + width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    fun x(indice: Int): Float {

                        return if (puntos.size == 1) {
                            left + width / 2f
                        } else {
                            left + width * indice /
                                    (puntos.size - 1).toFloat()
                        }
                    }

                    fun y(valor: Double): Float {

                        val normalizado = (valor / 10.0)
                            .coerceIn(0.0, 1.0)
                            .toFloat()

                        return top + height * (1f - normalizado)
                    }

                    fun dibujarSerie(
                        color: Color,
                        valor: (TrendPoint) -> Double
                    ) {

                        val path = Path()

                        puntos.forEachIndexed { index, punto ->

                            val px = x(index)
                            val py = y(valor(punto))

                            if (index == 0) {
                                path.moveTo(px, py)
                            } else {
                                path.lineTo(px, py)
                            }
                        }

                        drawPath(
                            path = path,
                            color = color,
                            style = Stroke(
                                width = 3.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        )

                        puntos.forEachIndexed { index, punto ->

                            drawCircle(
                                color = color,
                                radius = 3.dp.toPx(),
                                center = Offset(
                                    x(index),
                                    y(valor(punto))
                                )
                            )
                        }
                    }

                    dibujarSerie(Color(0xFFE5A13A)) {
                        it.energia
                    }

                    dibujarSerie(Color(0xFF1E9E83)) {
                        it.mediaMovilEstado
                    }

                    dibujarSerie(Color(0xFF0B5C8E)) {
                        it.estado
                    }
                }

                Spacer(Modifier.height(8.dp))

                EtiquetasFechasTendencia(puntos)

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "${puntos.size} registros en los últimos $periodoSeleccionado días.",
                    fontSize = 13.sp,
                    color = Color.Gray
                )

                Text(
                    text = "La media móvil representa el promedio del ánimo de los registros disponibles en los últimos 7 días.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun EtiquetasFechasTendencia(
    puntos: List<TrendPoint>
) {

    val indices = when {
        puntos.isEmpty() -> emptyList()
        puntos.size == 1 -> listOf(0)
        puntos.size == 2 -> listOf(0, 1)
        else -> listOf(
            0,
            puntos.size / 2,
            puntos.lastIndex
        ).distinct()
    }

    Row(
        modifier = Modifier.fillMaxWidth()
    ) {

        indices.forEachIndexed { posicion, indice ->

            val alineacion = when (posicion) {
                0 -> TextAlign.Start
                indices.lastIndex -> TextAlign.End
                else -> TextAlign.Center
            }

            Text(
                text = formatearFechaCorta(puntos[indice].fecha),
                fontSize = 11.sp,
                color = Color.Gray,
                textAlign = alineacion,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ActividadBarrasCard(
    actividades: List<ActivityStats>,
    diasTotales: Int
) {

    val actividadesValidas = actividades
        .filter { it.datosSuficientes }
        .sortedByDescending { it.estadoPromedio }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {

        Column(
            modifier = Modifier.padding(20.dp)
        ) {

            Text(
                text = "Promedios por actividad",
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0B5C8E)
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "¿Con qué actividades registras mayores promedios de ánimo y energía?",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(Modifier.height(14.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                LeyendaTendencia(
                    Color(0xFF0B5C8E),
                    "Ánimo"
                )

                LeyendaTendencia(
                    Color(0xFFE5A13A),
                    "Energía"
                )
            }

            Spacer(Modifier.height(16.dp))

            if (diasTotales < 14 || actividadesValidas.isEmpty()) {

                MensajeSinDatos(
                    texto = "Aún no hay datos suficientes. Se necesitan al menos 14 días registrados y 5 registros para cada actividad."
                )

            } else {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Bottom
                ) {

                    actividadesValidas.forEach { actividad ->

                        Column(
                            modifier = Modifier.width(86.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            Row(
                                modifier = Modifier.height(126.dp),
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {

                                Box(
                                    modifier = Modifier
                                        .width(22.dp)
                                        .height(
                                            (actividad.estadoPromedio / 8.0 * 112.0)
                                                .coerceIn(3.0, 112.0).dp
                                        )
                                        .clip(
                                            RoundedCornerShape(
                                                topStart = 6.dp,
                                                topEnd = 6.dp
                                            )
                                        )
                                        .background(Color(0xFF0B5C8E))
                                )

                                Box(
                                    modifier = Modifier
                                        .width(22.dp)
                                        .height(
                                            (actividad.energiaPromedio / 10.0 * 112.0)
                                                .coerceIn(3.0, 112.0).dp
                                        )
                                        .clip(
                                            RoundedCornerShape(
                                                topStart = 6.dp,
                                                topEnd = 6.dp
                                            )
                                        )
                                        .background(Color(0xFFE5A13A))
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = actividad.actividad,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF183B56),
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                minLines = 2
                            )

                            Text(
                                text = "n = ${actividad.totalRegistros}",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "Cada barra muestra el promedio de los registros que incluyen esa actividad. Los resultados son descriptivos.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun DispersionAnimoEnergiaCard(
    registros: List<MoodRecord>,
    diasTotales: Int
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {

        Column(
            modifier = Modifier.padding(20.dp)
        ) {

            Text(
                text = "Relación entre ánimo y energía",
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0B5C8E)
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "¿Cómo se distribuyen tus niveles de ánimo y energía?",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(Modifier.height(14.dp))

            if (diasTotales < 14) {

                MensajeSinDatos(
                    texto = "Aún no hay datos suficientes para mostrar esta distribución. Se necesitan al menos 14 días registrados."
                )

            } else {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {

                    Text(
                        text = "Ánimo ↑",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    Text(
                        text = "Energía →",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                ) {

                    val left = 18.dp.toPx()
                    val right = 12.dp.toPx()
                    val top = 12.dp.toPx()
                    val bottom = 12.dp.toPx()

                    val width = size.width - left - right
                    val height = size.height - top - bottom

                    for (i in 0..4) {

                        val fraction = i / 4f

                        val y = top + height * fraction
                        val x = left + width * fraction

                        drawLine(
                            color = Color(0xFFE7EDF2),
                            start = Offset(left, y),
                            end = Offset(left + width, y),
                            strokeWidth = 1.dp.toPx()
                        )

                        drawLine(
                            color = Color(0xFFE7EDF2),
                            start = Offset(x, top),
                            end = Offset(x, top + height),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    registros.forEach { registro ->

                        val x = left + width *
                                ((registro.energia - 1) / 9f)

                        val y = top + height *
                                (1f - ((registro.estado - 1) / 7f))

                        drawCircle(
                            color = Color(0xFF0B5C8E).copy(alpha = 0.72f),
                            radius = 5.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }
                }

                Text(
                    text = "${registros.size} registros · Cada punto representa un día registrado.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun MoodPatternDiscoveryCard(
    analisis: MoodPatternAnalysis
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {

        Column(
            modifier = Modifier.padding(20.dp)
        ) {

            Text(
                text = "Descubrimiento de patrones",
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0B5C8E)
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Agrupa días con niveles similares de ánimo y energía. También muestra las actividades y fechas que ayudan a entender cada grupo.",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(Modifier.height(14.dp))

            if (!analisis.listo) {

                MensajeSinDatos(
                    texto = analisis.mensaje
                )

                if (analisis.totalRegistros > 0) {
                    Text(
                        text = "Registros válidos disponibles: ${analisis.totalRegistros}.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

            } else {

                Text(
                    text = analisis.mensaje,
                    fontSize = 13.sp,
                    color = Color(0xFF505B65)
                )

                Spacer(Modifier.height(14.dp))

                if (analisis.evaluacionDisponible) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {

                        MetricaCard(
                            modifier = Modifier.weight(1f),
                            titulo = "Silhouette · K-means",
                            valor = formatoNumero(analisis.silhouetteScore)
                        )

                        MetricaCard(
                            modifier = Modifier.weight(1f),
                            titulo = "Método de referencia",
                            valor = formatoNumero(analisis.baselineScore)
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = analisis.comparacion,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF183B56)
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = "El índice silhouette compara qué tan cercanos son los registros dentro de cada grupo y qué tan separados están de los demás. Sus valores van de -1 a 1; un valor mayor suele indicar grupos mejor separados.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                } else {

                    Text(
                        text = analisis.comparacion,
                        fontSize = 13.sp,
                        color = Color(0xFF68737D)
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Patrones identificados",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF183B56)
                )

                Spacer(Modifier.height(10.dp))

                if (analisis.patrones.isEmpty()) {

                    MensajeSinDatos(
                        texto = "No se encontraron grupos para mostrar con los registros actuales."
                    )

                } else {

                    analisis.patrones.forEachIndexed { index, patron ->

                        MoodPatternItemCard(patron)

                        if (index != analisis.patrones.lastIndex) {
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "K-means agrupa los días según ánimo y energía. Las actividades se muestran como contexto de los grupos; estos resultados describen similitudes y no demuestran causas ni representan diagnósticos.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun MoodPatternItemCard(
    patron: MoodPattern
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF4F9FC)
        )
    ) {

        Column(
            modifier = Modifier.padding(15.dp)
        ) {

            Text(
                text = if (patron.titulo == "Resumen general") {
                    "Resumen general"
                } else {
                    "Patrón ${patron.id}"
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1E9E83)
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = patron.titulo,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF183B56)
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = patron.descripcion,
                fontSize = 13.sp,
                color = Color(0xFF505B65)
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                MetricaCard(
                    modifier = Modifier.weight(1f),
                    titulo = "Ánimo promedio",
                    valor = "${formatoNumero(patron.promedioAnimo)} / 8"
                )

                MetricaCard(
                    modifier = Modifier.weight(1f),
                    titulo = "Energía promedio",
                    valor = "${formatoNumero(patron.promedioEnergia)} / 10"
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Muestra: ${patron.totalRegistros} días",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF59636D)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Actividades más frecuentes",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF183B56)
            )

            if (patron.actividadesFrecuentes.isEmpty()) {
                Text(
                    text = "No hay actividades registradas en este grupo.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            } else {
                patron.actividadesFrecuentes.forEach { actividad ->
                    Text(
                        text = "${actividad.nombre}: ${actividad.totalRegistros} de ${patron.totalRegistros} días (${String.format(Locale.US, "%.1f", actividad.porcentaje)}%).",
                        fontSize = 12.sp,
                        color = Color(0xFF505B65)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Fechas incluidas",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF183B56)
            )

            val fechasVisibles = patron.fechas.take(5).joinToString(" · ") {
                formatearFechaCorta(it)
            }

            val fechasRestantes = patron.fechas.size - 5
            val textoFechas = if (fechasRestantes > 0) {
                "$fechasVisibles · y $fechasRestantes más"
            } else {
                fechasVisibles
            }

            Text(
                text = textoFechas.ifBlank { "Sin fechas disponibles" },
                fontSize = 12.sp,
                color = Color(0xFF59636D)
            )
        }
    }
}

@Composable
private fun ActividadesAnalisisCard(
    actividades: List<ActividadInsightUi>,
    diasTotales: Int
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {

        Column(
            modifier = Modifier.padding(20.dp)
        ) {

            Text(
                text = "Actividad y estado de ánimo",
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0B5C8E)
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Compara cómo se presentan tu ánimo y energía en los días con cada actividad.",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Método: correlación punto-biserial. Cada actividad se considera presente o ausente, y se compara con el estado de ánimo numérico. Este método describe una asociación, no una causa.",
                fontSize = 12.sp,
                color = Color(0xFF68737D)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Se analizan varias actividades, por lo que los resultados son exploratorios y deben interpretarse con cautela.",
                fontSize = 12.sp,
                color = Color(0xFF68737D)
            )

            Spacer(Modifier.height(16.dp))

            if (actividades.isEmpty()) {

                MensajeSinDatos(
                    texto = "Todavía no hay actividades registradas para analizar."
                )

            } else {

                actividades.forEachIndexed { index, actividad ->

                    ActividadInsightCard(actividad)

                    if (index != actividades.lastIndex) {
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }

            if (diasTotales < 14) {

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Para interpretar patrones se requieren al menos 14 días registrados.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun ActividadInsightCard(
    actividad: ActividadInsightUi
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F9FC)
        )
    ) {

        Column(
            modifier = Modifier.padding(15.dp)
        ) {

            Text(
                text = actividad.actividad,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF183B56)
            )

            Spacer(Modifier.height(10.dp))

            if (!actividad.datosSuficientes) {

                Text(
                    text = "Muestra: ${actividad.totalConActividad} registros con esta actividad y ${actividad.totalSinActividad} sin ella.",
                    fontSize = 13.sp,
                    color = Color(0xFF505B65)
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "Aún no hay datos suficientes para interpretar un patrón. Se requieren al menos 14 días y 5 registros en cada grupo.",
                    fontSize = 13.sp,
                    color = Color(0xFF707070)
                )

            } else {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    MetricaCard(
                        modifier = Modifier.weight(1f),
                        titulo = "Ánimo promedio",
                        valor = "${formatoNumero(actividad.estadoPromedio)} / 8"
                    )

                    MetricaCard(
                        modifier = Modifier.weight(1f),
                        titulo = "Energía promedio",
                        valor = "${formatoNumero(actividad.energiaPromedio)} / 10"
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    MetricaCard(
                        modifier = Modifier.weight(1f),
                        titulo = "Dif. ánimo",
                        valor = formatoConSigno(actividad.diferenciaEstado)
                    )

                    MetricaCard(
                        modifier = Modifier.weight(1f),
                        titulo = "Dif. energía",
                        valor = formatoConSigno(actividad.diferenciaEnergia)
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Muestra: n = ${actividad.totalConActividad} con actividad y n = ${actividad.totalSinActividad} sin actividad.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Se observa una relación ${actividad.direccion.lowercase()} ${actividad.nivelRelacion.lowercase()} entre esta actividad y el ánimo registrado.",
                    fontSize = 13.sp,
                    color = Color(0xFF404040)
                )

                Spacer(Modifier.height(5.dp))

                Text(
                    text = "Correlación r = ${formatoNumero(actividad.coeficiente)} · Tamaño de efecto d = ${formatoNumero(actividad.tamanoEfecto)} (${actividad.nivelEfecto.lowercase()}).",
                    fontSize = 12.sp,
                    color = Color(0xFF707070)
                )
            }
        }
    }
}

@Composable
private fun InterpretacionesInteligentesCard(
    actividades: List<ActividadInsightUi>,
    diasTotales: Int
) {
    val actividadesValidas = actividades.filter {
        it.datosSuficientes &&
                it.diferenciaEstado.isFinite() &&
                it.diferenciaEnergia.isFinite() &&
                it.estadoPromedio.isFinite() &&
                it.energiaPromedio.isFinite()
    }

    val mayorAnimo = actividadesValidas.maxByOrNull {
        it.diferenciaEstado
    }

    val menorAnimo = actividadesValidas.minByOrNull {
        it.diferenciaEstado
    }

    val mayorEnergia = actividadesValidas.maxByOrNull {
        it.diferenciaEnergia
    }

    val menorEnergia = actividadesValidas.minByOrNull {
        it.diferenciaEnergia
    }

    val actividadesPendientes = actividades.filterNot {
        it.datosSuficientes
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Interpretaciones automáticas",
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0B5C8E)
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Un resumen de lo que muestran tus registros al comparar cada actividad con tu promedio general.",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(Modifier.height(14.dp))

            when {
                diasTotales < 14 -> {
                    MensajeSinDatos(
                        texto = "Se necesitan al menos 14 días registrados para generar interpretaciones. Actualmente hay $diasTotales días."
                    )
                }

                actividadesValidas.isEmpty() -> {
                    MensajeSinDatos(
                        texto = "Todavía no hay actividades con una muestra suficiente. Se requieren al menos 5 registros con la actividad y 5 sin ella."
                    )
                }

                else -> {
                    Text(
                        text = "Estado de ánimo",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF183B56)
                    )

                    Spacer(Modifier.height(8.dp))

                    mayorAnimo?.let {
                        InterpretacionActividadItem(
                            titulo = "Mayor promedio relativo",
                            actividad = it,
                            promedio = it.estadoPromedio,
                            diferencia = it.diferenciaEstado,
                            escala = "/ 8"
                        )
                    }

                    if (menorAnimo != null &&
                        menorAnimo.actividad != mayorAnimo?.actividad
                    ) {
                        Spacer(Modifier.height(8.dp))

                        InterpretacionActividadItem(
                            titulo = "Menor promedio relativo",
                            actividad = menorAnimo,
                            promedio = menorAnimo.estadoPromedio,
                            diferencia = menorAnimo.diferenciaEstado,
                            escala = "/ 8"
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = "Nivel de energía",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF183B56)
                    )

                    Spacer(Modifier.height(8.dp))

                    mayorEnergia?.let {
                        InterpretacionActividadItem(
                            titulo = "Mayor promedio relativo",
                            actividad = it,
                            promedio = it.energiaPromedio,
                            diferencia = it.diferenciaEnergia,
                            escala = "/ 10"
                        )
                    }

                    if (menorEnergia != null &&
                        menorEnergia.actividad != mayorEnergia?.actividad
                    ) {
                        Spacer(Modifier.height(8.dp))

                        InterpretacionActividadItem(
                            titulo = "Menor promedio relativo",
                            actividad = menorEnergia,
                            promedio = menorEnergia.energiaPromedio,
                            diferencia = menorEnergia.diferenciaEnergia,
                            escala = "/ 10"
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "Siguiente paso",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF183B56)
                    )

                    Spacer(Modifier.height(5.dp))

                    Text(
                        text = "Continúa registrando tus actividades durante las próximas semanas para observar si estas diferencias se mantienen o cambian.",
                        fontSize = 13.sp,
                        color = Color(0xFF505B65)
                    )
                }
            }

            if (actividadesPendientes.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Comparaciones pendientes",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF183B56)
                )

                Spacer(Modifier.height(5.dp))

                actividadesPendientes.take(4).forEach { actividad ->
                    Text(
                        text = "${actividad.actividad}: ${actividad.totalConActividad} registros con la actividad y ${actividad.totalSinActividad} sin ella.",
                        fontSize = 12.sp,
                        color = Color(0xFF68737D)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Estas interpretaciones describen diferencias observadas en tus registros. No demuestran que una actividad cause cambios en el ánimo o la energía. La muestra mínima tampoco equivale a una prueba de significancia estadística.",
                fontSize = 12.sp,
                color = Color(0xFF68737D)
            )
        }
    }
}

@Composable
private fun InterpretacionActividadItem(
    titulo: String,
    actividad: ActividadInsightUi,
    promedio: Double,
    diferencia: Double,
    escala: String
) {
    val comparacion = when {
        diferencia > 0.005 -> "por encima de la media general"
        diferencia < -0.005 -> "por debajo de la media general"
        else -> "prácticamente igual a la media general"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF4F9FC))
            .padding(12.dp)
    ) {
        Text(
            text = titulo,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1E9E83)
        )

        Spacer(Modifier.height(3.dp))

        Text(
            text = actividad.actividad,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF183B56)
        )

        Spacer(Modifier.height(5.dp))

        Text(
            text = "Promedio: ${formatoNumero(promedio)} $escala",
            fontSize = 13.sp,
            color = Color(0xFF505B65)
        )

        Text(
            text = "Diferencia: ${formatoConSigno(diferencia)} puntos $comparacion.",
            fontSize = 13.sp,
            color = Color(0xFF505B65)
        )

        Text(
            text = "Muestra: ${actividad.totalConActividad} días con la actividad y ${actividad.totalSinActividad} sin ella.",
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

@Composable
private fun MensajeSinDatos(
    texto: String
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Aún no hay datos suficientes",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.DarkGray,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(7.dp))

        Text(
            text = texto,
            fontSize = 13.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LeyendaTendencia(
    color: Color,
    texto: String
) {

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(color)
        )

        Spacer(Modifier.width(6.dp))

        Text(
            text = texto,
            fontSize = 12.sp,
            color = Color(0xFF606060)
        )
    }
}

private data class ActividadInsightUi(
    val actividad: String,
    val estadoPromedio: Double,
    val energiaPromedio: Double,
    val diferenciaEstado: Double,
    val diferenciaEnergia: Double,
    val totalConActividad: Int,
    val totalSinActividad: Int,
    val datosSuficientes: Boolean,
    val tamanoEfecto: Double,
    val nivelEfecto: String,
    val coeficiente: Double,
    val direccion: String,
    val nivelRelacion: String
)

private fun obtenerFechaActual(): String {

    return SimpleDateFormat(
        "yyyy-MM-dd",
        Locale.US
    ).format(Date())
}

private fun formatearFechaCorta(
    fecha: String
): String {

    return try {

        val entrada = SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        ).apply {
            isLenient = false
        }

        val salida = SimpleDateFormat(
            "dd/MM",
            Locale("es", "MX")
        )

        salida.format(entrada.parse(fecha)!!)

    } catch (_: Exception) {

        fecha
    }
}

private fun nombreEstado(
    valor: Int
): String {

    return when (valor) {
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

private fun formatoNumero(
    valor: Double
): String {

    return String.format(
        Locale.US,
        "%.2f",
        valor
    )
}

private fun formatoConSigno(
    valor: Double
): String {

    val signo = if (valor > 0) "+" else ""

    return signo + String.format(
        Locale.US,
        "%.2f",
        valor
    )
}
