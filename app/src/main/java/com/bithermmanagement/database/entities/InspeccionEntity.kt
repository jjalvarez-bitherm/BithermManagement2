package com.bithermmanagement.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "inspecciones")
data class InspeccionEntity(
    @PrimaryKey val id: String,
    val instalacion: String?,
    val unidad: String?,
    val area: String?,
    val linea: String?,
    val marca: String?,
    val modelo: String?,
    val tipo: String?,
    @ColumnInfo(name = "p") val p: String?,
    val diametro: String?,
    val conexion: String?,
    val aislamiento: String?,
    @ColumnInfo(name = "pres_entrada") val presEntrada: String?,
    @ColumnInfo(name = "pres_salida") val presSalida: String?,
    @ColumnInfo(name = "by_pass") val byPass: String?,
    val descarga: String?,
    @ColumnInfo(name = "aplicacion") val aplicacion: String?,
    val servicio: String?,
    val ubicacion: String?,
    val estado: String?,
    @ColumnInfo(name = "fecha_inspeccion") val fechaInspeccion: String?,
    @ColumnInfo(name = "fuga_kg_h") val fugaKgH: String?,
    val nota: String?,
    @ColumnInfo(name = "identidad_inspector") val identidadInspector: String?,
    @ColumnInfo(name = "detector_utilizado") val detectorUtilizado: String?,
    val incidencias: String?,
    val gps: String?,
    val foto: String?,
    val fotoUbicacion: String?, // Foto de ubicación/lejana
    val orden: Double?, // NUEVO: número de orden de inspección (permite decimales)
    val gpsFotoUbicacion: String?, // Coordenadas de la foto de ubicación
    @ColumnInfo(name = "extra") val extra: String?, // JSON con columnas adicionales
    @ColumnInfo(name = "modificado_local") val modificadoLocal: Boolean = false
) 