package com.bithermmanagement.multimedia

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.location.Location
import com.google.android.gms.maps.model.LatLng
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class FotoManager(private val context: Context) {
    
    companion object {
        const val TIPO_EQUIPO = "EQUIPO"
        const val TIPO_UBICACION = "UBICACION"
        const val TIPO_MANIFOLD = "MANIFOLD"
        const val TIPO_MAPA = "MAPA"
        const val TIPO_EXTRA = "EXTRA"
        
        private const val FOLDER_NAME = "BithermFotos"
    }
    
    /**
     * Crea la estructura de carpetas: BithermFotos/Libro/Unidad_Area/
     */
    fun crearEstructuraCarpetas(libro: String, unidad: String, area: String): File {
        val baseDir = File(context.getExternalFilesDir(null), FOLDER_NAME)
        val libroDir = File(baseDir, libro.replace(" (APP)", ""))
        val unidadAreaDir = File(libroDir, "${unidad}_${area}")
        
        if (!unidadAreaDir.exists()) {
            unidadAreaDir.mkdirs()
        }
        
        return unidadAreaDir
    }
    
    /**
     * Guarda una foto en la estructura de carpetas correcta
     */
    fun guardarFoto(
        equipoId: String,
        tipo: String,
        bitmap: Bitmap,
        libro: String,
        unidad: String,
        area: String,
        coordenadas: LatLng? = null
    ): String {
        val carpeta = crearEstructuraCarpetas(libro, unidad, area)
        val fecha = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        
        val nombreArchivo = when (tipo) {
            TIPO_MAPA -> "${equipoId}_MAPA_${fecha}.jpg"
            else -> "${equipoId}_${tipo}_${fecha}.jpg"
        }
        
        val archivo = File(carpeta, nombreArchivo)
        
        try {
            FileOutputStream(archivo).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            
            // También guardar en memoria interna para la app
            guardarFotoInterna(equipoId, tipo, bitmap, fecha)
            
            return archivo.absolutePath
        } catch (e: Exception) {
            throw RuntimeException("Error guardando foto: ${e.message}", e)
        }
    }
    
    /**
     * Guarda una foto en la memoria interna de la app
     */
    private fun guardarFotoInterna(equipoId: String, tipo: String, bitmap: Bitmap, fecha: String) {
        val dir = File(context.filesDir, "fotos")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        
        val nombreArchivo = "${equipoId}_${tipo}_${fecha}.jpg"
        val archivo = File(dir, nombreArchivo)
        
        FileOutputStream(archivo).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
    }
    
    /**
     * Genera una imagen de mapa con las coordenadas del equipo
     */
    fun generarImagenMapa(equipoId: String, coordenadas: LatLng): Bitmap {
        val width = 800
        val height = 600
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Fondo
        val paint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        
        // Borde
        paint.apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRect(2f, 2f, (width - 2).toFloat(), (height - 2).toFloat(), paint)
        
        // Texto
        paint.apply {
            color = Color.BLACK
            style = Paint.Style.FILL
            textSize = 24f
            isFakeBoldText = true
        }
        
        val lat = coordenadas.latitude
        val lng = coordenadas.longitude
        
        canvas.drawText("MAPA DE UBICACIÓN", 50f, 50f, paint)
        canvas.drawText("Equipo: $equipoId", 50f, 100f, paint)
        canvas.drawText("Latitud: ${String.format("%.6f", lat)}", 50f, 150f, paint)
        canvas.drawText("Longitud: ${String.format("%.6f", lng)}", 50f, 200f, paint)
        
        // Dibujar un marcador en el centro
        paint.apply {
            color = Color.RED
            style = Paint.Style.FILL
        }
        val centerX = width / 2f
        val centerY = height / 2f
        canvas.drawCircle(centerX, centerY, 20f, paint)
        
        // Borde del marcador
        paint.apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawCircle(centerX, centerY, 20f, paint)
        
        return bitmap
    }
    
    /**
     * Obtiene las rutas de las fotos de un equipo
     */
    fun obtenerRutasFotos(equipoId: String): Map<String, String> {
        val rutas = mutableMapOf<String, String>()
        
        // Buscar en la memoria interna de la app
        val dirInterno = File(context.filesDir, "fotos")
        if (dirInterno.exists()) {
            val archivosInternos = dirInterno.listFiles()?.filter { it.name.startsWith(equipoId) } ?: emptyList()
            archivosInternos.forEach { archivo ->
                val nombre = archivo.name
                when {
                    nombre.contains("_EQUIPO_") -> rutas[TIPO_EQUIPO] = archivo.absolutePath
                    nombre.contains("_UBICACION_") -> rutas[TIPO_UBICACION] = archivo.absolutePath
                    nombre.contains("_MANIFOLD_") -> rutas[TIPO_MANIFOLD] = archivo.absolutePath
                    nombre.contains("_MAPA_") -> rutas[TIPO_MAPA] = archivo.absolutePath
                    nombre.contains("_EXTRA_") -> {
                        val numero = nombre.substringAfter("_EXTRA_").substringBefore(".")
                        rutas["${TIPO_EXTRA}_$numero"] = archivo.absolutePath
                    }
                }
            }
        }
        
        // También buscar en la estructura externa si no se encontraron fotos
        if (rutas.isEmpty()) {
            val baseDir = File(context.getExternalFilesDir(null), FOLDER_NAME)
            if (baseDir.exists()) {
                // Buscar en todas las subcarpetas
                baseDir.walkTopDown().forEach { archivo ->
                    if (archivo.isFile && archivo.name.startsWith(equipoId)) {
                        val nombre = archivo.name
                        when {
                            nombre.contains("_EQUIPO_") -> rutas[TIPO_EQUIPO] = archivo.absolutePath
                            nombre.contains("_UBICACION_") -> rutas[TIPO_UBICACION] = archivo.absolutePath
                            nombre.contains("_MANIFOLD_") -> rutas[TIPO_MANIFOLD] = archivo.absolutePath
                            nombre.contains("_MAPA_") -> rutas[TIPO_MAPA] = archivo.absolutePath
                            nombre.contains("_EXTRA_") -> {
                                val numero = nombre.substringAfter("_EXTRA_").substringBefore(".")
                                rutas["${TIPO_EXTRA}_$numero"] = archivo.absolutePath
                            }
                        }
                    }
                }
            }
        }
        
        return rutas
    }
    
    /**
     * Obtiene la lista de fotos extra como string separado por ;
     */
    fun obtenerFotosExtraString(equipoId: String): String {
        val rutas = obtenerRutasFotos(equipoId)
        return rutas.entries
            .filter { it.key.startsWith(TIPO_EXTRA) }
            .map { it.value }
            .joinToString(";")
    }
    
    /**
     * Guarda la lista de fotos extra como string separado por ;
     */
    fun guardarFotosExtra(equipoId: String, fotosExtraString: String) {
        // Este método guarda la información de fotos extra
        // La implementación real dependerá de cómo quieras almacenar esta información
        // Por ahora, solo logueamos la información
        android.util.Log.d("FotoManager", "Fotos extra guardadas para $equipoId: $fotosExtraString")
    }
    
    /**
     * Marca una foto como subida a Drive (cambia la ruta local por la URL de Drive)
     */
    fun marcarFotoSubidaDrive(rutaLocal: String, urlDrive: String) {
        // Aquí implementarías la lógica para actualizar la BD
        // cambiando la ruta local por la URL de Drive
    }
    
    /**
     * Obtiene las fotos pendientes de subir
     */
    fun obtenerFotosPendientes(): List<String> {
        val dir = File(context.filesDir, "fotos")
        if (!dir.exists()) return emptyList()
        
        return dir.listFiles()
            ?.filter { it.name.endsWith(".jpg") }
            ?.map { it.absolutePath }
            ?: emptyList()
    }
} 
 
 