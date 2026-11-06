package com.example.moodwave.analytics

data class EmotionalStats(
    val promedio: Double,
    val mediana: Double,
    val moda: Int,
    val desviacion: Double,
    val totalRegistros: Int,
    val rachaActual: Int
)