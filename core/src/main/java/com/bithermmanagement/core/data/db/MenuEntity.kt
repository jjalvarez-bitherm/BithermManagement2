package com.bithermmanagement.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "menu_items")
data class MenuEntity(
    @PrimaryKey val id: String,
    val parentId: String?,
    val titleFragment: String,
    val visibleName: String,
    val weight: Int,
    val colorFragment: String,
    val mainMenu: String?,
    val item: String?,
    val role: Int,
    val colorCard: String,
    val iconName: String?
) 