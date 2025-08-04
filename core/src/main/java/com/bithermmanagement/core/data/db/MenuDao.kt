package com.bithermmanagement.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MenuDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(menuItems: List<MenuEntity>)

    @Query("SELECT * FROM menu_items WHERE parentId = '0' OR parentId IS NULL ORDER BY weight ASC")
    fun getAllMainMenuItems(): Flow<List<MenuEntity>>

    @Query("SELECT * FROM menu_items WHERE parentId = :parentId ORDER BY weight ASC")
    fun getSubMenuItems(parentId: String): Flow<List<MenuEntity>>

    @Query("DELETE FROM menu_items")
    fun deleteAllMenuItems()

    @Query("DELETE FROM menu_items WHERE parentId = :parentId")
    fun deleteSubMenuItems(parentId: String)
} 