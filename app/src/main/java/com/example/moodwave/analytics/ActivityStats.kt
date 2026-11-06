package com.example.moodwave.analytics

data class ActivityStats(
    val actividad: String,
    val estadoPromedio: Double,
    val energiaPromedio: Double,
    val diferenciaMediaGeneral: Double,
    val totalRegistros: Int,
    val datosSuficientes: Boolean,
    val tamanoEfecto: Double,
    val nivelEfecto: String
)