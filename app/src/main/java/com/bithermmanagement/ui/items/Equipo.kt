package com.bithermmanagement.ui.items

data class Equipo(
    val orden: Double?, // número de orden de inspección (permite decimales)
    val id: String,
    val unidad: String,
    val area: String,
    val marca: String,
    val modelo: String,
    val tipo: String,
    val diametro: String,
    val conexion: String,
    val presEntrada: String,
    val presSalida: String,
    val descarga: String,
    val aplicacion: String,
    val servicio: String,
    val ubicacion: String,
    val estado: String,
    val fechasteado: String,
    val nota: String,
    val identidadInspector: String,
    val detectorUtilizado: String,
    val incidencias: String,
    val gpsCoord: String, // coordenadas GPS (latitud,longitud)
    val foto: String // ruta o url de la foto principal
) 