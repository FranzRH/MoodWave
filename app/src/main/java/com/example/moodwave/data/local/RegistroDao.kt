package com.example.moodwave.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RegistroDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarRegistro(
        registro: RegistroEntity
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarRegistros(
        registros: List<RegistroEntity>
    )

    @Update
    suspend fun actualizarRegistro(
        registro: RegistroEntity
    )

    @Delete
    suspend fun eliminarRegistro(
        registro: RegistroEntity
    )

    @Query(
        """
        SELECT * FROM registros
        ORDER BY fecha DESC, hora DESC
        """
    )
    fun obtenerTodosLosRegistros(): Flow<List<RegistroEntity>>

    @Query(
        """
        SELECT * FROM registros
        WHERE fecha = :fecha
        LIMIT 1
        """
    )
    suspend fun obtenerRegistroPorFecha(
        fecha: String
    ): RegistroEntity?

    @Query(
        """
        SELECT * FROM registros
        WHERE id = :id
        LIMIT 1
        """
    )
    suspend fun obtenerRegistroPorId(
        id: Int
    ): RegistroEntity?

    @Query(
        """
        DELETE FROM registros
        WHERE id = :id
        """
    )
    suspend fun eliminarRegistroPorId(
        id: Int
    )

    @Query(
        """
        DELETE FROM registros
        """
    )
    suspend fun eliminarTodosLosRegistros()

    @Query(
        """
        SELECT COUNT(*) FROM registros
        """
    )
    fun contarRegistros(): Flow<Int>
}
