package com.example.moodwave.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodwave.data.local.RegistroEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun HomeScreen(
    registroHoy: RegistroEntity?,
    registros: List<RegistroEntity>,
    onNewRecord: () -> Unit,
    onDiary: () -> Unit,
    onCalendar: () -> Unit,
    onStatistics: () -> Unit,
    onSettings: () -> Unit,
    onTimelineRecordClick: (RegistroEntity) -> Unit
) {

    val azulPrincipal =
        Color(0xFF0B5C8E)

    val azulClaro =
        Color(0xFFE3F4FC)

    val emoji =
        if (registroHoy != null) {
            obtenerEmojiInicio(
                registroHoy.estadoValor
            )
        } else {
            "🙂"
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(azulClaro)
            .verticalScroll(
                rememberScrollState()
            )
            .padding(24.dp),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Spacer(
            modifier = Modifier.height(45.dp)
        )

        Text(
            text = "¡Hola!",
            modifier = Modifier.fillMaxWidth(),
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = azulPrincipal
        )

        Text(
            text = "¿Cómo te sientes hoy?",
            modifier = Modifier.fillMaxWidth(),
            fontSize = 18.sp,
            color = Color(0xFF506070)
        )

        Spacer(
            modifier = Modifier.height(28.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape =
                RoundedCornerShape(22.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        Color.White
                )
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text = emoji,
                    fontSize = 48.sp
                )

                Spacer(
                    modifier =
                        Modifier.width(16.dp)
                )

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text = "Estado de hoy",
                        fontSize = 15.sp,
                        color = Color.Gray
                    )

                    if (registroHoy != null) {

                        Text(
                            text =
                                registroHoy.estadoNombre,
                            fontSize = 22.sp,
                            fontWeight =
                                FontWeight.Bold
                        )

                        Spacer(
                            modifier =
                                Modifier.height(4.dp)
                        )

                        Text(
                            text =
                                "Energía: ${registroHoy.energia}/10 ⚡",
                            fontSize = 15.sp,
                            color =
                                Color(0xFF506070)
                        )

                    } else {

                        Text(
                            text =
                                "Aún no has registrado tu día",
                            fontSize = 18.sp,
                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Button(
            onClick = onNewRecord,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            shape =
                RoundedCornerShape(28.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        azulPrincipal
                )
        ) {

            Text(
                text =
                    if (registroHoy == null) {
                        "+  Nuevo registro"
                    } else {
                        "Editar registro de hoy"
                    },
                fontSize = 18.sp
            )
        }

        Spacer(
            modifier = Modifier.height(28.dp)
        )

        TimelineEmocional(
            registros = registros,
            onRegistroClick =
                onTimelineRecordClick
        )

        Spacer(
            modifier = Modifier.height(28.dp)
        )

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(16.dp)
        ) {

            HomeOption(
                modifier =
                    Modifier.weight(1f),
                icon = "📅",
                title = "Calendario",
                onClick = onCalendar
            )

            HomeOption(
                modifier =
                    Modifier.weight(1f),
                icon = "📘",
                title = "Diario",
                onClick = onDiary
            )
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(16.dp)
        ) {

            HomeOption(
                modifier =
                    Modifier.weight(1f),
                icon = "📊",
                title = "Estadísticas",
                onClick = onStatistics
            )

            HomeOption(
                modifier =
                    Modifier.weight(1f),
                icon = "⚙️",
                title = "Configuración",
                onClick = onSettings
            )
        }

        Spacer(
            modifier = Modifier.height(30.dp)
        )
    }
}

@Composable
private fun TimelineEmocional(
    registros: List<RegistroEntity>,
    onRegistroClick:
        (RegistroEntity) -> Unit
) {

    val azulPrincipal =
        Color(0xFF0B5C8E)

    val dias =
        obtenerUltimosSieteDias(
            registros
        )

    Column(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        Text(
            text = "Últimos 7 días",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        Text(
            text =
                "Tu línea de tiempo emocional",
            fontSize = 14.sp,
            color = Color.Gray
        )

        Spacer(
            modifier = Modifier.height(14.dp)
        )

        Card(
            modifier =
                Modifier.fillMaxWidth(),
            shape =
                RoundedCornerShape(22.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        Color.White
                )
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement =
                    Arrangement.spacedBy(4.dp)
            ) {

                dias.forEach { dia ->

                    val tieneRegistro =
                        dia.registro != null

                    Card(
                        onClick = {

                            dia.registro?.let {
                                    registro ->

                                onRegistroClick(
                                    registro
                                )
                            }
                        },
                        enabled =
                            tieneRegistro,
                        modifier =
                            Modifier.weight(1f),
                        shape =
                            RoundedCornerShape(
                                14.dp
                            ),
                        colors =
                            CardDefaults
                                .cardColors(
                                    containerColor =
                                        if (
                                            tieneRegistro
                                        ) {
                                            Color(
                                                0xFFE3F4FC
                                            )
                                        } else {
                                            Color(
                                                0xFFF5F5F5
                                            )
                                        },
                                    disabledContainerColor =
                                        Color(
                                            0xFFF5F5F5
                                        )
                                )
                    ) {

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    vertical =
                                        10.dp,
                                    horizontal =
                                        2.dp
                                ),
                            horizontalAlignment =
                                Alignment
                                    .CenterHorizontally
                        ) {

                            Text(
                                text =
                                    dia.nombreDia,
                                fontSize = 11.sp,
                                fontWeight =
                                    FontWeight.Bold,
                                color =
                                    if (
                                        tieneRegistro
                                    ) {
                                        azulPrincipal
                                    } else {
                                        Color.Gray
                                    },
                                textAlign =
                                    TextAlign.Center
                            )

                            Text(
                                text =
                                    dia.numeroDia,
                                fontSize = 13.sp,
                                color =
                                    Color.Gray
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(
                                        5.dp
                                    )
                            )

                            Text(
                                text =
                                    dia.registro
                                        ?.let {
                                            obtenerEmojiInicio(
                                                it.estadoValor
                                            )
                                        }
                                        ?: "—",
                                fontSize =
                                    if (
                                        tieneRegistro
                                    ) {
                                        24.sp
                                    } else {
                                        18.sp
                                    }
                            )
                        }
                    }
                }
            }
        }

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text =
                "Toca un día con registro para ver sus detalles.",
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun HomeOption(
    modifier: Modifier = Modifier,
    icon: String,
    title: String,
    onClick: () -> Unit
) {

    Card(
        onClick = onClick,
        modifier =
            modifier.height(140.dp),
        shape =
            RoundedCornerShape(22.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color.White
            )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.Center
        ) {

            Text(
                text = icon,
                fontSize = 38.sp
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight =
                    FontWeight.Medium,
                textAlign =
                    TextAlign.Center
            )
        }
    }
}

