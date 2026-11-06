package com.example.moodwave.analytics

data class TrendPoint(
    val fecha: String,
    val estado: Double,
    val energia: Double,
    val mediaMovilEstado: Double
)