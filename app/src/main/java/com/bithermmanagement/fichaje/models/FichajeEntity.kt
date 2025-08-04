package com.bithermmanagement.fichaje.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fichajes")
data class FichajeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val usuario: String,
    val fecha: String,
    val hora: String,
    val tipo: String, // ENTRADA o SALIDA
    val tiempoMinutos: Int,
    val gpsLink: String,
    val ubicacion: String,
    val timestamp: Long
) 