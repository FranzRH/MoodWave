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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun ChangePinScreen(
    onBack: () -> Unit,
    onSave: suspend (
        pinActual: String,
        pinNuevo: String
    ) -> Boolean,
    onDone: () -> Unit
) {

    var pinActual by remember {
        mutableStateOf("")
    }

    var pinNuevo by remember {
        mutableStateOf("")
    }

    var confirmar by remember {
        mutableStateOf("")
    }

    var mensaje by remember {
        mutableStateOf("")
    }

    val scope = rememberCoroutineScope()

    val azulPrincipal =
        Color(0xFF0B5C8E)

    val azulClaro =
        Color(0xFFE3F4FC)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(azulClaro)
            .padding(20.dp)
    ) {

        Spacer(
            modifier = Modifier.height(30.dp)
        )

        Row(
            verticalAlignment =
                Alignment.CenterVertically
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
                text = "Cambiar PIN",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = azulPrincipal
            )
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {

            Column(
                modifier = Modifier.padding(22.dp)
            ) {

                CampoPinCambio(
                    titulo = "PIN actual",
                    value = pinActual,
                    onValueChange = {
                        pinActual = it
                    }
                )

                Spacer(
                    modifier = Modifier.height(18.dp)
                )

                CampoPinCambio(
                    titulo = "Nuevo PIN",
                    value = pinNuevo,
                    onValueChange = {
                        pinNuevo = it
                    }
                )

                Spacer(
                    modifier = Modifier.height(18.dp)
                )

                CampoPinCambio(
                    titulo = "Confirmar nuevo PIN",
                    value = confirmar,
                    onValueChange = {
                        confirmar = it
                    }
                )

                if (mensaje.isNotEmpty()) {

                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )

                    Text(
                        text = mensaje,
                        color =
                            MaterialTheme.colorScheme.error
                    )
                }

                Spacer(
                    modifier = Modifier.height(24.dp)
                )

                Button(
                    onClick = {

                        when {

                            pinActual.length != 4 ||
                                    pinNuevo.length != 4 -> {

                                mensaje =
                                    "Los PIN deben tener 4 dígitos."
                            }

                            pinNuevo != confirmar -> {

                                mensaje =
                                    "El nuevo PIN no coincide."
                            }

                            else -> {

                                scope.launch {

                                    val correcto =
                                        onSave(
                                            pinActual,
                                            pinNuevo
                                        )

                                    if (correcto) {
                                        onDone()
                                    } else {
                                        mensaje =
                                            "El PIN actual es incorrecto."
                                    }
                                }
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

                    Text("Guardar nuevo PIN")
                }
            }
        }
    }
}

@Composable
private fun CampoPinCambio(
    titulo: String,
    value: String,
    onValueChange: (String) -> Unit
) {

    Text(
        text = titulo,
        fontWeight = FontWeight.Bold
    )

    Spacer(
        modifier = Modifier.height(6.dp)
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
        visualTransformation =
            PasswordVisualTransformation(),
        keyboardOptions =
            KeyboardOptions(
                keyboardType =
                    KeyboardType.NumberPassword
            ),
        shape =
            RoundedCornerShape(16.dp)
    )
}