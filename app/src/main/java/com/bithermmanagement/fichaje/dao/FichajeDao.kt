package com.bithermmanagement.fichaje.dao

import androidx.room.*
import com.bithermmanagement.fichaje.models.FichajeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FichajeDao {
    
    @Query("SELECT * FROM fichajes WHERE usuario = :usuario ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastFichajeByUser(usuario: String): FichajeEntity?
    
    @Query("SELECT * FROM fichajes WHERE usuario = :usuario ORDER BY timestamp DESC")
    fun getFichajesByUser(usuario: String): Flow<List<FichajeEntity>>
    
    @Query("SELECT * FROM fichajes WHERE usuario = :usuario AND fecha = :fecha ORDER BY timestamp ASC")
    suspend fun getFichajesByUserAndDate(usuario: String, fecha: String): List<FichajeEntity>
    
    @Query("SELECT * FROM fichajes WHERE usuario = :usuario AND tipo = :tipo ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastFichajeByUserAndType(usuario: String, tipo: String): FichajeEntity?
    
    @Insert
    suspend fun insertFichaje(fichaje: FichajeEntity)
    
    @Update
    suspend fun updateFichaje(fichaje: FichajeEntity)
    
    @Delete
    suspend fun deleteFichaje(fichaje: FichajeEntity)
    
    @Query("DELETE FROM fichajes WHERE usuario = :usuario")
    suspend fun deleteAllFichajesByUser(usuario: String)
    
    @Query("SELECT COUNT(*) FROM fichajes WHERE usuario = :usuario AND fecha = :fecha")
    suspend fun getFichajesCountByUserAndDate(usuario: String, fecha: String): Int
} 