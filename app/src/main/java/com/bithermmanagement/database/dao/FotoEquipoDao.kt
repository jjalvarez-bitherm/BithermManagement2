package com.bithermmanagement.database.dao

import androidx.room.*
import com.bithermmanagement.database.entities.FotoEquipoEntity

@Dao
interface FotoEquipoDao {
    @Insert suspend fun insert(foto: FotoEquipoEntity): Long
    @Update suspend fun update(foto: FotoEquipoEntity)
    @Delete suspend fun delete(foto: FotoEquipoEntity)
    @Query("SELECT * FROM fotos_equipo WHERE idEquipo = :idEquipo AND tipo = :tipo ORDER BY fecha")
    suspend fun getFotosPorEquipoYTipo(idEquipo: String, tipo: String): List<FotoEquipoEntity>
    @Query("SELECT * FROM fotos_equipo WHERE estadoSubida != 'SUBIDA'")
    suspend fun getFotosPendientes(): List<FotoEquipoEntity>
    @Query("SELECT * FROM fotos_equipo WHERE idEquipo = :idEquipo AND tipo = :tipo AND esFavorita = 1 LIMIT 1")
    suspend fun getFavorita(idEquipo: String, tipo: String): FotoEquipoEntity?
} 