package com.bithermmanagement.database.dao

import androidx.room.*
import com.bithermmanagement.database.entities.MenuEntity

@Dao
interface MenuDao {
    @Query("SELECT * FROM menu ORDER BY peso ASC")
    suspend fun getAllMenus(): List<MenuEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMenus(menus: List<MenuEntity>)

    @Query("DELETE FROM menu")
    suspend fun deleteAllMenus()
} 