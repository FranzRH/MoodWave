package com.example.moodwave.screens

import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.launch

@Composable
fun UnlockScreen(
    biometricEnabled: Boolean,
    onValidatePin: suspend (String) -> Boolean,
    onUnlock: () -> Unit
) {

    var pin by remember {
        mutableStateOf("")
    }

    var mensaje by remember {
        mutableStateOf("")
    }

    var validando by remember {
        mutableStateOf(false)
    }

    val scope =
        rememberCoroutineScope()

    val context =
        LocalContext.current

    val activity =
        context.findFragmentActivity()

    val azulPrincipal =
        Color(0xFF0B5C8E)

    val azulClaro =
        Color(0xFFE3F4FC)

    val biometricManager =
        remember {
            BiometricManager.from(context)
        }

    val executor =
        remember {
            ContextCompat.getMainExecutor(
                context
            )
        }

    val biometricPrompt =
        remember(activity) {

            activity?.let {

                BiometricPrompt(
                    it,
                    executor,
                    object :
                        BiometricPrompt.AuthenticationCallback() {

                        override fun onAuthenticationSucceeded(
                            result:
                            BiometricPrompt.AuthenticationResult
                        ) {

                            super
                                .onAuthenticationSucceeded(
                                    result
                                )

                            mensaje = ""
                            onUnlock()
                        }

                        override fun onAuthenticationFailed() {

                            super
                                .onAuthenticationFailed()

                            mensaje =
                                "Huella no reconocida."
                        }

                        override fun onAuthenticationError(
                            errorCode: Int,
                            errString: CharSequence
                        ) {

                            super
                                .onAuthenticationError(
                                    errorCode,
                                    errString
                                )

                            if (
                                errorCode !=
                                BiometricPrompt.ERROR_USER_CANCELED &&
                                errorCode !=
                                BiometricPrompt.ERROR_NEGATIVE_BUTTON
                            ) {

                                mensaje =
                                    errString.toString()
                            }
                        }
                    }
                )
            }
        }

    val promptInfo =
        remember {

            val builder =
                BiometricPrompt
                    .PromptInfo
                    .Builder()
                    .setTitle(
                        "Desbloquear MoodWave"
                    )
                    .setSubtitle(
                        "Coloca tu dedo en el sensor"
                    )
                    .setNegativeButtonText(
                        "Cancelar"
                    )

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.R
            ) {

                builder
                    .setAllowedAuthenticators(
                        BiometricManager
                            .Authenticators
                            .BIOMETRIC_WEAK
                    )
            }

            builder.build()
        }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(azulClaro)
            .padding(24.dp)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 60.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(
                        Color(0xFFB9E7F7)
                    ),
                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text = "🌊",
                    fontSize = 64.sp
                )
            }

            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )

            Text(
                text = "MoodWave",
                fontSize = 38.sp,
                fontWeight =
                    FontWeight.Bold,
                color = azulPrincipal
            )

            Text(
                text =
                    "Tus días también cuentan",
                fontSize = 16.sp,
                color = Color(0xFF506070)
            )

            Spacer(
                modifier =
                    Modifier.height(40.dp)
            )

            Card(
                modifier =
                    Modifier.fillMaxWidth(),
                shape =
                    RoundedCornerShape(28.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            Color.White
                    )
            ) {

                Column(
                    modifier =
                        Modifier.padding(24.dp),
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Text(
                        text =
                            "Introduce tu PIN",
                        fontSize = 20.sp,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(20.dp)
                    )

                    OutlinedTextField(
                        value = pin,
                        onValueChange = {

                            if (
                                it.length <= 4 &&
                                it.all { caracter ->
                                    caracter.isDigit()
                                }
                            ) {

                                pin = it
                                mensaje = ""
                            }
                        },
                        modifier =
                            Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle =
                            LocalTextStyle
                                .current
                                .copy(
                                    textAlign =
                                        TextAlign.Center,
                                    fontSize = 24.sp
                                ),
                        visualTransformation =
                            PasswordVisualTransformation(),
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType =
                                    KeyboardType
                                        .NumberPassword
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
                            RoundedCornerShape(
                                16.dp
                            )
                    )

                    Spacer(
                        modifier =
                            Modifier.height(24.dp)
                    )

                    Button(
                        onClick = {

                            if (
                                pin.length != 4
                            ) {

                                mensaje =
                                    "Ingresa los 4 dígitos"

                            } else {

                                scope.launch {

                                    validando = true

                                    val correcto =
                                        onValidatePin(
                                            pin
                                        )

                                    validando = false

                                    if (correcto) {

                                        pin = ""
                                        mensaje = ""

                                        onUnlock()

                                    } else {

                                        mensaje =
                                            "PIN incorrecto"
                                    }
                                }
                            }
                        },
                        enabled = !validando,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape =
                            RoundedCornerShape(
                                28.dp
                            ),
                        colors =
                            ButtonDefaults
                                .buttonColors(
                                    containerColor =
                                        azulPrincipal
                                )
                    ) {

                        Text(
                            text =
                                if (
                                    validando
                                ) {
                                    "Comprobando..."
                                } else {
                                    "Desbloquear"
                                },
                            fontSize = 18.sp
                        )
                    }

                    if (
                        mensaje.isNotEmpty()
                    ) {

                        Spacer(
                            modifier =
                                Modifier.height(
                                    12.dp
                                )
                        )

                        Text(
                            text = mensaje,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .error,
                            textAlign =
                                TextAlign.Center
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.height(22.dp)
                    )

                    if (biometricEnabled) {

                        Text(
                            text =
                                "o usa tu huella digital",
                            color =
                                Color.Gray
                        )

                        Spacer(
                            modifier =
                                Modifier.height(
                                    12.dp
                                )
                        )

                        OutlinedButton(
                            onClick = {

                                val resultado =
                                    comprobarBiometria(
                                        biometricManager
                                    )

                                when (resultado) {

                                    BiometricManager
                                        .BIOMETRIC_SUCCESS -> {

                                        mensaje = ""

                                        biometricPrompt
                                            ?.authenticate(
                                                promptInfo
                                            )
                                    }

                                    BiometricManager
                                        .BIOMETRIC_STATUS_UNKNOWN -> {

                                        biometricPrompt
                                            ?.authenticate(
                                                promptInfo
                                            )
                                    }

                                    BiometricManager
                                        .BIOMETRIC_ERROR_NONE_ENROLLED -> {

                                        mensaje =
                                            "No hay huellas registradas."
                                    }

                                    BiometricManager
                                        .BIOMETRIC_ERROR_NO_HARDWARE -> {

                                        mensaje =
                                            "No se detectó un sensor biométrico."
                                    }

                                    else -> {

                                        mensaje =
                                            "La biometría no está disponible."
                                    }
                                }
                            },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                            shape =
                                RoundedCornerShape(
                                    24.dp
                                )
                        ) {

                            Text(
                                text =
                                    "Usar huella digital"
                            )
                        }

                    } else {

                        Text(
                            text =
                                "La biometría está desactivada.",
                            color =
                                Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
private fun comprobarBiometria(
    biometricManager:
    BiometricManager
): Int {

    return if (
        Build.VERSION.SDK_INT >=
        Build.VERSION_CODES.R
    ) {

        biometricManager
            .canAuthenticate(
                BiometricManager
                    .Authenticators
                    .BIOMETRIC_WEAK
            )

    } else {

        biometricManager
            .canAuthenticate()
    }
}

private fun Context.findFragmentActivity():
        FragmentActivity? {

    var contextoActual = this

    while (
        contextoActual is ContextWrapper
    ) {

        if (
            contextoActual
                    is FragmentActivity
        ) {

            return contextoActual
        }

        contextoActual =
            contextoActual.baseContext
    }

    return contextoActual
            as? FragmentActivity
}