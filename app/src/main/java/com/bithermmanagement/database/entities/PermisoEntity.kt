package com.bithermmanagement.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "permiso")
data class PermisoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val usuario: String,
    val fragment: String,
    val permiso: Boolean
) 