package com.bithermmanagement.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manager para la configuración de cámara con arquitectura híbrida:
 * - Mantiene cache local (último descargado)
 * - Si hay valores nuevos en el sheet, los añade (no reemplaza)
 */
class CameraConfigManager(private val context: Context) {
    private val TAG = "CameraConfigManager"
    private val prefs: SharedPreferences = context.getSharedPreferences("camera_config_cache", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    companion object {
        private const val KEY_PROYECTOS = "cached_proyectos"
        private const val KEY_TIPOS_DENUNCIA = "cached_tipos_denuncia"
        private const val KEY_LAST_UPDATE = "last_config_update"
    }
    
    /**
     * Obtiene la configuración híbrida: cache local + nuevos valores del sheet
     */
    suspend fun getCameraConfig(googleSheetsManager: GoogleSheetsManager?): GoogleSheetsManager.CameraConfig = withContext(Dispatchers.IO) {
        // 1. Cargar cache local
        val cachedConfig = loadCachedConfig()
        Log.d(TAG, "Cache local: ${cachedConfig.proyectos.size} proyectos, ${cachedConfig.tiposDenuncia.size} tipos")
        
        // 2. Intentar leer desde sheet (si hay conexión y manager disponible)
        val sheetConfig = if (googleSheetsManager != null) {
            try {
                googleSheetsManager.getCameraConfigFromSheet()
            } catch (e: Exception) {
                Log.e(TAG, "Error leyendo desde sheet, usando solo cache", e)
                null
            }
        } else {
            null
        }
        
        // 3. Combinar: cache + nuevos del sheet
        val mergedConfig = if (sheetConfig != null) {
            Log.d(TAG, "Sheet: ${sheetConfig.proyectos.size} proyectos, ${sheetConfig.tiposDenuncia.size} tipos")
            mergeConfigs(cachedConfig, sheetConfig)
        } else {
            Log.d(TAG, "No hay conexión al sheet, usando solo cache local")
            cachedConfig
        }
        
        // 4. Guardar resultado combinado en cache
        if (sheetConfig != null) {
            saveCachedConfig(mergedConfig)
        }
        
        mergedConfig
    }
    
    /**
     * Combina dos configuraciones: mantiene todos los del cache y añade los nuevos del sheet
     */
    private fun mergeConfigs(
        cached: GoogleSheetsManager.CameraConfig,
        fromSheet: GoogleSheetsManager.CameraConfig
    ): GoogleSheetsManager.CameraConfig {
        // Para proyectos: combinar por nombre (evitar duplicados)
        val proyectosMap = mutableMapOf<String, GoogleSheetsManager.ProyectoCamara>()
        
        // Primero añadir todos los del cache
        cached.proyectos.forEach { proyecto ->
            proyectosMap[proyecto.nombre.uppercase()] = proyecto
        }
        
        // Luego añadir/actualizar con los del sheet
        fromSheet.proyectos.forEach { proyecto ->
            val key = proyecto.nombre.uppercase()
            // Si no existe en cache o el del sheet tiene color (más reciente), actualizar
            if (!proyectosMap.containsKey(key) || proyecto.color.isNotBlank()) {
                proyectosMap[key] = proyecto
            }
        }
        
        // Para tipos de denuncia: mismo proceso
        val tiposMap = mutableMapOf<String, GoogleSheetsManager.TipoDenuncia>()
        
        cached.tiposDenuncia.forEach { tipo ->
            tiposMap[tipo.tipo.uppercase()] = tipo
        }
        
        fromSheet.tiposDenuncia.forEach { tipo ->
            val key = tipo.tipo.uppercase()
            if (!tiposMap.containsKey(key) || tipo.color.isNotBlank()) {
                tiposMap[key] = tipo
            }
        }
        
        val merged = GoogleSheetsManager.CameraConfig(
            proyectos = proyectosMap.values.toList(),
            tiposDenuncia = tiposMap.values.toList()
        )
        
        Log.d(TAG, "Configuración combinada: ${merged.proyectos.size} proyectos, ${merged.tiposDenuncia.size} tipos")
        return merged
    }
    
    /**
     * Carga la configuración desde cache local
     */
    private fun loadCachedConfig(): GoogleSheetsManager.CameraConfig {
        val proyectosJson = prefs.getString(KEY_PROYECTOS, null)
        val tiposJson = prefs.getString(KEY_TIPOS_DENUNCIA, null)
        
        val proyectos = if (proyectosJson != null) {
            try {
                val type = object : TypeToken<List<GoogleSheetsManager.ProyectoCamara>>() {}.type
                gson.fromJson<List<GoogleSheetsManager.ProyectoCamara>>(proyectosJson, type) ?: emptyList()
            } catch (e: Exception) {
                Log.e(TAG, "Error parseando proyectos desde cache", e)
                emptyList()
            }
        } else {
            emptyList()
        }
        
        val tiposDenuncia = if (tiposJson != null) {
            try {
                val type = object : TypeToken<List<GoogleSheetsManager.TipoDenuncia>>() {}.type
                gson.fromJson<List<GoogleSheetsManager.TipoDenuncia>>(tiposJson, type) ?: emptyList()
            } catch (e: Exception) {
                Log.e(TAG, "Error parseando tipos desde cache", e)
                emptyList()
            }
        } else {
            emptyList()
        }
        
        return GoogleSheetsManager.CameraConfig(proyectos, tiposDenuncia)
    }
    
    /**
     * Guarda la configuración en cache local
     */
    private fun saveCachedConfig(config: GoogleSheetsManager.CameraConfig) {
        try {
            val proyectosJson = gson.toJson(config.proyectos)
            val tiposJson = gson.toJson(config.tiposDenuncia)
            
            prefs.edit()
                .putString(KEY_PROYECTOS, proyectosJson)
                .putString(KEY_TIPOS_DENUNCIA, tiposJson)
                .putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
                .apply()
            
            Log.d(TAG, "Configuración guardada en cache")
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando configuración en cache", e)
        }
    }
    
    /**
     * Limpia el cache local (útil para testing o reset)
     */
    fun clearCache() {
        prefs.edit().clear().apply()
        Log.d(TAG, "Cache limpiado")
    }
}

