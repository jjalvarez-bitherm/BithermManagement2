package com.bithermmanagement.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fotos_equipo")
data class FotoEquipoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val idEquipo: String,
    val tipo: String, // "equipo", "ubicacion", "manifold"
    val rutaLocal: String,
    val urlDrive: String?,
    val esFavorita: Boolean,
    val estadoSubida: String, // "LOCAL", "PENDIENTE", "SUBIDA"
    val fecha: Long
) 