package com.bithermmanagement.ui.items

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.bithermmanagement.data.GoogleSheetsManager
import com.bithermmanagement.database.entities.Equipo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader

/**
 * Clase helper para procesar datos de inspección desde Google Sheets.
 * Contiene funciones auxiliares para mapeo de columnas, búsqueda en hojas anteriores,
 * y procesamiento de datos de equipos.
 */
class InspeccionDataProcessor(
    private val context: Context,
    private val prefs: SharedPreferences
) {
    
    /**
     * Data class para almacenar los datos de inspección de un equipo
     */
    data class DatosInspeccion(
        val estado: String,
        val fecha: String,
        val inspector: String,
        val detector: String,
        val nota: String
    )
    
    companion object {
        val CAMPOS_OBLIGATORIOS = listOf(
            "id", "area", "unidad", "instalacion", "linea", "ubicacion",
            "marca", "modelo", "tipo", "p", "diametro", "conexion",
            "aislamiento", "presEntrada", "presSalida", "byPass", "descarga",
            "aplicacion", "gps", "foto", "fotoUbic", "estado"
        )
    }
    
    /**
     * Normaliza un nombre de columna eliminando espacios, caracteres especiales y acentos
     */
    fun normalizaNombre(nombre: String): String {
        return nombre.lowercase()
            .replace(" ", "")
            .replace("_", "")
            .replace("-", "")
            .replace(".", "")
            .replace("(", "")
            .replace(")", "")
            .replace("á", "a")
            .replace("é", "e")
            .replace("í", "i")
            .replace("ó", "o")
            .replace("ú", "u")
            .replace("ñ", "n")
    }
    
    /**
     * Escapa el nombre de una hoja de Google Sheets para usarlo en rangos.
     * Solo escapa si el nombre contiene espacios o caracteres especiales.
     */
    fun escapeSheetName(sheetName: String): String {
        if (sheetName.contains(" ") || sheetName.contains("'") || sheetName.contains("!") || 
            sheetName.contains("[") || sheetName.contains("]") || sheetName.contains("\\")) {
            val escaped = sheetName.replace("'", "''")
            return "'$escaped'"
        }
        return sheetName
    }
    
    /**
     * Carga el cache de índices de columnas desde SharedPreferences
     */
    fun cargarCacheIndices(): MutableMap<String, Int> {
        val cacheJson = prefs.getString("cache_indices_columnas", "{}")
        return try {
            val jsonObject = JSONObject(cacheJson ?: "{}")
            val cache = mutableMapOf<String, Int>()
            jsonObject.keys().forEach { key ->
                cache[key] = jsonObject.getInt(key)
            }
            Log.d("InspeccionDataProcessor", "Cache de índices cargado: $cache")
            cache
        } catch (e: Exception) {
            Log.w("InspeccionDataProcessor", "Error cargando cache de índices: ${e.message}")
            mutableMapOf()
        }
    }
    
    /**
     * Guarda el cache de índices de columnas en SharedPreferences
     */
    fun guardarCacheIndices(cache: Map<String, Int>) {
        try {
            val jsonObject = JSONObject()
            cache.forEach { (key, value) ->
                jsonObject.put(key, value)
            }
            prefs.edit()
                .putString("cache_indices_columnas", jsonObject.toString())
                .apply()
            Log.d("InspeccionDataProcessor", "Cache de índices guardado: $cache")
        } catch (e: Exception) {
            Log.e("InspeccionDataProcessor", "Error guardando cache de índices: ${e.message}")
        }
    }
    
    /**
     * Busca el índice de una columna usando cache primero, luego búsqueda directa
     */
    fun buscarIndiceColumna(header: List<String>, nombreColumna: String, cache: MutableMap<String, Int>): Int {
        val cacheKey = "columna_$nombreColumna"
        if (cache.containsKey(cacheKey)) {
            val indiceCache = cache[cacheKey]!!
            if (indiceCache < header.size && header[indiceCache].equals(nombreColumna, ignoreCase = true)) {
                Log.d("InspeccionDataProcessor", "Índice encontrado en cache para '$nombreColumna': $indiceCache")
                return indiceCache
            } else {
                cache.remove(cacheKey)
                Log.d("InspeccionDataProcessor", "Cache inválido para '$nombreColumna', removido")
            }
        }
        
        // Búsqueda directa
        val indice = header.indexOfFirst { it.toString().trim().equals(nombreColumna, ignoreCase = true) }
        if (indice != -1) {
            cache[cacheKey] = indice
            Log.d("InspeccionDataProcessor", "Índice encontrado para '$nombreColumna': $indice (guardado en cache)")
        } else {
            Log.w("InspeccionDataProcessor", "Índice NO encontrado para '$nombreColumna'")
        }
        return indice
    }
    
    /**
     * Carga todos los datos de inspección de la hoja anterior de una vez.
     * Esto optimiza el proceso evitando múltiples llamadas a la API.
     * 
     * @param libroId El ID del libro de Google Sheets
     * @param sheetsManager El manager de Google Sheets
     * @param hojaInspeccionSeleccionada La hoja de inspección seleccionada (ej: "38")
     * @return Un mapa indexado por TAG (en mayúsculas) con los datos de inspección
     */
    suspend fun cargarDatosInspeccionAnterior(
        libroId: String?,
        sheetsManager: GoogleSheetsManager?,
        hojaInspeccionSeleccionada: String?
    ): Map<String, DatosInspeccion> {
        val datosInspeccionAnteriorMap = mutableMapOf<String, DatosInspeccion>()
        
        Log.e("InspeccionDataProcessor", "=== ENTRADA cargarDatosInspeccionAnterior ===")
        val libroIdStr = if (libroId != null) "NO NULL" else "NULL"
        val sheetsManagerStr = if (sheetsManager != null) "NO NULL" else "NULL"
        val hojaStr = if (hojaInspeccionSeleccionada?.isNotEmpty() == true) "NO EMPTY" else "EMPTY/NULL"
        Log.e("InspeccionDataProcessor", "libroId=${libroId != null} ($libroIdStr)")
        Log.e("InspeccionDataProcessor", "sheetsManager=${sheetsManager != null} ($sheetsManagerStr)")
        Log.e("InspeccionDataProcessor", "hojaInspeccionSeleccionada='$hojaInspeccionSeleccionada' ($hojaStr)")
        
        if (libroId == null || sheetsManager == null || hojaInspeccionSeleccionada.isNullOrEmpty()) {
            Log.e("InspeccionDataProcessor", "ERROR: No hay datos suficientes para cargar hoja anterior: libroId=${libroId != null}, sheetsManager=${sheetsManager != null}, hojaInspeccionSeleccionada=${hojaInspeccionSeleccionada?.isNotEmpty()}")
            return datosInspeccionAnteriorMap
        }
        
        return withContext(Dispatchers.IO) {
            try {
                Log.e("InspeccionDataProcessor", "Cargando datos de hoja anterior '$hojaInspeccionSeleccionada' de una vez...")
                val numeroHoja = hojaInspeccionSeleccionada.toIntOrNull()
                if (numeroHoja == null || numeroHoja <= 0) {
                    Log.e("InspeccionDataProcessor", "ERROR: La hoja seleccionada '$hojaInspeccionSeleccionada' no es un número válido")
                    return@withContext datosInspeccionAnteriorMap
                }
                
                val hojaAnterior = (numeroHoja - 1).toString()
                val hojaEscapada = escapeSheetName(hojaAnterior)
                Log.e("InspeccionDataProcessor", "Buscando en hoja anterior: '$hojaAnterior' (escapada: '$hojaEscapada')")
                
                // Las hojas de inspección tienen formato: ID, ESTADO, FECHA, INSPECTOR, DETECTOR, NOTA
                // La fila 1 contiene las cabeceras, los datos empiezan en la fila 2
                // Por lo tanto, los índices son fijos: ID=0, ESTADO=1, FECHA=2, INSPECTOR=3, DETECTOR=4, NOTA=5
                val idIndex = 0
                val estadoIndex = 1
                val fechaIndex = 2
                val inspectorIndex = 3
                val detectorIndex = 4
                val notaIndex = 5
                
                Log.e("InspeccionDataProcessor", "Índices fijos para hoja de inspección: ID=$idIndex, ESTADO=$estadoIndex, FECHA=$fechaIndex, INSPECTOR=$inspectorIndex, DETECTOR=$detectorIndex, NOTA=$notaIndex")
                
                // Leer datos (desde fila 2) - La fila 1 contiene las cabeceras
                Log.e("InspeccionDataProcessor", "Leyendo datos de hoja '$hojaEscapada' desde fila 2...")
                val dataResponse = sheetsManager.sheetsServicePublic.spreadsheets().values()
                    .get(libroId, "$hojaEscapada!A2:ZZ")
                    .execute()
                val dataRowsAnterior = dataResponse.getValues() ?: emptyList()
                Log.e("InspeccionDataProcessor", "Filas de datos encontradas: ${dataRowsAnterior.size}")
                if (dataRowsAnterior.isNotEmpty()) {
                    val primeraFila = dataRowsAnterior.first().take(6).joinToString("|")
                    Log.e("InspeccionDataProcessor", "Primera fila de datos (primeras 6 columnas): $primeraFila")
                }
                
                // Crear mapa de datos indexado por TAG
                // Nota: Los IDs en la hoja anterior pueden tener formato diferente (ej: NA-00001 vs A-00001)
                // Por lo tanto, intentamos buscar tanto con el ID original como sin el prefijo
                for (rowAnterior in dataRowsAnterior) {
                    if (rowAnterior.size > idIndex) {
                        val rowTag = rowAnterior[idIndex].toString().trim()
                        if (rowTag.isNotEmpty() && !rowTag.equals("ID", ignoreCase = true)) {
                            val estado = if (rowAnterior.size > estadoIndex) rowAnterior[estadoIndex].toString().trim() else ""
                            val fecha = if (rowAnterior.size > fechaIndex) rowAnterior[fechaIndex].toString().trim() else ""
                            val inspector = if (rowAnterior.size > inspectorIndex) rowAnterior[inspectorIndex].toString().trim() else ""
                            val detector = if (rowAnterior.size > detectorIndex) rowAnterior[detectorIndex].toString().trim() else ""
                            val nota = if (rowAnterior.size > notaIndex) rowAnterior[notaIndex].toString().trim() else ""
                            
                            val datosInspeccion = DatosInspeccion(estado, fecha, inspector, detector, nota)
                            
                            // Guardar con el ID tal cual está (mayúsculas)
                            datosInspeccionAnteriorMap[rowTag.uppercase()] = datosInspeccion
                            
                            // También guardar sin el prefijo si tiene formato NA-XXXXX o similar
                            // Esto permite encontrar A-00001 cuando el ID en la hoja anterior es NA-00001
                            val tagSinPrefijo = rowTag.uppercase().let { tag ->
                                // Si tiene formato NA-00001, guardar también como A-00001
                                if (tag.matches(Regex("^[A-Z]{1,3}-\\d+.*"))) {
                                    val partes = tag.split("-", limit = 2)
                                    if (partes.size == 2) {
                                        // Intentar con solo la primera letra del prefijo
                                        val nuevoTag = "${partes[0].first()}-${partes[1]}"
                                        nuevoTag
                                    } else {
                                        null
                                    }
                                } else {
                                    null
                                }
                            }
                            
                            if (tagSinPrefijo != null && tagSinPrefijo != rowTag.uppercase()) {
                                datosInspeccionAnteriorMap[tagSinPrefijo] = datosInspeccion
                                Log.e("InspeccionDataProcessor", "ID duplicado en mapa: '$rowTag' -> también como '$tagSinPrefijo'")
                            }
                        }
                    }
                }
                Log.e("InspeccionDataProcessor", "Cargados ${datosInspeccionAnteriorMap.size} registros de la hoja anterior '$hojaAnterior'")
                if (datosInspeccionAnteriorMap.isNotEmpty()) {
                    val primeros3 = datosInspeccionAnteriorMap.entries.take(3)
                    Log.e("InspeccionDataProcessor", "Primeros 3 registros cargados: $primeros3")
                }
            } catch (e: Exception) {
                Log.e("InspeccionDataProcessor", "ERROR cargando datos de hoja anterior: ${e.message}", e)
            }
            
            datosInspeccionAnteriorMap
        }
    }
    
    /**
     * Crea el mapeo automático de columnas basado en JSON, preferencias y coincidencias normalizadas
     */
    fun crearMapeoAutomatico(
        header: List<String>,
        mapeoColumnas: JSONObject,
        mapeoPrefs: Map<String, String>,
        cacheIndices: MutableMap<String, Int>
    ): Map<String, String> {
        val headerNormalizado = header.map { normalizaNombre(it) }
        val mapeoAuto = mutableMapOf<String, String>()
        
        Log.d("InspeccionDataProcessor", "=== MAPEO DE CAMPOS ===")
        Log.d("InspeccionDataProcessor", "Headers disponibles (${header.size}): ${header.joinToString(", ")}")
        
        for (campo in CAMPOS_OBLIGATORIOS) {
            if (campo == "gps") {
                mapeoAuto[campo] = "GPS_COORD"
                Log.d("InspeccionDataProcessor", "Campo $campo -> GPS_COORD (hardcoded)")
                continue
            }
            // 'estado' viene de la hoja anterior, no de FLOTA, así que no se mapea
            if (campo == "estado") {
                Log.d("InspeccionDataProcessor", "Campo $campo -> se obtiene de hoja anterior, no se mapea desde FLOTA")
                continue
            }
            if (campo == "aislamiento") {
                val indiceBloqueo = buscarIndiceColumna(header, "BLOQUEO", cacheIndices)
                if (indiceBloqueo != -1) {
                    mapeoAuto[campo] = "BLOQUEO"
                    Log.d("InspeccionDataProcessor", "Campo $campo -> BLOQUEO (mapeo automático)")
                    continue
                }
                val indiceAislamiento = buscarIndiceColumna(header, "AISLAMIENTO", cacheIndices)
                if (indiceAislamiento != -1) {
                    mapeoAuto[campo] = "AISLAMIENTO"
                    Log.d("InspeccionDataProcessor", "Campo $campo -> AISLAMIENTO (nombre antiguo encontrado)")
                    continue
                }
            }
            
            val columnaJson = if (mapeoColumnas.has(campo)) mapeoColumnas.getString(campo) else null
            val columnaManual = mapeoPrefs[campo]
            
            val columnaFinal = columnaManual ?: columnaJson
            if (columnaFinal != null) {
                val indice = buscarIndiceColumna(header, columnaFinal, cacheIndices)
                if (indice != -1) {
                    mapeoAuto[campo] = columnaFinal
                    Log.d("InspeccionDataProcessor", "Campo $campo mapeado a columna '$columnaFinal' (índice $indice)")
                } else {
                    Log.w("InspeccionDataProcessor", "Campo $campo NO MAPEADO - columna '$columnaFinal' no encontrada")
                }
            } else {
                val indice = headerNormalizado.indexOfFirst { it.equals(normalizaNombre(campo), ignoreCase = true) }
                if (indice != -1) {
                    val columnaEncontrada = header[indice]
                    mapeoAuto[campo] = columnaEncontrada
                    cacheIndices["columna_$columnaEncontrada"] = indice
                    Log.d("InspeccionDataProcessor", "Campo $campo mapeado automáticamente a columna '$columnaEncontrada' (índice $indice)")
                } else {
                    Log.w("InspeccionDataProcessor", "Campo $campo NO MAPEADO")
                }
            }
        }
        
        Log.d("InspeccionDataProcessor", "Mapeo final: $mapeoAuto")
        return mapeoAuto
    }
    
    /**
     * Lee el mapeo de columnas desde assets y SharedPreferences
     */
    fun leerMapeoColumnas(): Pair<JSONObject, Map<String, String>> {
        val mapeoJson = context.assets.open("mapeo_columnas.json").bufferedReader().use(BufferedReader::readText)
        val mapeoColumnas = JSONObject(mapeoJson)
        val mapeoPrefs = prefs.getStringSet("mapeo_inspeccion", null)?.associate {
            val (campo, columna) = it.split(":")
            campo to columna
        }?.toMutableMap() ?: mutableMapOf()
        
        return Pair(mapeoColumnas, mapeoPrefs)
    }
}

