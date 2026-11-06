package com.example.moodwave.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
fun CalendarScreen(
    registros: List<RegistroEntity>,
    onBack: () -> Unit,
    onRegistroClick: (RegistroEntity) -> Unit
) {
    val azulPrincipal = Color(0xFF0B5C8E)
    val azulClaro = Color(0xFFE3F4FC)

    val hoy = remember { Calendar.getInstance() }

    var anio by remember {
        mutableIntStateOf(hoy.get(Calendar.YEAR))
    }

    var mes by remember {
        mutableIntStateOf(hoy.get(Calendar.MONTH))
    }

    val calendarioMes = Calendar.getInstance().apply {
        set(Calendar.YEAR, anio)
        set(Calendar.MONTH, mes)
        set(Calendar.DAY_OF_MONTH, 1)
    }

    val totalDias =
        calendarioMes.getActualMaximum(Calendar.DAY_OF_MONTH)

    val primerDiaSemana =
        calendarioMes.get(Calendar.DAY_OF_WEEK)

    // Convierte a semana iniciando en lunes.
    val espaciosIniciales =
        (primerDiaSemana + 5) % 7

    val nombreMes =
        SimpleDateFormat(
            "MMMM yyyy",
            Locale("es", "MX")
        ).format(calendarioMes.time)
            .replaceFirstChar {
                if (it.isLowerCase()) {
                    it.titlecase()
                } else {
                    it.toString()
                }
            }

    val celdas =
        buildList<Int?> {

            repeat(espaciosIniciales) {
                add(null)
            }

            for (dia in 1..totalDias) {
                add(dia)
            }

            while (size % 7 != 0) {
                add(null)
            }
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(azulClaro)
            .padding(20.dp)
    ) {

        Spacer(modifier = Modifier.height(30.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            TextButton(
                onClick = onBack
            ) {
                Text(
                    text = "←",
                    fontSize = 28.sp,
                    color = azulPrincipal
                )
            }

            Text(
                text = "Calendario",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = azulPrincipal
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {

            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    IconButton(
                        onClick = {
                            if (mes == Calendar.JANUARY) {
                                mes = Calendar.DECEMBER
                                anio--
                            } else {
                                mes--
                            }
                        }
                    ) {
                        Text(
                            text = "‹",
                            fontSize = 34.sp,
                            color = azulPrincipal
                        )
                    }

                    Text(
                        text = nombreMes,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = azulPrincipal
                    )

                    IconButton(
                        onClick = {
                            if (mes == Calendar.DECEMBER) {
                                mes = Calendar.JANUARY
                                anio++
                            } else {
                                mes++
                            }
                        }
                    ) {
                        Text(
                            text = "›",
                            fontSize = 34.sp,
                            color = azulPrincipal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    listOf(
                        "L", "M", "M", "J", "V", "S", "D"
                    ).forEach { dia ->

                        Text(
                            text = dia,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                celdas.chunked(7).forEach { semana ->

                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        semana.forEach { dia ->

                            if (dia == null) {

                                Spacer(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                )

                            } else {

                                val fecha =
                                    String.format(
                                        Locale.US,
                                        "%04d-%02d-%02d",
                                        anio,
                                        mes + 1,
                                        dia
                                    )

                                val registro =
                                    registros.find {
                                        it.fecha == fecha
                                    }

                                DiaCalendario(
                                    dia = dia,
                                    registro = registro,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        if (registro != null) {
                                            onRegistroClick(registro)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        Text(
            text = "Los días con un emoji tienen un registro emocional.",
            fontSize = 15.sp,
            color = Color(0xFF506070)
        )
    }
}

@Composable
private fun DiaCalendario(
    dia: Int,
    registro: RegistroEntity?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {

    val fondo =
        if (registro != null) {
            Color(0xFFE3F4FC)
        } else {
            Color.Transparent
        }

    Card(
        onClick = onClick,
        enabled = registro != null,
        modifier = modifier
            .aspectRatio(1f)
            .padding(3.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = fondo,
            disabledContainerColor = fondo
        )
    ) {

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = dia.toString(),
                fontSize = 14.sp,
                fontWeight =
                    if (registro != null) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Normal
                    }
            )

            if (registro != null) {

                Text(
                    text = emojiEstado(
                        registro.estadoValor
                    ),
                    fontSize = 20.sp
                )
            }
        }
    }
}

private fun emojiEstado(
    valor: Int
): String {

    return when (valor) {
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