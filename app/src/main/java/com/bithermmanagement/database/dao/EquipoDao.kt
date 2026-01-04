package com.bithermmanagement.database.dao

import androidx.room.*
import com.bithermmanagement.database.entities.Equipo
import com.bithermmanagement.database.entities.EquipoView

@Dao
interface EquipoDao {
    @Query("SELECT * FROM equipos")
    suspend fun getAllEquipos(): List<Equipo>

    @Query("SELECT * FROM equipos")
    suspend fun getAll(): List<Equipo>

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
            periodicidad,
            marca, 
            modelo, 
            tipo, 
            diametro, 
            conexion, 
            pres_entrada AS presEntrada, 
            pres_salida AS presSalida, 
            by_pass AS byPass,
            descarga, 
            aplicacion, 
            servicio, 
            ubicacion, 
            fecha_inspeccion AS fechasteado, 
            nota, 
            identidad_inspector AS inspector, 
            detector_utilizado AS detector, 
            incidencias, 
            gps_coord AS gps, 
            gps_acc AS gpsAcc,
            url_foto_equipo AS foto, 
            orden
        FROM equipos
        ORDER BY orden ASC
    """)
    suspend fun getAllEquiposView(): List<EquipoView>

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
            periodicidad,
            marca, 
            modelo, 
            tipo, 
            diametro, 
            conexion, 
            pres_entrada AS presEntrada, 
            pres_salida AS presSalida, 
            by_pass AS byPass,
            descarga, 
            aplicacion, 
            servicio, 
            ubicacion, 
            fecha_inspeccion AS fechasteado, 
            nota, 
            identidad_inspector AS inspector, 
            detector_utilizado AS detector, 
            incidencias, 
            gps_coord AS gps, 
            gps_acc AS gpsAcc,
            url_foto_equipo AS foto, 
            orden
        FROM equipos 
        WHERE id = :id 
        LIMIT 1
    """)
    suspend fun getEquipoViewPorId(id: String): EquipoView?

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

    @Query("SELECT DISTINCT unidad FROM equipos WHERE area IN (:areas) AND unidad IS NOT NULL AND unidad != ''")
    suspend fun getUnidadesUnicasPorAreas(areas: List<String>): List<String>

    @Query("SELECT DISTINCT unidad FROM equipos WHERE area = :area AND unidad IS NOT NULL AND unidad != ''")
    suspend fun getUnidadesPorArea(area: String): List<String>

    @Query("SELECT DISTINCT modelo FROM equipos WHERE marca IN (:marcas) AND modelo IS NOT NULL AND modelo != ''")
    suspend fun getModelosUnicosPorMarcas(marcas: List<String>): List<String>

    @Query("SELECT * FROM equipos WHERE area = :area AND unidad = :unidad")
    suspend fun getEquiposPorAreaYUnidad(area: String, unidad: String): List<Equipo>

    @Query("SELECT * FROM equipos WHERE modificado_local = 1")
    suspend fun getModificadasLocal(): List<Equipo>

    @Query("UPDATE equipos SET modificado_local = :nuevoValor WHERE id = :id")
    suspend fun setModificadoLocal(id: String, nuevoValor: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEquipo(equipo: Equipo)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(equipos: List<Equipo>)

    @Update
    suspend fun updateEquipo(equipo: Equipo)

    @Update
    suspend fun update(equipo: Equipo)

    @Delete
    suspend fun deleteEquipo(equipo: Equipo)

    @Query("SELECT * FROM equipos WHERE id = :id")
    suspend fun getEquipoById(id: String): Equipo?

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

    @Query("UPDATE equipos SET orden = :nuevoOrden, modificado_local = 1 WHERE id = :id")
    suspend fun actualizarOrdenEquipo(id: String, nuevoOrden: Double)

    @Query("SELECT DISTINCT servicio FROM equipos WHERE servicio IS NOT NULL AND servicio != ''")
    suspend fun getServiciosUnicos(): List<String>

    @Query("SELECT DISTINCT ubicacion FROM equipos WHERE ubicacion IS NOT NULL AND ubicacion != ''")
    suspend fun getUbicacionesUnicas(): List<String>

    @Query("SELECT DISTINCT instalacion FROM equipos WHERE instalacion IS NOT NULL AND instalacion != ''")
    suspend fun getInstalacionesUnicas(): List<String>

    @Query("SELECT DISTINCT aislamiento FROM equipos WHERE aislamiento IS NOT NULL AND aislamiento != ''")
    suspend fun getAislamientosUnicos(): List<String>

    @Query("SELECT DISTINCT CASE WHEN :columna = 'instalacion' THEN instalacion WHEN :columna = 'aislamiento' THEN aislamiento ELSE '' END FROM equipos WHERE CASE WHEN :columna = 'instalacion' THEN instalacion WHEN :columna = 'aislamiento' THEN aislamiento ELSE '' END IS NOT NULL AND CASE WHEN :columna = 'instalacion' THEN instalacion WHEN :columna = 'aislamiento' THEN aislamiento ELSE '' END != ''")
    suspend fun getValoresUnicos(columna: String): List<String>

    @Query("DELETE FROM equipos")
    suspend fun borrarTodo()

    @Query("SELECT DISTINCT instalacion_mf FROM equipos WHERE instalacion_mf IS NOT NULL AND instalacion_mf != ''")
    suspend fun getValoresUnicosInstalacionMf(): List<String>

    // Nuevo método para upsert individual
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEquipo(equipo: Equipo)
} 