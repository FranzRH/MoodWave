package com.example.moodwave.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "registros",
    indices = [
        Index(
            value = ["fecha"],
            unique = true
        )
    ]
)
data class RegistroEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val fecha: String,

    val hora: String,

    val estadoValor: Int,

    val estadoNombre: String,

    val energia: Int,

    val reflexion: String,

    val actividades: String
)