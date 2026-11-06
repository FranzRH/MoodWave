package com.example.moodwave.analytics

data class MoodRecord(
    val fecha: String,
    val estado: Int,
    val energia: Int,
    val actividades: List<String>
)