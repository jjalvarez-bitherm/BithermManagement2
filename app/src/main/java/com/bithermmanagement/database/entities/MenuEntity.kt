package com.bithermmanagement.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "menu")
data class MenuEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fragment: String,
    val nombreVisible: String,
    val peso: Int,
    val colorHex: String
) 