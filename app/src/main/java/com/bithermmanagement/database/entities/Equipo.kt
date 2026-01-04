package com.bithermmanagement.database.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "equipos")
data class Equipo(
    @PrimaryKey val id: String,
    val instalacion: String?,
    val unidad: String?,
    val area: String?,
    val linea: String?,
    var marca: String?,
    var modelo: String?,
    val tipo: String?,
    @ColumnInfo(name = "periodicidad") val periodicidad: String?,
    val diametro: String?,
    val conexion: String?,
    val aislamiento: String?,
    @ColumnInfo(name = "pres_entrada") val presEntrada: String?,
    @ColumnInfo(name = "pres_salida") val presSalida: String?,
    @ColumnInfo(name = "by_pass") val byPass: Boolean?,
    val descarga: String?,
    @ColumnInfo(name = "aplicacion") val aplicacion: String?,
    val servicio: String?,
    var ubicacion: String?,
    var estado: String?,
    @ColumnInfo(name = "status") var flota: String?,
    @ColumnInfo(name = "fecha_inspeccion") val fechaInspeccion: String?,
    var nota: String?,
    @ColumnInfo(name = "identidad_inspector") val identidadInspector: String?,
    @ColumnInfo(name = "detector_utilizado") val detectorUtilizado: String?,
    val incidencias: String?,
    @ColumnInfo(name = "gps_coord") var gpsCoord: String?,
    @ColumnInfo(name = "url_foto_equipo") var urlFotoEquipo: String?,
    @ColumnInfo(name = "url_foto_ubicacion") var urlFotoUbicacion: String?,
    val orden: Double?,
    @ColumnInfo(name = "gps_acc") var gpsAcc: String?,
    @ColumnInfo(name = "extra") val extra: String?,
    @ColumnInfo(name = "modificado_local") var modificadoLocal: Boolean? = false,
    @ColumnInfo(name = "instalacion_mf") val instalacionMf: String?,
    @ColumnInfo(name = "url_foto_manifold") var urlFotoManifold: String?,
    @ColumnInfo(name = "url_fotos_extra") var urlFotosExtra: String?
) : Parcelable 