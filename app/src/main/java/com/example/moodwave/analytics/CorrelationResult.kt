package com.example.moodwave.analytics

data class CorrelationResult(
    val actividad: String,
    val coeficiente: Double,
    val nivelRelacion: String,
    val direccion: String,
    val totalConActividad: Int,
    val totalSinActividad: Int,
    val datosSuficientes: Boolean
)