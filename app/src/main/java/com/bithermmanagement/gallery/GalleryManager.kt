package com.bithermmanagement.gallery

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.bithermmanagement.utils.ImageUtils
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Gestor independiente de la galería que maneja todas las operaciones de imágenes
 * de forma aislada del resto del sistema. Si falla, solo afecta a la galería.
 */
class GalleryManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "GalleryManager"
        private var instance: GalleryManager? = null
        private val isInitialized = AtomicBoolean(false)
        
        @JvmStatic
        fun getInstance(context: Context): GalleryManager {
            return instance ?: synchronized(this) {
                instance ?: GalleryManager(context.applicationContext).also { instance = it }
            }
        }
        
        fun isAvailable(): Boolean {
            return isInitialized.get()
        }
    }
    
    init {
        try {
            // Verificar que el directorio base existe
            val baseDir = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), "BithermManagement")
            if (!baseDir.exists()) {
                val created = baseDir.mkdirs()
                if (!created) {
                    Log.e(TAG, "No se pudo crear el directorio base de la galería")
                    isInitialized.set(false)
                } else {
                    isInitialized.set(true)
                }
            } else {
                isInitialized.set(true)
            }
            Log.d(TAG, "GalleryManager inicializado correctamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error inicializando GalleryManager", e)
            isInitialized.set(false)
        }
    }
    
    /**
     * Resultado de una operación de galería
     */
    sealed class GalleryResult<out T> {
        data class Success<T>(val data: T) : GalleryResult<T>()
        data class Error(val message: String, val exception: Exception? = null) : GalleryResult<Nothing>()
        object Cancelled : GalleryResult<Nothing>()
    }
    
    /**
     * Interfaz para callbacks de la galería
     */
    interface GalleryCallback<T> {
        fun onSuccess(result: T)
        fun onError(message: String, exception: Exception? = null)
        fun onCancelled()
    }
    
    /**
     * Crea un archivo de imagen de forma segura
     */
    fun createImageFile(idEquipo: String, tipo: String): GalleryResult<File> {
        return try {
            if (!isInitialized.get()) {
                return GalleryResult.Error("Galería no inicializada")
            }
            val file = ImageUtils.createImageFile(context, idEquipo, tipo)
            GalleryResult.Success(file)
        } catch (e: Exception) {
            Log.e(TAG, "Error creando archivo de imagen", e)
            GalleryResult.Error("Error creando archivo: "+e.message, e)
        }
    }
    
    /**
     * Guarda una imagen de forma segura
     */
    fun saveImage(bitmap: Bitmap, file: File): GalleryResult<Boolean> {
        return try {
            if (!isInitialized.get()) {
                return GalleryResult.Error("Galería no inicializada")
            }
            ImageUtils.saveBitmapToFile(context, bitmap, file)
            GalleryResult.Success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando imagen", e)
            GalleryResult.Error("Error guardando imagen: "+e.message, e)
        }
    }
    
    /**
     * Carga una imagen de forma segura
     */
    fun loadImage(file: File): GalleryResult<Bitmap> {
        return try {
            if (!isInitialized.get()) {
                return GalleryResult.Error("Galería no inicializada")
            }
            val bitmap = ImageUtils.loadBitmapFromFile(file)
            if (bitmap != null) {
                GalleryResult.Success(bitmap)
            } else {
                GalleryResult.Error("No se pudo cargar la imagen")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cargando imagen", e)
            GalleryResult.Error("Error cargando imagen: "+e.message, e)
        }
    }
    
    /**
     * Obtiene todas las imágenes de un equipo de forma segura
     */
    fun getAllImagesForEquipo(idEquipo: String, tipo: String): GalleryResult<List<String>> {
        return try {
            if (!isInitialized.get()) {
                return GalleryResult.Error("Galería no inicializada")
            }
            val images = ImageUtils.getAllImagesForEquipo(context, idEquipo, tipo)
            GalleryResult.Success(images)
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo imágenes del equipo", e)
            GalleryResult.Error("Error obteniendo imágenes: "+e.message, e)
        }
    }
    
    /**
     * Elimina una imagen de forma segura
     */
    fun deleteImage(imagePath: String): GalleryResult<Boolean> {
        return try {
            if (!isInitialized.get()) {
                return GalleryResult.Error("Galería no inicializada")
            }
            
            val file = File(imagePath)
            if (file.exists() && file.delete()) {
                GalleryResult.Success(true)
            } else {
                GalleryResult.Error("No se pudo eliminar la imagen")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error eliminando imagen", e)
            GalleryResult.Error("Error eliminando imagen: ${e.message}", e)
        }
    }
    
    /**
     * Marca una imagen como favorita de forma segura
     */
    fun markAsFavorite(imagePath: String, area: String, unidad: String, idEquipo: String): GalleryResult<String> {
        return try {
            if (!isInitialized.get()) {
                return GalleryResult.Error("Galería no inicializada")
            }
            
            val originalFile = File(imagePath)
            if (!originalFile.exists()) {
                return GalleryResult.Error("La imagen original no existe")
            }
            
            val carpeta = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), "BithermManagement/$area/$unidad")
            if (!carpeta.exists()) {
                carpeta.mkdirs()
            }
            
            val nombreFav = "${idEquipo}_fav.jpg"
            val archivoFav = File(carpeta, nombreFav)
            
            // Si ya hay un favorito, quitarle el sufijo
            carpeta.listFiles { f -> f.name == nombreFav }?.forEach { favFile ->
                val nombreSinFav = favFile.name.removeSuffix("_fav.jpg") + ".jpg"
                val nuevoArchivo = File(carpeta, nombreSinFav)
                favFile.renameTo(nuevoArchivo)
            }
            
            // Renombrar la imagen seleccionada a _fav
            if (originalFile.absolutePath != archivoFav.absolutePath) {
                if (originalFile.parentFile?.absolutePath != carpeta.absolutePath) {
                    // Si la imagen está fuera de la carpeta, muévela
                    originalFile.copyTo(archivoFav, overwrite = true)
                    originalFile.delete()
                } else {
                    originalFile.renameTo(archivoFav)
                }
            }
            
            GalleryResult.Success(archivoFav.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, "Error marcando como favorita", e)
            GalleryResult.Error("Error marcando como favorita: ${e.message}", e)
        }
    }
    
    /**
     * Verifica si la galería está funcionando correctamente
     */
    fun isGalleryWorking(): Boolean {
        return try {
            isInitialized.get() && 
            context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)?.exists() == true
        } catch (e: Exception) {
            Log.e(TAG, "Error verificando estado de la galería", e)
            false
        }
    }
    
    /**
     * Limpia recursos de la galería
     */
    fun cleanup() {
        try {
            // Aquí podrías limpiar cache, etc.
            Log.d(TAG, "Galería limpiada")
        } catch (e: Exception) {
            Log.e(TAG, "Error limpiando galería", e)
        }
    }
} 