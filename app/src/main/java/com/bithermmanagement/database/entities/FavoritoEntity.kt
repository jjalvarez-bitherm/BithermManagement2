package com.bithermmanagement.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorito")
data class FavoritoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val usuario: String,
    val item: String
) 