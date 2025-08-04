package com.bithermmanagement.core.data.db

import androidx.room.*
import com.bithermmanagement.core.database.entities.InspeccionEntity
import com.bithermmanagement.core.database.entities.EquipoView

@Dao
interface InspeccionDao {
    @Query("SELECT * FROM inspecciones")
    fun getAllInspecciones(): List<InspeccionEntity>

    @Query("SELECT * FROM inspecciones")
    fun getAll(): List<InspeccionEntity>

    @Query("""
        SELECT 
            id, 
            estado, 
            area, 
            unidad,
            instalacion,
            linea,
            aislamiento, 
            marca, 
            modelo, 
            tipo, 
            diametro, 
            conexion, 
            pres_entrada AS presEntrada, 
            pres_salida AS presSalida, 
            descarga, 
            aplicacion, 
            servicio, 
            ubicacion, 
            fecha_inspeccion AS fechasteado, 
            nota, 
            identidad_inspector AS inspector, 
            detector_utilizado AS detector, 
            incidencias, 
            gps, 
            foto, 
            orden,
            NULL AS fotoUbic,
            NULL AS fotoManifold,
            NULL AS gpsFotoUbicacion,
            CASE 
                WHEN extra IS NOT NULL AND extra != '' 
                THEN json_extract(extra, '$.periodicidad') 
                ELSE NULL 
            END AS periodicidad,
            CASE 
                WHEN extra IS NOT NULL AND extra != '' 
                THEN json_extract(extra, '$.byPass') = 'true'
                ELSE NULL 
            END AS byPass,
            fuga_kg_h AS fugaKgH,
            CASE 
                WHEN extra IS NOT NULL AND extra != '' 
                THEN json_extract(extra, '$.instalacionType') 
                ELSE NULL 
            END AS instalacionType,
            CASE 
                WHEN extra IS NOT NULL AND extra != '' 
                THEN json_extract(extra, '$.manifold') 
                ELSE NULL 
            END AS manifold
        FROM inspecciones
        ORDER BY orden ASC
    """)
    fun getAllEquiposView(): List<EquipoView>

    @Query("""
        SELECT 
            id, 
            estado, 
            area, 
            unidad,
            instalacion,
            linea,
            aislamiento, 
            marca, 
            modelo, 
            tipo, 
            diametro, 
            conexion, 
            pres_entrada AS presEntrada, 
            pres_salida AS presSalida, 
            descarga, 
            aplicacion, 
            servicio, 
            ubicacion, 
            fecha_inspeccion AS fechasteado, 
            nota, 
            identidad_inspector AS inspector, 
            detector_utilizado AS detector, 
            incidencias, 
            gps, 
            foto, 
            orden,
            NULL AS fotoUbic,
            NULL AS fotoManifold,
            NULL AS gpsFotoUbicacion,
            CASE 
                WHEN extra IS NOT NULL AND extra != '' 
                THEN json_extract(extra, '$.periodicidad') 
                ELSE NULL 
            END AS periodicidad,
            CASE 
                WHEN extra IS NOT NULL AND extra != '' 
                THEN json_extract(extra, '$.byPass') = 'true'
                ELSE NULL 
            END AS byPass,
            fuga_kg_h AS fugaKgH,
            CASE 
                WHEN extra IS NOT NULL AND extra != '' 
                THEN json_extract(extra, '$.instalacionType') 
                ELSE NULL 
            END AS instalacionType,
            CASE 
                WHEN extra IS NOT NULL AND extra != '' 
                THEN json_extract(extra, '$.manifold') 
                ELSE NULL 
            END AS manifold
        FROM inspecciones 
        WHERE id = :id 
        LIMIT 1
    """)
    fun getEquipoViewPorId(id: String): EquipoView?

    @Query("SELECT DISTINCT estado FROM inspecciones WHERE estado IS NOT NULL AND estado != ''")
    fun getEstadosUnicos(): List<String>

    @Query("SELECT DISTINCT area FROM inspecciones WHERE area IS NOT NULL AND area != ''")
    fun getAreasUnicas(): List<String>

    @Query("SELECT DISTINCT unidad FROM inspecciones WHERE unidad IS NOT NULL AND unidad != ''")
    fun getUnidadesUnicas(): List<String>

    @Query("SELECT DISTINCT marca FROM inspecciones WHERE marca IS NOT NULL AND marca != ''")
    fun getMarcasUnicas(): List<String>

    @Query("SELECT DISTINCT modelo FROM inspecciones WHERE modelo IS NOT NULL AND modelo != ''")
    fun getModelosUnicos(): List<String>

    @Query("SELECT DISTINCT unidad FROM inspecciones WHERE area IN (:areas) AND unidad IS NOT NULL AND unidad != ''")
    fun getUnidadesUnicasPorAreas(areas: List<String>): List<String>

    @Query("SELECT DISTINCT modelo FROM inspecciones WHERE marca IN (:marcas) AND modelo IS NOT NULL AND modelo != ''")
    fun getModelosUnicosPorMarcas(marcas: List<String>): List<String>
} 