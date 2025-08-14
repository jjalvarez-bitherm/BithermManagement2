package com.bithermmanagement.core.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.IOException

object DebugConfigManager {
    private const val TAG = "DebugConfigManager"
    private const val PREFS_NAME = "debug_config"
    private const val KEY_DEBUG_MODE = "debug_mode_enabled"
    
    private var debugModeEnabled: Boolean = false
    private var configLoaded = false
    
    fun isDebugModeEnabled(context: Context): Boolean {
        if (!configLoaded) {
            loadDebugConfig(context)
        }
        return debugModeEnabled
    }
    
    fun setDebugMode(context: Context, enabled: Boolean) {
        debugModeEnabled = enabled
        saveDebugMode(context, enabled)
        Log.d(TAG, "Debug mode set to: $enabled")
    }
    
    private fun loadDebugConfig(context: Context) {
        try {
            // Cargar configuración desde variables.json
            val jsonString = context.assets.open("variables.json").bufferedReader().use { it.readText() }
            val jsonObject = JsonParser.parseString(jsonString).asJsonObject
            
            // Buscar configuración de debug
            val debugConfig = jsonObject.getAsJsonObject("debug_config")
            if (debugConfig != null) {
                debugModeEnabled = debugConfig.get("enabled")?.asBoolean ?: false
                Log.d(TAG, "Debug config loaded from JSON: $debugModeEnabled")
            } else {
                // Si no existe en JSON, cargar desde SharedPreferences
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                debugModeEnabled = prefs.getBoolean(KEY_DEBUG_MODE, false)
                Log.d(TAG, "Debug config loaded from SharedPreferences: $debugModeEnabled")
            }
            
            configLoaded = true
        } catch (e: IOException) {
            Log.e(TAG, "Error loading debug config from JSON: ${e.message}")
            // Fallback a SharedPreferences
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            debugModeEnabled = prefs.getBoolean(KEY_DEBUG_MODE, false)
            configLoaded = true
        } catch (e: Exception) {
            Log.e(TAG, "Error loading debug config: ${e.message}")
            debugModeEnabled = false
            configLoaded = true
        }
    }
    
    private fun saveDebugMode(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DEBUG_MODE, enabled).apply()
    }
    
    fun getDebugConfig(context: Context): JsonObject? {
        return try {
            val jsonString = context.assets.open("variables.json").bufferedReader().use { it.readText() }
            val jsonObject = JsonParser.parseString(jsonString).asJsonObject
            jsonObject.getAsJsonObject("debug_config")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting debug config: ${e.message}")
            null
        }
    }
    
    fun getOverlayConfig(context: Context): JsonObject? {
        return getDebugConfig(context)?.getAsJsonObject("overlay")
    }
    
    fun getGestureConfig(context: Context): JsonObject? {
        return getDebugConfig(context)?.getAsJsonObject("gesture_activation")
    }
    
    fun getFeaturesConfig(context: Context): JsonObject? {
        return getDebugConfig(context)?.getAsJsonObject("features")
    }
}
