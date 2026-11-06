
package com.example.moodwave.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditRecordScreen(
    registro: RegistroEntity,
    customActivities: List<String>,
    maxReflectionChars: Int,
    onAddCustomActivity: suspend (String) -> Boolean,
    onBack: () -> Unit,
    onSave: (
        estadoValor: Int,
        estadoNombre: String,
        energia: Int,
        reflexion: String,
        actividades: List<String>
    ) -> Unit
) {

    val azulPrincipal = Color(0xFF0B5C8E)
    val azulClaro = Color(0xFFE3F4FC)

    val estados = listOf(
        Triple(1, "😡", "Enojado"),
        Triple(2, "😢", "Triste"),
        Triple(3, "😞", "Desanimado"),
        Triple(4, "😕", "Preocupado"),
        Triple(5, "😐", "Neutral"),
        Triple(6, "🙂", "Bien"),
        Triple(7, "😄", "Feliz"),
        Triple(8, "🤩", "Excelente")
    )

    val actividadesBase = listOf(
        "Trabajo",
        "Estudio",
        "Ejercicio",
        "Familia",
        "Amigos",
        "Entretenimiento",
        "Descanso",
        "Viaje",
        "Compras",
        "Tareas domésticas"
    )

    var estadoValor by remember {
        mutableIntStateOf(registro.estadoValor)
    }

    var estadoNombre by remember {
        mutableStateOf(registro.estadoNombre)
    }

    var energia by remember {
        mutableFloatStateOf(registro.energia.toFloat())
    }

    var reflexion by remember {
        mutableStateOf(
            if (registro.reflexion.length > maxReflectionChars) {
                registro.reflexion.take(maxReflectionChars)
            } else {
                registro.reflexion
            }
        )
    }

    var actividadesSeleccionadas by remember {
        mutableStateOf(
            registro.actividades
                .split(" · ")
                .filter { it.isNotBlank() }
                .toSet()
        )
    }

    var mostrarNuevaActividad by remember {
        mutableStateOf(false)
    }

    var nuevaActividad by remember {
        mutableStateOf("")
    }

    var errorActividad by remember {
        mutableStateOf("")
    }

    val scope = rememberCoroutineScope()

    val todasLasActividades = (
            actividadesBase +
                    customActivities +
                    actividadesSeleccionadas
            ).distinctBy {
            it.trim().lowercase()
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(azulClaro)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {

        Spacer(Modifier.height(30.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            TextButton(onClick = onBack) {
                Text(
                    text = "←",
                    fontSize = 28.sp,
                    color = azulPrincipal
                )
            }

            Text(
                text = "Editar registro",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = azulPrincipal
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = "¿Cómo te sentiste?",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(16.dp))

        estados.chunked(4).forEach { fila ->

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                fila.forEach { estado ->

                    val seleccionado = estadoValor == estado.first

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                estadoValor = estado.first
                                estadoNombre = estado.third
                            },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (seleccionado) {
                                Color(0xFFB9E7F7)
                            } else {
                                Color.White
                            }
                        )
                    ) {

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            Text(
                                text = estado.second,
                                fontSize = 32.sp
                            )

                            Text(
                                text = estado.third,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Nivel de energía",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "${energia.toInt()} / 10",
            fontSize = 18.sp,
            color = azulPrincipal
        )

        Slider(
            value = energia,
            onValueChange = { energia = it },
            valueRange = 1f..10f,
            steps = 8
        )

        Spacer(Modifier.height(20.dp))

        Text(
            text = "Actividades",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(14.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            todasLasActividades.forEach { actividad ->

                val seleccionada =
                    actividadesSeleccionadas.contains(actividad)

                Card(
                    modifier = Modifier.clickable {

                        actividadesSeleccionadas =
                            if (seleccionada) {
                                actividadesSeleccionadas - actividad
                            } else {
                                actividadesSeleccionadas + actividad
                            }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (seleccionada) {
                            Color(0xFFB9E7F7)
                        } else {
                            Color.White
                        }
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 1.dp
                    )
                ) {

                    Row(
                        modifier = Modifier.padding(
                            horizontal = 14.dp,
                            vertical = 10.dp
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Text(
                            text = actividad,
                            fontSize = 14.sp,
                            fontWeight = if (seleccionada) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Medium
                            },
                            color = azulPrincipal,
                            maxLines = 1
                        )

                        if (seleccionada) {

                            Spacer(Modifier.width(6.dp))

                            Text(
                                text = "✓",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = azulPrincipal
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        OutlinedButton(
            onClick = {
                mostrarNuevaActividad = true
            },
            shape = RoundedCornerShape(22.dp)
        ) {
            Text(
                text = "+ Agregar actividad",
                color = azulPrincipal
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Reflexión",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = reflexion,
            onValueChange = {
                if (it.length <= maxReflectionChars) {
                    reflexion = it
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            placeholder = {
                Text("Escribe una breve reflexión...")
            },
            supportingText = {
                Text("${reflexion.length}/$maxReflectionChars")
            },
            shape = RoundedCornerShape(18.dp)
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                onSave(
                    estadoValor,
                    estadoNombre,
                    energia.toInt(),
                    reflexion.trim(),
                    actividadesSeleccionadas.toList()
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = azulPrincipal
            )
        ) {

            Text(
                text = "Guardar cambios",
                fontSize = 18.sp
            )
        }

        Spacer(Modifier.height(30.dp))
    }

    if (mostrarNuevaActividad) {

        AlertDialog(
            onDismissRequest = {
                mostrarNuevaActividad = false
                nuevaActividad = ""
                errorActividad = ""
            },
            title = {
                Text("Agregar actividad")
            },
            text = {

                Column {

                    OutlinedTextField(
                        value = nuevaActividad,
                        onValueChange = {
                            if (it.length <= 30) {
                                nuevaActividad = it
                                errorActividad = ""
                            }
                        },
                        label = {
                            Text("Actividad")
                        },
                        singleLine = true,
                        supportingText = {
                            Text("${nuevaActividad.length}/30")
                        }
                    )

                    if (errorActividad.isNotEmpty()) {
                        Text(
                            text = errorActividad,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {

                TextButton(
                    onClick = {

                        val actividad = nuevaActividad.trim()

                        when {

                            actividad.isBlank() -> {
                                errorActividad = "Escribe un nombre."
                            }

                            actividadesBase.any {
                                it.equals(actividad, ignoreCase = true)
                            } -> {
                                errorActividad = "Esta actividad ya existe."
                            }

                            else -> {

                                scope.launch {

                                    val guardada =
                                        onAddCustomActivity(actividad)

                                    if (guardada) {

                                        actividadesSeleccionadas =
                                            actividadesSeleccionadas + actividad

                                        mostrarNuevaActividad = false
                                        nuevaActividad = ""
                                        errorActividad = ""

                                    } else {
                                        errorActividad =
                                            "Esta actividad ya existe."
                                    }
                                }
                            }
                        }
                    }
                ) {
                    Text("Agregar")
                }
            },
            dismissButton = {

                TextButton(
                    onClick = {
                        mostrarNuevaActividad = false
                        nuevaActividad = ""
                        errorActividad = ""
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}
