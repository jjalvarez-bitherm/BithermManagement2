package com.bithermmanagement.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val cod: String,
    val nombre: String,
    val apellidos: String,
    val dni: String,
    val fechaNacimiento: Date?,
    val app: String,
    val password: String,
    val rol: String,
    val swWeb: String,
    val equipoAsignado: String?,
    val fechaCalibracion: Date?,
    val telefonoEmpresa: String?,
    val emailEmpresa: String?,
    val altaEmpresa: Date?,
    val telefonoPersonal: String?,
    val emailPersonal: String?,
    val categoria: String?,
    val rMedico: Boolean,
    val accesoRLR: Boolean,
    val supEjec: Boolean
) 