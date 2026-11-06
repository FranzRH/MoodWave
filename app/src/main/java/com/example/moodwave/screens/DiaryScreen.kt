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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodwave.data.local.RegistroEntity

@Composable
fun DiaryScreen(
    registros: List<RegistroEntity>,
    onBack: () -> Unit,
    onRegistroClick: (RegistroEntity) -> Unit
) {

    val azulPrincipal = Color(0xFF0B5C8E)
    val azulClaro = Color(0xFFE3F4FC)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(azulClaro)
            .verticalScroll(rememberScrollState())
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
                text = "Diario",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = azulPrincipal
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Tus registros emocionales",
            fontSize = 17.sp,
            color = Color(0xFF506070)
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (registros.isEmpty()) {

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "📘",
                        fontSize = 50.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Todavía no hay registros",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Cuando registres cómo te sientes, aparecerá aquí.",
                        fontSize = 15.sp,
                        color = Color.Gray
                    )
                }
            }

        } else {

            registros.forEach { registro ->

                RegistroCard(
                    registro = registro,
                    onClick = {
                        onRegistroClick(registro)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
private fun RegistroCard(
    registro: RegistroEntity,
    onClick: () -> Unit
) {

    val emoji = when (registro.estadoValor) {
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

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {

        Column(
            modifier = Modifier.padding(20.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = emoji,
                    fontSize = 42.sp
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = registro.estadoNombre,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "${registro.fecha} · ${registro.hora}",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }

                Text(
                    text = "⚡ ${registro.energia}/10",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            if (registro.reflexion.isNotBlank()) {

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = registro.reflexion,
                    fontSize = 15.sp,
                    color = Color(0xFF404040),
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Ver detalle →",
                color = Color(0xFF0B5C8E),
                fontWeight = FontWeight.Medium
            )
        }
    }
}