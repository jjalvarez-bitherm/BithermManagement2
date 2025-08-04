package com.bithermmanagement.database.dao

import androidx.room.*
import com.bithermmanagement.database.entities.PermisoEntity

@Dao
interface PermisoDao {
    @Query("SELECT * FROM permiso WHERE usuario = :usuario")
    suspend fun getPermisosByUsuario(usuario: String): List<PermisoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPermisos(permisos: List<PermisoEntity>)

    @Query("DELETE FROM permiso WHERE usuario = :usuario")
    suspend fun deletePermisosByUsuario(usuario: String)
} 