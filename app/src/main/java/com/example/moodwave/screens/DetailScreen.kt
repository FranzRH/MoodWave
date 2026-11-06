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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodwave.data.local.RegistroEntity

@Composable
fun DetailScreen(
    registro: RegistroEntity,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val azulPrincipal = Color(0xFF0B5C8E)
    val azulClaro = Color(0xFFE3F4FC)

    var mostrarDialogo by remember {
        mutableStateOf(false)
    }

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
                text = "Detalle del registro",
                fontSize = 27.sp,
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
                modifier = Modifier.padding(24.dp)
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = emoji,
                        fontSize = 58.sp
                    )

                    Spacer(modifier = Modifier.width(18.dp))

                    Column {

                        Text(
                            text = registro.estadoNombre,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "${registro.fecha} · ${registro.hora}",
                            fontSize = 15.sp,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(26.dp))

                HorizontalDivider()

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Nivel de energía",
                    fontSize = 15.sp,
                    color = Color.Gray
                )

                Text(
                    text = "⚡ ${registro.energia} / 10",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(22.dp))

                Text(
                    text = "Actividades",
                    fontSize = 15.sp,
                    color = Color.Gray
                )

                Text(
                    text =
                        if (registro.actividades.isBlank()) {
                            "Sin actividades registradas"
                        } else {
                            registro.actividades
                        },
                    fontSize = 17.sp
                )

                Spacer(modifier = Modifier.height(22.dp))

                Text(
                    text = "Reflexión",
                    fontSize = 15.sp,
                    color = Color.Gray
                )

                Text(
                    text =
                        if (registro.reflexion.isBlank()) {
                            "Sin reflexión"
                        } else {
                            registro.reflexion
                        },
                    fontSize = 17.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onEdit,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = azulPrincipal
            )
        ) {
            Text(
                text = "Editar registro",
                fontSize = 17.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = {
                mostrarDialogo = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text(
                text = "Eliminar registro",
                fontSize = 17.sp
            )
        }
    }

    if (mostrarDialogo) {

        AlertDialog(
            onDismissRequest = {
                mostrarDialogo = false
            },
            title = {
                Text("Eliminar registro")
            },
            text = {
                Text(
                    "¿Seguro que deseas eliminar este registro? Esta acción no se puede deshacer."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        mostrarDialogo = false
                        onDelete()
                    }
                ) {
                    Text(
                        text = "Eliminar",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        mostrarDialogo = false
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}