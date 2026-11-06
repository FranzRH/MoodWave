package com.example.moodwave.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PinSetupScreen(
    onSave: (String) -> Unit
) {

    var pin by remember {
        mutableStateOf("")
    }

    var confirmarPin by remember {
        mutableStateOf("")
    }

    var mensaje by remember {
        mutableStateOf("")
    }

    val azulPrincipal =
        Color(0xFF0B5C8E)

    val azulClaro =
        Color(0xFFE3F4FC)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(azulClaro)
            .padding(24.dp),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Spacer(
            modifier = Modifier.height(70.dp)
        )

        Text(
            text = "🌊",
            fontSize = 70.sp
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Text(
            text = "Protege MoodWave",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = azulPrincipal
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "Crea un PIN de 4 dígitos",
            fontSize = 17.sp,
            color = Color(0xFF506070)
        )

        Spacer(
            modifier = Modifier.height(36.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {

            Column(
                modifier = Modifier.padding(24.dp)
            ) {

                PinField(
                    titulo = "Nuevo PIN",
                    value = pin,
                    onValueChange = {
                        pin = it
                        mensaje = ""
                    }
                )

                Spacer(
                    modifier = Modifier.height(20.dp)
                )

                PinField(
                    titulo = "Confirmar PIN",
                    value = confirmarPin,
                    onValueChange = {
                        confirmarPin = it
                        mensaje = ""
                    }
                )

                if (mensaje.isNotEmpty()) {

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    Text(
                        text = mensaje,
                        color =
                            MaterialTheme.colorScheme.error
                    )
                }

                Spacer(
                    modifier = Modifier.height(26.dp)
                )

                Button(
                    onClick = {

                        when {

                            pin.length != 4 -> {
                                mensaje =
                                    "El PIN debe tener 4 dígitos."
                            }

                            pin != confirmarPin -> {
                                mensaje =
                                    "Los PIN no coinciden."
                            }

                            else -> {
                                onSave(pin)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape =
                        RoundedCornerShape(28.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                azulPrincipal
                        )
                ) {

                    Text(
                        text = "Guardar PIN",
                        fontSize = 17.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PinField(
    titulo: String,
    value: String,
    onValueChange: (String) -> Unit
) {

    Text(
        text = titulo,
        fontWeight = FontWeight.Bold
    )

    Spacer(
        modifier = Modifier.height(8.dp)
    )

    OutlinedTextField(
        value = value,
        onValueChange = {

            if (
                it.length <= 4 &&
                it.all { caracter ->
                    caracter.isDigit()
                }
            ) {
                onValueChange(it)
            }
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        textStyle =
            LocalTextStyle.current.copy(
                textAlign = TextAlign.Center,
                fontSize = 22.sp
            ),
        visualTransformation =
            PasswordVisualTransformation(),
        keyboardOptions =
            KeyboardOptions(
                keyboardType =
                    KeyboardType.NumberPassword
            ),
        placeholder = {

            Text(
                text = "••••",
                modifier =
                    Modifier.fillMaxWidth(),
                textAlign =
                    TextAlign.Center
            )
        },
        shape =
            RoundedCornerShape(16.dp)
    )
}