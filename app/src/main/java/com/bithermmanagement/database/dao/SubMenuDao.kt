package com.bithermmanagement.database.dao

import androidx.room.*
import com.bithermmanagement.database.entities.SubMenuEntity

@Dao
interface SubMenuDao {
    @Query("SELECT * FROM submenu")
    suspend fun getAllSubMenus(): List<SubMenuEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubMenus(subMenus: List<SubMenuEntity>)

    @Query("DELETE FROM submenu")
    suspend fun deleteAllSubMenus()
} 