package com.bithermmanagement.database.dao

import androidx.room.*
import com.bithermmanagement.database.entities.FavoritoEntity

@Dao
interface FavoritoDao {
    @Query("SELECT * FROM favorito WHERE usuario = :usuario")
    suspend fun getFavoritosByUsuario(usuario: String): List<FavoritoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavoritos(favoritos: List<FavoritoEntity>)

    @Query("DELETE FROM favorito WHERE usuario = :usuario")
    suspend fun deleteFavoritosByUsuario(usuario: String)
} 