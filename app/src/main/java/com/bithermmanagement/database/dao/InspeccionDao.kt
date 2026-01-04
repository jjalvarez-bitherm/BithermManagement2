package com.bithermmanagement.database.dao

import androidx.room.*
import com.bithermmanagement.database.entities.Equipo
import com.bithermmanagement.database.entities.InspeccionEntity

@Dao
interface InspeccionDao {
    @Query("SELECT id, estado, area, unidad, instalacion, linea, aislamiento, marca, modelo, tipo, periodicidad, diametro, conexion, pres_entrada, pres_salida, descarga, aplicacion, servicio, ubicacion, fecha_inspeccion, nota, identidad_inspector, detector_utilizado, incidencias, gps_coord, url_foto_equipo, orden, modificado_local, instalacion_mf, gps_acc, extra, by_pass, url_foto_ubicacion, url_foto_manifold, url_fotos_extra FROM equipos")
    suspend fun getAllEquipos(): List<Equipo>

    @Query("SELECT id, estado, area, unidad, instalacion, linea, aislamiento, marca, modelo, tipo, periodicidad, diametro, conexion, pres_entrada, pres_salida, descarga, aplicacion, servicio, ubicacion, fecha_inspeccion, nota, identidad_inspector, detector_utilizado, incidencias, gps_coord, url_foto_equipo, orden, modificado_local, instalacion_mf, gps_acc, extra, by_pass, url_foto_ubicacion, url_foto_manifold, url_fotos_extra FROM equipos WHERE id = :id")
    suspend fun getEquipoPorId(id: String): Equipo?

    @Query("SELECT DISTINCT estado FROM equipos WHERE estado IS NOT NULL AND estado != ''")
    suspend fun getEstadosUnicos(): List<String>

    @Query("SELECT DISTINCT area FROM equipos WHERE area IS NOT NULL AND area != ''")
    suspend fun getAreasUnicas(): List<String>

    @Query("SELECT DISTINCT unidad FROM equipos WHERE unidad IS NOT NULL AND unidad != ''")
    suspend fun getUnidadesUnicas(): List<String>

    @Query("SELECT DISTINCT marca FROM equipos WHERE marca IS NOT NULL AND marca != ''")
    suspend fun getMarcasUnicas(): List<String>

    @Query("SELECT DISTINCT modelo FROM equipos WHERE modelo IS NOT NULL AND modelo != ''")
    suspend fun getModelosUnicos(): List<String>

    @Query("SELECT DISTINCT modelo FROM equipos WHERE marca IN (:marcas) AND modelo IS NOT NULL AND modelo != ''")
    suspend fun getModelosUnicosPorMarcas(marcas: List<String>): List<String>

    @Query("SELECT DISTINCT unidad FROM equipos WHERE area = :area AND unidad IS NOT NULL AND unidad != ''")
    suspend fun getUnidadesPorArea(area: String): List<String>

    @Query("SELECT * FROM equipos WHERE modificado_local = 1")
    suspend fun getModificadasLocal(): List<Equipo>

    @Query("UPDATE equipos SET modificado_local = :nuevoValor WHERE id = :id")
    suspend fun setModificadoLocal(id: String, nuevoValor: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInspeccion(inspeccion: Equipo)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(inspecciones: List<Equipo>)

    @Update
    suspend fun updateInspeccion(inspeccion: Equipo)

    @Update
    suspend fun update(inspeccion: Equipo)

    @Delete
    suspend fun deleteInspeccion(inspeccion: Equipo)

    @Query("SELECT * FROM equipos WHERE id = :id")
    suspend fun getInspeccionById(id: String): Equipo?

    @Query("SELECT DISTINCT tipo FROM equipos WHERE tipo IS NOT NULL AND tipo != ''")
    suspend fun getTiposUnicos(): List<String>

    @Query("SELECT DISTINCT diametro FROM equipos WHERE diametro IS NOT NULL AND diametro != ''")
    suspend fun getDiametrosUnicos(): List<String>

    @Query("SELECT DISTINCT conexion FROM equipos WHERE conexion IS NOT NULL AND conexion != ''")
    suspend fun getConexionesUnicas(): List<String>

    @Query("SELECT DISTINCT pres_entrada FROM equipos WHERE pres_entrada IS NOT NULL AND pres_entrada != ''")
    suspend fun getPresionesEntradaUnicas(): List<String>

    @Query("SELECT DISTINCT pres_salida FROM equipos WHERE pres_salida IS NOT NULL AND pres_salida != ''")
    suspend fun getPresionesSalidaUnicas(): List<String>

    @Query("SELECT DISTINCT descarga FROM equipos WHERE descarga IS NOT NULL AND descarga != ''")
    suspend fun getDescargasUnicas(): List<String>

    @Query("SELECT DISTINCT aplicacion FROM equipos WHERE aplicacion IS NOT NULL AND aplicacion != ''")
    suspend fun getAplicacionesUnicas(): List<String>

    @Query("""
        SELECT 
            id, 
            estado,
            status,
            area, 
            unidad,
            instalacion,
            linea,
            aislamiento, 
            marca, 
            modelo, 
            tipo, 
            periodicidad,
            diametro, 
            conexion, 
            pres_entrada, 
            pres_salida, 
            descarga, 
            aplicacion, 
            servicio, 
            ubicacion, 
            fecha_inspeccion, 
            nota, 
            identidad_inspector, 
            detector_utilizado, 
            incidencias, 
            gps_coord, 
            url_foto_equipo, 
            orden,
            modificado_local,
            instalacion_mf,
            gps_acc,
            extra,
            by_pass,
            url_foto_ubicacion,
            url_foto_manifold,
            url_fotos_extra
        FROM equipos
    """)
    suspend fun getAllEquiposFull(): List<com.bithermmanagement.database.entities.Equipo>

    @Query("UPDATE equipos SET orden = :nuevoOrden, modificado_local = 1 WHERE id = :id")
    suspend fun actualizarOrdenEquipo(id: String, nuevoOrden: Double)

    @Update
    suspend fun actualizarEquipo(equipo: Equipo)

    @Query("SELECT DISTINCT servicio FROM equipos WHERE servicio IS NOT NULL AND servicio != ''")
    suspend fun getServiciosUnicos(): List<String>

    @Query("SELECT * FROM equipos WHERE id IN (:ids)")
    suspend fun getEquiposByIds(ids: List<String>): List<Equipo>

    @Query("SELECT DISTINCT tipo FROM equipos WHERE marca = :marca AND modelo = :modelo AND tipo IS NOT NULL AND tipo != ''")
    suspend fun getTiposUnicosPorMarcaModelo(marca: String, modelo: String): List<String>

    @Query("SELECT DISTINCT pres_salida FROM equipos WHERE pres_salida IS NOT NULL AND pres_salida != '' AND CAST(REPLACE(REPLACE(pres_salida, ',', '.'), ' ', '') AS REAL) < CAST(REPLACE(REPLACE(:presionEntrada, ',', '.'), ' ', '') AS REAL)")
    suspend fun getPresionesSalidaMenoresQue(presionEntrada: String): List<String>

    @Query("SELECT DISTINCT instalacion FROM equipos WHERE instalacion IS NOT NULL AND instalacion != ''")
    suspend fun getInstalacionesUnicas(): List<String>

    @Query("SELECT DISTINCT aislamiento FROM equipos WHERE aislamiento IS NOT NULL AND aislamiento != ''")
    suspend fun getAislamientosUnicos(): List<String>
} 