private data class DiaTimeline(
    val nombreDia: String,
    val numeroDia: String,
    val registro: RegistroEntity?
)

private fun obtenerUltimosSieteDias(
    registros: List<RegistroEntity>
): List<DiaTimeline> {

    val resultado =
        mutableListOf<DiaTimeline>()

    val calendario =
        Calendar.getInstance()

    calendario.add(
        Calendar.DAY_OF_MONTH,
        -6
    )

    val formatoFecha =
        SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        )

    val formatoDia =
        SimpleDateFormat(
            "EEE",
            Locale("es", "MX")
        )

    val formatoNumero =
        SimpleDateFormat(
            "d",
            Locale("es", "MX")
        )

    repeat(7) {

        val fecha =
            formatoFecha.format(
                calendario.time
            )

        val nombreDia =
            formatoDia
                .format(
                    calendario.time
                )
                .replace(".", "")
                .replaceFirstChar {
                    it.uppercase()
                }

        val numeroDia =
            formatoNumero.format(
                calendario.time
            )

        val registro =
            registros.find {
                it.fecha == fecha
            }

        resultado.add(
            DiaTimeline(
                nombreDia = nombreDia,
                numeroDia = numeroDia,
                registro = registro
            )
        )

        calendario.add(
            Calendar.DAY_OF_MONTH,
            1
        )
    }

    return resultado
}

private fun obtenerEmojiInicio(
    estadoValor: Int
): String {

    return when (estadoValor) {

        1 -> "😡"
        2 -> "😢"
        3 -> "😞"
        4 -> "😕"
        5 -> "😐"
        6 -> "🙂"
        7 -> "😄"
        8 -> "🤩"

        else -> "🙂"
    }
}