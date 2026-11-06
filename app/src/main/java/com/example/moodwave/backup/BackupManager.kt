package com.example.moodwave.backup

import com.example.moodwave.data.local.RegistroEntity
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupManager {

    private const val FORMATO = "moodwave_backup"
    private const val VERSION = 1
    private const val MAX_REGISTROS = 50000

    data class BackupData(
        val registros: List<RegistroEntity>,
        val actividadesPersonalizadas: Set<String>
    )

    fun crearJson(
        registros: List<RegistroEntity>,
        actividadesPersonalizadas: Set<String>
    ): String {

        val raiz = JSONObject()

        raiz.put("formato", FORMATO)
        raiz.put("version", VERSION)
        raiz.put(
            "creadoEn",
            SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                Locale.US
            ).format(Date())
        )
        raiz.put("totalRegistros", registros.size)

        val arregloRegistros = JSONArray()

        registros
            .sortedWith(
                compareBy<RegistroEntity> { it.fecha }
                    .thenBy { it.hora }
            )
            .forEach { registro ->

                val objeto = JSONObject()

                objeto.put("fecha", registro.fecha)
                objeto.put("hora", registro.hora)
                objeto.put("estadoValor", registro.estadoValor)
                objeto.put("estadoNombre", registro.estadoNombre)
                objeto.put("energia", registro.energia)
                objeto.put("reflexion", registro.reflexion)
                objeto.put("actividades", registro.actividades)

                arregloRegistros.put(objeto)
            }

        raiz.put("registros", arregloRegistros)

        val arregloActividades = JSONArray()

        actividadesPersonalizadas
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase(Locale.getDefault()) }
            .sortedBy { it.lowercase(Locale.getDefault()) }
            .forEach { actividad ->
                arregloActividades.put(actividad)
            }

        raiz.put(
            "actividadesPersonalizadas",
            arregloActividades
        )

        raiz.put(
            "nota",
            "El respaldo no incluye PIN ni configuración biométrica."
        )

        return raiz.toString(2)
    }

    fun leerJson(
        contenido: String
    ): BackupData {

        val raiz = try {
            JSONObject(contenido)
        } catch (_: Exception) {
            throw IllegalArgumentException(
                "El archivo no contiene un JSON válido."
            )
        }

        if (
            raiz.optString("formato") != FORMATO
        ) {
            throw IllegalArgumentException(
                "El archivo no es un respaldo válido de MoodWave."
            )
        }

        val version =
            raiz.optInt("version", -1)

        if (version != VERSION) {
            throw IllegalArgumentException(
                "La versión del respaldo no es compatible."
            )
        }

        val arregloRegistros =
            raiz.optJSONArray("registros")
                ?: throw IllegalArgumentException(
                    "El respaldo no contiene registros."
                )

        if (
            arregloRegistros.length() >
            MAX_REGISTROS
        ) {
            throw IllegalArgumentException(
                "El respaldo contiene demasiados registros."
            )
        }

        val registros =
            mutableListOf<RegistroEntity>()

        val fechas =
            mutableSetOf<String>()

        for (
            indice in 0 until
            arregloRegistros.length()
        ) {

            val objeto = try {
                arregloRegistros
                    .getJSONObject(indice)
            } catch (_: Exception) {
                throw IllegalArgumentException(
                    "Hay un registro inválido en el respaldo."
                )
            }

            val fecha =
                textoRequerido(
                    objeto,
                    "fecha"
                )

            val hora =
                textoRequerido(
                    objeto,
                    "hora"
                )

            val estadoValor =
                enteroRequerido(
                    objeto,
                    "estadoValor"
                )

            val estadoNombre =
                textoRequerido(
                    objeto,
                    "estadoNombre"
                )

            val energia =
                enteroRequerido(
                    objeto,
                    "energia"
                )

            val reflexion =
                objeto.optString(
                    "reflexion",
                    ""
                )

            val actividades =
                objeto.optString(
                    "actividades",
                    ""
                )

            if (
                !fecha.matches(
                    Regex("\\d{4}-\\d{2}-\\d{2}")
                )
            ) {
                throw IllegalArgumentException(
                    "Hay una fecha inválida en el respaldo."
                )
            }

            if (
                !hora.matches(
                    Regex("\\d{2}:\\d{2}")
                )
            ) {
                throw IllegalArgumentException(
                    "Hay una hora inválida en el respaldo."
                )
            }

            if (estadoValor !in 1..8) {
                throw IllegalArgumentException(
                    "Hay un estado emocional fuera del rango permitido."
                )
            }

            if (energia !in 1..10) {
                throw IllegalArgumentException(
                    "Hay un nivel de energía fuera del rango permitido."
                )
            }

            if (!fechas.add(fecha)) {
                throw IllegalArgumentException(
                    "El respaldo contiene más de un registro para la misma fecha."
                )
            }

            registros +=
                RegistroEntity(
                    id = 0,
                    fecha = fecha,
                    hora = hora,
                    estadoValor = estadoValor,
                    estadoNombre = estadoNombre,
                    energia = energia,
                    reflexion = reflexion,
                    actividades = actividades
                )
        }

        val actividadesPersonalizadas =
            linkedSetOf<String>()

        val arregloActividades =
            raiz.optJSONArray(
                "actividadesPersonalizadas"
            )

        if (arregloActividades != null) {

            for (
                indice in 0 until
                arregloActividades.length()
            ) {

                val actividad =
                    arregloActividades
                        .optString(indice)
                        .trim()

                if (actividad.isNotBlank()) {
                    actividadesPersonalizadas +=
                        actividad
                }
            }
        }

        return BackupData(
            registros = registros,
            actividadesPersonalizadas =
                actividadesPersonalizadas
        )
    }

    private fun textoRequerido(
        objeto: JSONObject,
        clave: String
    ): String {

        if (!objeto.has(clave)) {
            throw IllegalArgumentException(
                "Falta el campo $clave en el respaldo."
            )
        }

        return objeto
            .optString(clave)
            .trim()
            .ifBlank {
                throw IllegalArgumentException(
                    "El campo $clave está vacío en el respaldo."
                )
            }
    }

    private fun enteroRequerido(
        objeto: JSONObject,
        clave: String
    ): Int {

        if (!objeto.has(clave)) {
            throw IllegalArgumentException(
                "Falta el campo $clave en el respaldo."
            )
        }

        return try {
            objeto.getInt(clave)
        } catch (_: Exception) {
            throw IllegalArgumentException(
                "El campo $clave no es válido en el respaldo."
            )
        }
    }
}
