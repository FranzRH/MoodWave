package com.example.moodwave.screens

import android.Manifest
import android.net.Uri
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    biometricEnabled: Boolean,
    autoLockSeconds: Int,
    maxReflectionChars: Int,
    reminderEnabled: Boolean,
    reminderHour: Int,
    onBiometricChange: (Boolean) -> Unit,
    onAutoLockChange: (Int) -> Unit,
    onMaxReflectionCharsChange: (Int) -> Unit,
    onReminderEnabledChange: (Boolean) -> Unit,
    onReminderHourChange: (Int) -> Unit,
    onTestReminder: () -> Unit,
    onExportBackup: suspend (Uri) -> Result<Int>,
    onImportBackup: suspend (Uri) -> Result<Int>,
    onChangePin: () -> Unit,
    onLock: () -> Unit,
    onBack: () -> Unit
) {

    val azulPrincipal =
        Color(0xFF0B5C8E)

    val azulClaro =
        Color(0xFFE3F4FC)

    var menuBloqueoAbierto by remember {
        mutableStateOf(false)
    }

    var menuReflexionAbierto by remember {
        mutableStateOf(false)
    }

    var menuHoraAbierto by remember {
        mutableStateOf(false)
    }

    var mensajeRecordatorio by remember {
        mutableStateOf<String?>(null)
    }

    val context = LocalContext.current

    val scope = rememberCoroutineScope()

    var mensajeRespaldo by remember {
        mutableStateOf<String?>(null)
    }

    var confirmarImportacion by remember {
        mutableStateOf(false)
    }

    val exportarRespaldoLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts
                    .CreateDocument(
                        "application/json"
                    )
        ) { uri ->

            if (uri != null) {

                scope.launch {

                    mensajeRespaldo =
                        "Exportando respaldo..."

                    val resultado =
                        onExportBackup(uri)

                    mensajeRespaldo =
                        resultado.fold(
                            onSuccess = { total ->
                                "Respaldo exportado correctamente: $total registros."
                            },
                            onFailure = { error ->
                                error.message
                                    ?: "No se pudo exportar el respaldo."
                            }
                        )
                }
            }
        }

    val importarRespaldoLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts
                    .OpenDocument()
        ) { uri ->

            if (uri != null) {

                scope.launch {

                    mensajeRespaldo =
                        "Restaurando respaldo..."

                    val resultado =
                        onImportBackup(uri)

                    mensajeRespaldo =
                        resultado.fold(
                            onSuccess = { total ->
                                "Respaldo restaurado correctamente: $total registros."
                            },
                            onFailure = { error ->
                                error.message
                                    ?: "No se pudo importar el respaldo."
                            }
                        )
                }
            }
        }

    if (confirmarImportacion) {

        AlertDialog(
            onDismissRequest = {
                confirmarImportacion = false
            },
            title = {
                Text("Restaurar respaldo")
            },
            text = {
                Text(
                    "Al importar un respaldo se reemplazarán los registros y actividades personalizadas actuales. El PIN y la biometría no se modificarán."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmarImportacion = false

                        importarRespaldoLauncher
                            .launch(
                                arrayOf(
                                    "application/json",
                                    "text/plain",
                                    "application/octet-stream"
                                )
                            )
                    }
                ) {
                    Text("Continuar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        confirmarImportacion = false
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    val permisoNotificacionesLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.RequestPermission()
        ) { concedido ->

            if (concedido) {
                onReminderEnabledChange(true)
                mensajeRecordatorio =
                    "Recordatorio activado."
            } else {
                onReminderEnabledChange(false)
                mensajeRecordatorio =
                    "Para activar el recordatorio debes permitir las notificaciones."
            }
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(azulClaro)
            .verticalScroll(rememberScrollState())
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
                text = "Configuración",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = azulPrincipal
            )
        }

        Spacer(
            modifier = Modifier.height(22.dp)
        )

        Text(
            text = "Seguridad",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {

            Column {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Column(
                        modifier =
                            Modifier.weight(1f)
                    ) {

                        Text(
                            text =
                                "Desbloqueo biométrico",
                            fontSize = 17.sp,
                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            text =
                                "Usar la huella registrada en el dispositivo",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }

                    Switch(
                        checked =
                            biometricEnabled,
                        onCheckedChange =
                            onBiometricChange
                    )
                }

                HorizontalDivider()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {

                    Text(
                        text =
                            "Bloqueo automático",
                        fontSize = 17.sp,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )

                    Text(
                        text =
                            "Pedir PIN o huella al volver a MoodWave",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )

                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )

                    Box {

                        OutlinedButton(
                            onClick = {
                                menuBloqueoAbierto =
                                    true
                            },
                            modifier =
                                Modifier.fillMaxWidth()
                        ) {

                            Text(
                                text =
                                    textoBloqueo(
                                        autoLockSeconds
                                    )
                            )
                        }

                        DropdownMenu(
                            expanded =
                                menuBloqueoAbierto,
                            onDismissRequest = {
                                menuBloqueoAbierto =
                                    false
                            }
                        ) {

                            OpcionBloqueo(
                                texto =
                                    "Inmediatamente",
                                segundos = 0,
                                onSelect = {
                                    onAutoLockChange(it)

                                    menuBloqueoAbierto =
                                        false
                                }
                            )

                            OpcionBloqueo(
                                texto =
                                    "Después de 30 segundos",
                                segundos = 30,
                                onSelect = {
                                    onAutoLockChange(it)

                                    menuBloqueoAbierto =
                                        false
                                }
                            )

                            OpcionBloqueo(
                                texto =
                                    "Después de 1 minuto",
                                segundos = 60,
                                onSelect = {
                                    onAutoLockChange(it)

                                    menuBloqueoAbierto =
                                        false
                                }
                            )

                            OpcionBloqueo(
                                texto =
                                    "Después de 5 minutos",
                                segundos = 300,
                                onSelect = {
                                    onAutoLockChange(it)

                                    menuBloqueoAbierto =
                                        false
                                }
                            )
                        }
                    }
                }

                HorizontalDivider()

                TextButton(
                    onClick = onChangePin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {

                    Text(
                        text = "Cambiar PIN",
                        modifier =
                            Modifier.fillMaxWidth(),
                        color = azulPrincipal,
                        fontSize = 16.sp
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Text(
            text = "Diario",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {

            Column(
                modifier =
                    Modifier.padding(20.dp)
            ) {

                Text(
                    text =
                        "Límite de la reflexión",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )

                Text(
                    text =
                        "Número máximo de caracteres que puedes escribir en cada registro",
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )

                Box {

                    OutlinedButton(
                        onClick = {
                            menuReflexionAbierto =
                                true
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Text(
                            text =
                                "$maxReflectionChars caracteres"
                        )
                    }

                    DropdownMenu(
                        expanded =
                            menuReflexionAbierto,
                        onDismissRequest = {
                            menuReflexionAbierto =
                                false
                        }
                    ) {

                        listOf(
                            150,
                            300,
                            500
                        ).forEach { limite ->

                            DropdownMenuItem(
                                text = {

                                    Text(
                                        "$limite caracteres"
                                    )
                                },
                                onClick = {

                                    onMaxReflectionCharsChange(
                                        limite
                                    )

                                    menuReflexionAbierto =
                                        false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Text(
            text = "Recordatorios",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {

            Column {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            text = "Recordatorio diario",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text =
                                "Recibe una notificación para registrar cómo te sientes.",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }

                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = { activar ->

                            mensajeRecordatorio = null

                            if (!activar) {

                                onReminderEnabledChange(
                                    false
                                )

                            } else if (
                                Build.VERSION.SDK_INT >=
                                Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat
                                    .checkSelfPermission(
                                        context,
                                        Manifest.permission
                                            .POST_NOTIFICATIONS
                                    ) !=
                                PackageManager
                                    .PERMISSION_GRANTED
                            ) {

                                permisoNotificacionesLauncher
                                    .launch(
                                        Manifest.permission
                                            .POST_NOTIFICATIONS
                                    )

                            } else {

                                onReminderEnabledChange(
                                    true
                                )

                                mensajeRecordatorio =
                                    "Recordatorio activado."
                            }
                        }
                    )
                }

                HorizontalDivider()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {

                    Text(
                        text = "Hora del recordatorio",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text =
                            "Se intentará mostrar diariamente cerca de esta hora.",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    Box {

                        OutlinedButton(
                            onClick = {
                                menuHoraAbierto = true
                            },
                            modifier =
                                Modifier.fillMaxWidth()
                        ) {

                            Text(
                                text =
                                    formatoHora(
                                        reminderHour
                                    )
                            )
                        }

                        DropdownMenu(
                            expanded =
                                menuHoraAbierto,
                            onDismissRequest = {
                                menuHoraAbierto =
                                    false
                            }
                        ) {

                            listOf(
                                8,
                                12,
                                18,
                                20,
                                21,
                                22
                            ).forEach { hora ->

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            formatoHora(
                                                hora
                                            )
                                        )
                                    },
                                    onClick = {

                                        onReminderHourChange(
                                            hora
                                        )

                                        menuHoraAbierto =
                                            false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    OutlinedButton(
                        onClick = onTestReminder,
                        enabled = reminderEnabled,
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Text(
                            text =
                                "Probar notificación ahora"
                        )
                    }

                    if (
                        mensajeRecordatorio != null
                    ) {

                        Spacer(
                            modifier =
                                Modifier.height(10.dp)
                        )

                        Text(
                            text =
                                mensajeRecordatorio!!,
                            fontSize = 13.sp,
                            color = azulPrincipal
                        )
                    }
                }
            }
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Text(
            text = "Respaldo de información",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {

            Column(
                modifier = Modifier.padding(20.dp)
            ) {

                Text(
                    text = "Exportar e importar",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text =
                        "Guarda tus registros emocionales y actividades personalizadas en un archivo JSON versionado.",
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                Button(
                    onClick = {

                        val fecha =
                            SimpleDateFormat(
                                "yyyy-MM-dd",
                                Locale.getDefault()
                            ).format(Date())

                        exportarRespaldoLauncher
                            .launch(
                                "MoodWave_respaldo_$fecha.json"
                            )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Exportar respaldo")
                }

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                OutlinedButton(
                    onClick = {
                        confirmarImportacion = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Importar respaldo")
                }

                if (mensajeRespaldo != null) {

                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )

                    Text(
                        text = mensajeRespaldo!!,
                        fontSize = 13.sp,
                        color = azulPrincipal
                    )
                }

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                Text(
                    text =
                        "Por seguridad, el respaldo no incluye tu PIN ni la configuración biométrica.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        OutlinedButton(
            onClick = onLock,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape =
                RoundedCornerShape(28.dp)
        ) {

            Text(
                text = "Bloquear ahora",
                fontSize = 17.sp
            )
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Text(
            text =
                "El PIN se almacena de forma protegida y no como texto visible.",
            fontSize = 13.sp,
            color = Color.Gray
        )
    }
}

@Composable
private fun OpcionBloqueo(
    texto: String,
    segundos: Int,
    onSelect: (Int) -> Unit
) {

    DropdownMenuItem(
        text = {
            Text(texto)
        },
        onClick = {
            onSelect(segundos)
        }
    )
}

private fun textoBloqueo(
    segundos: Int
): String {

    return when (segundos) {

        0 ->
            "Inmediatamente"

        30 ->
            "Después de 30 segundos"

        60 ->
            "Después de 1 minuto"

        300 ->
            "Después de 5 minutos"

        else ->
            "Después de 1 minuto"
    }
}

private fun formatoHora(
    hora: Int
): String {

    return String.format(
        "%02d:00",
        hora.coerceIn(0, 23)
    )
}
