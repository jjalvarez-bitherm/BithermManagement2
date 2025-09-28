package com.bithermmanagement.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlin.random.Random

/**
 * Manager para el debug de escritura en Google Sheets
 * Genera colores aleatorios para visualizar las celdas modificadas
 */
object DebugEscrituraManager {
    
    private const val PREFS_NAME = "debug_escritura_prefs"
    private const val KEY_LAST_COLOR = "last_color_used"
    private const val KEY_DEBUG_ENABLED = "debug_escritura_enabled"
    
    // Colores disponibles para el debug (en formato RGB)
    private val coloresDisponibles = listOf(
        Triple(255, 200, 200), // Rojo claro
        Triple(200, 255, 200), // Verde claro
        Triple(200, 200, 255), // Azul claro
        Triple(255, 255, 200), // Amarillo claro
        Triple(255, 200, 255), // Magenta claro
        Triple(200, 255, 255), // Cian claro
        Triple(255, 220, 200), // Naranja claro
        Triple(220, 200, 255), // Púrpura claro
        Triple(200, 255, 220), // Verde lima
        Triple(255, 240, 200)  // Melocotón
    )
    
    /**
     * Verifica si el debug de escritura está habilitado
     */
    fun isDebugEscrituraEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_DEBUG_ENABLED, false)
    }
    
    /**
     * Genera un color aleatorio diferente al último usado
     */
    fun generarColorAleatorio(context: Context): Triple<Int, Int, Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val ultimoColorIndex = prefs.getInt(KEY_LAST_COLOR, -1)
        
        // Obtener índices disponibles (excluyendo el último usado)
        val indicesDisponibles = coloresDisponibles.indices.filter { it != ultimoColorIndex }
        
        // Si no hay colores disponibles, usar todos
        val indicesParaUsar = if (indicesDisponibles.isEmpty()) {
            coloresDisponibles.indices.toList()
        } else {
            indicesDisponibles
        }
        
        // Seleccionar color aleatorio
        val nuevoColorIndex = indicesParaUsar[Random.nextInt(indicesParaUsar.size)]
        val nuevoColor = coloresDisponibles[nuevoColorIndex]
        
        // Guardar el color usado
        prefs.edit().putInt(KEY_LAST_COLOR, nuevoColorIndex).apply()
        
        Log.d("DebugEscrituraManager", "Color generado: RGB(${nuevoColor.first}, ${nuevoColor.second}, ${nuevoColor.third}) - Índice: $nuevoColorIndex")
        
        return nuevoColor
    }
    
    /**
     * Convierte un color RGB a formato hexadecimal para Google Sheets
     */
    fun rgbToHex(color: Triple<Int, Int, Int>): String {
        val (r, g, b) = color
        return String.format("#%02X%02X%02X", r, g, b)
    }
    
    /**
     * Aplica formato de color a una celda en Google Sheets
     */
    fun aplicarColorACelda(
        context: Context,
        spreadsheetId: String,
        range: String,
        color: Triple<Int, Int, Int>
    ): Boolean {
        if (!isDebugEscrituraEnabled(context)) {
            return false
        }
        
        try {
            // Aquí se implementaría la lógica para aplicar el color a la celda
            // Esto requeriría acceso a la API de Google Sheets para modificar el formato
            Log.d("DebugEscrituraManager", "Aplicando color ${rgbToHex(color)} a rango $range en spreadsheet $spreadsheetId")
            
            // TODO: Implementar la aplicación real del color usando Google Sheets API
            // Por ahora solo loggeamos la acción
            
            return true
        } catch (e: Exception) {
            Log.e("DebugEscrituraManager", "Error aplicando color a celda: ${e.message}", e)
            return false
        }
    }
    
    /**
     * Registra una escritura en Google Sheets con debug de color
     */
    fun registrarEscritura(
        context: Context,
        spreadsheetId: String,
        range: String,
        valor: String
    ) {
        if (!isDebugEscrituraEnabled(context)) {
            return
        }
        
        val color = generarColorAleatorio(context)
        val colorHex = rgbToHex(color)
        
        Log.d("DebugEscrituraManager", "🔍 DEBUG ESCRITURA:")
        Log.d("DebugEscrituraManager", "  📊 Spreadsheet: $spreadsheetId")
        Log.d("DebugEscrituraManager", "  📍 Rango: $range")
        Log.d("DebugEscrituraManager", "  📝 Valor: $valor")
        Log.d("DebugEscrituraManager", "  🎨 Color: $colorHex")
        
        // Aplicar color a la celda
        aplicarColorACelda(context, spreadsheetId, range, color)
    }
    
    /**
     * Limpia todos los colores de debug de un spreadsheet
     */
    fun limpiarColoresDebug(context: Context, spreadsheetId: String) {
        if (!isDebugEscrituraEnabled(context)) {
            return
        }
        
        Log.d("DebugEscrituraManager", "🧹 Limpiando colores de debug del spreadsheet: $spreadsheetId")
        
        // TODO: Implementar la limpieza real de colores usando Google Sheets API
        // Por ahora solo loggeamos la acción
    }
}
