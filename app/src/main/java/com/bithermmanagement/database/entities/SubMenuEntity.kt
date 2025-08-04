package com.bithermmanagement.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "submenu")
data class SubMenuEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val menuPrincipal: String,
    val item: String,
    val rol: Int,
    val colorHex: String,
    val nombreVisible: String
) 