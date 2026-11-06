package com.example.moodwave

import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.room.withTransaction
import com.example.moodwave.backup.BackupManager
import com.example.moodwave.data.local.MoodWaveDatabase
import com.example.moodwave.data.local.RegistroEntity
import com.example.moodwave.data.preferences.ActivityPreferences
import com.example.moodwave.data.preferences.DiaryPreferences
import com.example.moodwave.data.preferences.SecurityPreferences
import com.example.moodwave.data.preferences.ReminderPreferences
import com.example.moodwave.screens.*
import com.example.moodwave.notifications.ReminderScheduler
import com.example.moodwave.ui.theme.MoodWaveTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : FragmentActivity() {

    private val pantallaState =
        mutableStateOf("unlock")

    private var momentoSegundoPlano:
            Long? = null

    private var segundosBloqueoActual:
            Int = 60

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val database =
            MoodWaveDatabase.getDatabase(
                applicationContext
            )

        val securityPreferences =
            SecurityPreferences(
                applicationContext
            )

        val activityPreferences =
            ActivityPreferences(
                applicationContext
            )

        val diaryPreferences =
            DiaryPreferences(
                applicationContext
            )

        val reminderPreferences =
            ReminderPreferences(
                applicationContext
            )

        setContent {

            MoodWaveTheme {

                var pantalla by pantallaState

                var pantallaAnteriorDetalle
                        by remember {
                            mutableStateOf("diary")
                        }

                var origenEdicion by remember {
                    mutableStateOf("detail")
                }

                var registroSeleccionado
                        by remember {
                            mutableStateOf<
                                    RegistroEntity?
                                    >(null)
                        }

                val scope =
                    rememberCoroutineScope()

                val pinConfigurado
                        by securityPreferences
                            .pinConfigurado
                            .collectAsState(
                                initial = null
                            )

                val biometriaActivada
                        by securityPreferences
                            .biometriaActivada
                            .collectAsState(
                                initial = true
                            )

                val bloqueoAutomatico
                        by securityPreferences
                            .bloqueoAutomaticoSegundos
                            .collectAsState(
                                initial = 60
                            )

                val limiteReflexion
                        by diaryPreferences
                            .maxReflectionChars
                            .collectAsState(
                                initial = 300
                            )

                val actividadesPersonalizadas
                        by activityPreferences
                            .actividadesPersonalizadas
                            .collectAsState(
                                initial = emptySet()
                            )

                val recordatorioActivado
                        by reminderPreferences
                            .recordatorioActivado
                            .collectAsState(
                                initial = false
                            )

                val horaRecordatorio
                        by reminderPreferences
                            .horaRecordatorio
                            .collectAsState(
                                initial = 20
                            )

                val registros
                        by database
                            .registroDao()
                            .obtenerTodosLosRegistros()
                            .collectAsState(
                                initial =
                                    emptyList()
                            )

                LaunchedEffect(
                    bloqueoAutomatico
                ) {

                    segundosBloqueoActual =
                        bloqueoAutomatico
                }

                LaunchedEffect(
                    recordatorioActivado,
                    horaRecordatorio
                ) {

                    if (recordatorioActivado) {

                        ReminderScheduler
                            .programar(
                                applicationContext,
                                horaRecordatorio
                            )

                    } else {

                        ReminderScheduler
                            .cancelar(
                                applicationContext
                            )
                    }
                }

                val customActivities =
                    actividadesPersonalizadas
                        .sortedBy {
                            it.lowercase()
                        }

                val fechaHoy =
                    SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.getDefault()
                    ).format(Date())

                val registroHoy =
                    registros.find {
                        it.fecha == fechaHoy
                    }

                if (
                    pinConfigurado == null
                ) {

                    Box(
                        modifier =
                            Modifier.fillMaxSize(),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        CircularProgressIndicator()
                    }

                } else if (
                    pinConfigurado == false
                ) {

                    PinSetupScreen(
                        onSave = { pin ->

                            scope.launch {

                                securityPreferences
                                    .guardarPin(pin)

                                pantalla =
                                    "unlock"
                            }
                        }
                    )

                } else {

                    when (pantalla) {

                        "unlock" -> {

                            UnlockScreen(
                                biometricEnabled =
                                    biometriaActivada,

                                onValidatePin = {
                                        pin ->

                                    securityPreferences
                                        .validarPin(
                                            pin
                                        )
                                },

                                onUnlock = {

                                    pantalla =
                                        "home"
                                }
                            )
                        }

                        "home" -> {

                            HomeScreen(
                                registroHoy =
                                    registroHoy,

                                registros =
                                    registros,

                                onNewRecord = {

                                    if (
                                        registroHoy ==
                                        null
                                    ) {

                                        pantalla =
                                            "newRecord"

                                    } else {

                                        registroSeleccionado =
                                            registroHoy

                                        origenEdicion =
                                            "home"

                                        pantalla =
                                            "edit"
                                    }
                                },

                                onDiary = {

                                    pantalla =
                                        "diary"
                                },

                                onCalendar = {

                                    pantalla =
                                        "calendar"
                                },

                                onStatistics = {

                                    pantalla =
                                        "statistics"
                                },

                                onSettings = {

                                    pantalla =
                                        "settings"
                                },

                                onTimelineRecordClick = {
                                        registro ->

                                    registroSeleccionado =
                                        registro

                                    pantallaAnteriorDetalle =
                                        "home"

                                    pantalla =
                                        "detail"
                                }
                            )
                        }

                        "settings" -> {

                            SettingsScreen(
                                biometricEnabled =
                                    biometriaActivada,

                                autoLockSeconds =
                                    bloqueoAutomatico,

                                maxReflectionChars =
                                    limiteReflexion,

                                reminderEnabled =
                                    recordatorioActivado,

                                reminderHour =
                                    horaRecordatorio,

                                onBiometricChange = {
                                        activada ->

                                    scope.launch {

                                        securityPreferences
                                            .cambiarBiometria(
                                                activada
                                            )
                                    }
                                },

                                onAutoLockChange = {
                                        segundos ->

                                    scope.launch {

                                        securityPreferences
                                            .cambiarBloqueoAutomatico(
                                                segundos
                                            )
                                    }
                                },

                                onMaxReflectionCharsChange = {
                                        limite ->

                                    scope.launch {

                                        diaryPreferences
                                            .cambiarLimiteReflexion(
                                                limite
                                            )
                                    }
                                },

                                onReminderEnabledChange = {
                                        activado ->

                                    scope.launch {

                                        reminderPreferences
                                            .cambiarRecordatorioActivado(
                                                activado
                                            )
                                    }
                                },

                                onReminderHourChange = {
                                        hora ->

                                    scope.launch {

                                        reminderPreferences
                                            .cambiarHoraRecordatorio(
                                                hora
                                            )
                                    }
                                },

                                onTestReminder = {

                                    ReminderScheduler
                                        .probarAhora(
                                            applicationContext
                                        )
                                },

                                onExportBackup = { uri: Uri ->

                                    withContext(
                                        Dispatchers.IO
                                    ) {

                                        runCatching {

                                            val contenido =
                                                BackupManager
                                                    .crearJson(
                                                        registros =
                                                            registros,
                                                        actividadesPersonalizadas =
                                                            customActivities
                                                                .toSet()
                                                    )

                                            val salida =
                                                contentResolver
                                                    .openOutputStream(
                                                        uri,
                                                        "w"
                                                    )
                                                    ?: error(
                                                        "No se pudo abrir el archivo para guardar el respaldo."
                                                    )

                                            salida
                                                .bufferedWriter(
                                                    Charsets.UTF_8
                                                )
                                                .use { escritor ->
                                                    escritor.write(
                                                        contenido
                                                    )
                                                }

                                            registros.size
                                        }
                                    }
                                },

                                onImportBackup = { uri: Uri ->

                                    withContext(
                                        Dispatchers.IO
                                    ) {

                                        runCatching {

                                            val entrada =
                                                contentResolver
                                                    .openInputStream(
                                                        uri
                                                    )
                                                    ?: error(
                                                        "No se pudo abrir el archivo seleccionado."
                                                    )

                                            val bytes =
                                                entrada.use {
                                                    it.readBytes()
                                                }

                                            if (
                                                bytes.size >
                                                5 * 1024 * 1024
                                            ) {
                                                error(
                                                    "El archivo de respaldo es demasiado grande."
                                                )
                                            }

                                            val respaldo =
                                                BackupManager
                                                    .leerJson(
                                                        String(
                                                            bytes,
                                                            Charsets.UTF_8
                                                        )
                                                    )

                                            database
                                                .withTransaction {

                                                    val dao =
                                                        database
                                                            .registroDao()

                                                    dao
                                                        .eliminarTodosLosRegistros()

                                                    if (
                                                        respaldo
                                                            .registros
                                                            .isNotEmpty()
                                                    ) {
                                                        dao.guardarRegistros(
                                                            respaldo.registros
                                                        )
                                                    }
                                                }

                                            activityPreferences
                                                .reemplazarActividades(
                                                    respaldo
                                                        .actividadesPersonalizadas
                                                )

                                            respaldo.registros.size
                                        }
                                    }
                                },

                                onChangePin = {

                                    pantalla =
                                        "changePin"
                                },

                                onLock = {

                                    pantalla =
                                        "unlock"
                                },

                                onBack = {

                                    pantalla =
                                        "home"
                                }
                            )
                        }

                        "changePin" -> {

                            ChangePinScreen(
                                onBack = {

                                    pantalla =
                                        "settings"
                                },

                                onSave = {
                                        pinActual,
                                        pinNuevo ->

                                    val valido =
                                        securityPreferences
                                            .validarPin(
                                                pinActual
                                            )

                                    if (valido) {

                                        securityPreferences
                                            .guardarPin(
                                                pinNuevo
                                            )
                                    }

                                    valido
                                },

                                onDone = {

                                    pantalla =
                                        "settings"
                                }
                            )
                        }

                        "newRecord" -> {

                            NewRecordScreen(
                                customActivities =
                                    customActivities,

                                maxReflectionChars =
                                    limiteReflexion,

                                onAddCustomActivity = {
                                        actividad ->

                                    activityPreferences
                                        .agregarActividad(
                                            actividad
                                        )
                                },

                                onBack = {

                                    pantalla =
                                        "home"
                                },

                                onSave = {
                                        estadoValor,
                                        estadoNombre,
                                        energia,
                                        reflexion,
                                        actividades ->

                                    scope.launch(
                                        Dispatchers.IO
                                    ) {

                                        val fecha =
                                            SimpleDateFormat(
                                                "yyyy-MM-dd",
                                                Locale
                                                    .getDefault()
                                            )
                                                .format(
                                                    Date()
                                                )

                                        val hora =
                                            SimpleDateFormat(
                                                "HH:mm",
                                                Locale
                                                    .getDefault()
                                            )
                                                .format(
                                                    Date()
                                                )

                                        val existente =
                                            database
                                                .registroDao()
                                                .obtenerRegistroPorFecha(
                                                    fecha
                                                )

                                        if (
                                            existente ==
                                            null
                                        ) {

                                            val registro =
                                                RegistroEntity(
                                                    fecha =
                                                        fecha,
                                                    hora =
                                                        hora,
                                                    estadoValor =
                                                        estadoValor,
                                                    estadoNombre =
                                                        estadoNombre,
                                                    energia =
                                                        energia,
                                                    reflexion =
                                                        reflexion,
                                                    actividades =
                                                        actividades
                                                            .joinToString(
                                                                " · "
                                                            )
                                                )

                                            database
                                                .registroDao()
                                                .guardarRegistro(
                                                    registro
                                                )

                                        } else {

                                            val actualizado =
                                                existente.copy(
                                                    hora =
                                                        hora,
                                                    estadoValor =
                                                        estadoValor,
                                                    estadoNombre =
                                                        estadoNombre,
                                                    energia =
                                                        energia,
                                                    reflexion =
                                                        reflexion,
                                                    actividades =
                                                        actividades
                                                            .joinToString(
                                                                " · "
                                                            )
                                                )

                                            database
                                                .registroDao()
                                                .actualizarRegistro(
                                                    actualizado
                                                )
                                        }

                                        withContext(
                                            Dispatchers.Main
                                        ) {

                                            pantalla =
                                                "home"
                                        }
                                    }
                                }
                            )
                        }

                        "diary" -> {

                            DiaryScreen(
                                registros =
                                    registros,

                                onBack = {

                                    pantalla =
                                        "home"
                                },

                                onRegistroClick = {
                                        registro ->

                                    registroSeleccionado =
                                        registro

                                    pantallaAnteriorDetalle =
                                        "diary"

                                    pantalla =
                                        "detail"
                                }
                            )
                        }

                        "calendar" -> {

                            CalendarScreen(
                                registros =
                                    registros,

                                onBack = {

                                    pantalla =
                                        "home"
                                },

                                onRegistroClick = {
                                        registro ->

                                    registroSeleccionado =
                                        registro

                                    pantallaAnteriorDetalle =
                                        "calendar"

                                    pantalla =
                                        "detail"
                                }
                            )
                        }

                        "statistics" -> {

                            StatisticsScreen(
                                registros =
                                    registros,

                                onBack = {

                                    pantalla =
                                        "home"
                                }
                            )
                        }

                        "detail" -> {

                            registroSeleccionado
                                ?.let {
                                        registro ->

                                    DetailScreen(
                                        registro =
                                            registro,

                                        onBack = {

                                            pantalla =
                                                pantallaAnteriorDetalle
                                        },

                                        onEdit = {

                                            origenEdicion =
                                                "detail"

                                            pantalla =
                                                "edit"
                                        },

                                        onDelete = {

                                            scope.launch(
                                                Dispatchers.IO
                                            ) {

                                                database
                                                    .registroDao()
                                                    .eliminarRegistro(
                                                        registro
                                                    )

                                                withContext(
                                                    Dispatchers.Main
                                                ) {

                                                    registroSeleccionado =
                                                        null

                                                    pantalla =
                                                        pantallaAnteriorDetalle
                                                }
                                            }
                                        }
                                    )
                                }
                        }

                        "edit" -> {

                            registroSeleccionado
                                ?.let {
                                        registro ->

                                    EditRecordScreen(
                                        registro =
                                            registro,

                                        customActivities =
                                            customActivities,

                                        maxReflectionChars =
                                            limiteReflexion,

                                        onAddCustomActivity = {
                                                actividad ->

                                            activityPreferences
                                                .agregarActividad(
                                                    actividad
                                                )
                                        },

                                        onBack = {

                                            pantalla =
                                                if (
                                                    origenEdicion ==
                                                    "home"
                                                ) {
                                                    "home"
                                                } else {
                                                    "detail"
                                                }
                                        },

                                        onSave = {
                                                estadoValor,
                                                estadoNombre,
                                                energia,
                                                reflexion,
                                                actividades ->

                                            scope.launch(
                                                Dispatchers.IO
                                            ) {

                                                val actualizado =
                                                    registro.copy(
                                                        estadoValor =
                                                            estadoValor,
                                                        estadoNombre =
                                                            estadoNombre,
                                                        energia =
                                                            energia,
                                                        reflexion =
                                                            reflexion,
                                                        actividades =
                                                            actividades
                                                                .joinToString(
                                                                    " · "
                                                                )
                                                    )

                                                database
                                                    .registroDao()
                                                    .actualizarRegistro(
                                                        actualizado
                                                    )

                                                withContext(
                                                    Dispatchers.Main
                                                ) {

                                                    registroSeleccionado =
                                                        actualizado

                                                    pantalla =
                                                        if (
                                                            origenEdicion ==
                                                            "home"
                                                        ) {
                                                            "home"
                                                        } else {
                                                            "detail"
                                                        }
                                                }
                                            }
                                        }
                                    )
                                }
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {

        super.onStop()

        if (
            !isChangingConfigurations &&
            pantallaState.value !=
            "unlock"
        ) {

            momentoSegundoPlano =
                SystemClock
                    .elapsedRealtime()
        }
    }

    override fun onStart() {

        super.onStart()

        val momento =
            momentoSegundoPlano
                ?: return

        if (
            pantallaState.value ==
            "unlock"
        ) {

            momentoSegundoPlano =
                null

            return
        }

        val tiempoFuera =
            SystemClock
                .elapsedRealtime() -
                    momento

        val tiempoLimite =
            segundosBloqueoActual *
                    1000L

        if (
            segundosBloqueoActual == 0 ||
            tiempoFuera >=
            tiempoLimite
        ) {

            pantallaState.value =
                "unlock"
        }

        momentoSegundoPlano =
            null
    }
}