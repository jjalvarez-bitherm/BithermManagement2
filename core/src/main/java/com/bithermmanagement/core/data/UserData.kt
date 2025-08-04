package com.bithermmanagement.core.data

data class UserData(
    val codigo: String,
    val nombre: String,
    val apellidos: String,
    val dni: String,
    val fechaNacimiento: String,
    val app: String,
    val pass: String,
    val rol: String,
    val rolPound: Int,
    val swWeb: String,
    val equipoAsignado: String,
    val fechaCalibracion: String,
    val telefonoEmpresa: String,
    val emailEmpresa: String,
    val fechaAltaEmpresa: String,
    val telefonoPersonal: String,
    val emailPersonal: String,
    val categoria: String,
    val revisionMedica: String,
    val accesoRLR: Boolean,
    val supervisorEjecutivo: Boolean,
    val apodo: String
) 