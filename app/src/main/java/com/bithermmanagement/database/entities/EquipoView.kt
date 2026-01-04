package com.bithermmanagement.database.entities

// Clase de datos para la vista simplificada de equipos
data class EquipoView(
    val id: String,
    val estado: String?,
    val flota: String?, // FLOTA: ACTIVO, MONITORIZADO, AFS, ELIMINADO (no se usa para colorear)
    val area: String?,
    val unidad: String?,
    val marca: String?,
    val modelo: String?,
    val tipo: String?,
    val diametro: String?,
    val conexion: String?,
    val presEntrada: String?,
    val presSalida: String?,
    val descarga: String?,
    val aplicacion: String?,
    val servicio: String?,
    val ubicacion: String?,
    val fechasteado: String?,
    val nota: String?,
    val inspector: String?,
    val detector: String?,
    val incidencias: String?,
    val gps: String?,
    val gpsAcc: String?, // Precisión GPS
    val foto: String?,
    val orden: Double?,
    val instalacion: String?,
    val linea: String?,
    val aislamiento: String?,
    val periodicidad: String?,
    val byPass: Boolean?
) 