package com.bithermmanagement.chat.services

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ChatFileManager(private val context: Context) {
    
    companion object {
        private const val TAG = "ChatFileManager"
        private const val CHAT_FILES_DIR = "chat_files"
        private const val IMAGES_DIR = "images"
        private const val DRAWINGS_DIR = "drawings"
        private const val AUDIO_DIR = "audio"
        private const val VIDEO_DIR = "video"
        private const val THUMBNAILS_DIR = "thumbnails"
        
        private const val MAX_FILE_SIZE = 50 * 1024 * 1024 // 50MB
        private const val MAX_IMAGE_SIZE = 10 * 1024 * 1024 // 10MB
        private const val THUMBNAIL_SIZE = 200 // 200px
    }
    
    private val chatFilesDir: File
    private val imagesDir: File
    private val drawingsDir: File
    private val audioDir: File
    private val videoDir: File
    private val thumbnailsDir: File
    
    init {
        chatFilesDir = File(context.filesDir, CHAT_FILES_DIR)
        imagesDir = File(chatFilesDir, IMAGES_DIR)
        drawingsDir = File(chatFilesDir, DRAWINGS_DIR)
        audioDir = File(chatFilesDir, AUDIO_DIR)
        videoDir = File(chatFilesDir, VIDEO_DIR)
        thumbnailsDir = File(chatFilesDir, THUMBNAILS_DIR)
        
        createDirectories()
    }
    
    private fun createDirectories() {
        chatFilesDir.mkdirs()
        imagesDir.mkdirs()
        drawingsDir.mkdirs()
        audioDir.mkdirs()
        videoDir.mkdirs()
        thumbnailsDir.mkdirs()
    }
    
    /**
     * Guarda una imagen desde Uri
     */
    suspend fun saveImageFromUri(uri: Uri, chatId: String): String? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "img_${chatId}_${timestamp}.jpg"
            val imageFile = File(imagesDir, fileName)
            
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(imageFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            // Crear miniatura
            createThumbnail(imageFile, fileName)
            
            imageFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando imagen", e)
            null
        }
    }
    
    /**
     * Guarda un dibujo como imagen
     */
    suspend fun saveDrawing(bitmap: Bitmap, chatId: String): String? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "draw_${chatId}_${timestamp}.png"
            val drawingFile = File(drawingsDir, fileName)
            
            FileOutputStream(drawingFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            
            // Crear miniatura
            createThumbnail(drawingFile, fileName)
            
            drawingFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando dibujo", e)
            null
        }
    }
    
    /**
     * Guarda un archivo de audio
     */
    suspend fun saveAudioFile(uri: Uri, chatId: String): String? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "audio_${chatId}_${timestamp}.mp3"
            val audioFile = File(audioDir, fileName)
            
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(audioFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            audioFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando audio", e)
            null
        }
    }
    
    /**
     * Guarda un archivo de video
     */
    suspend fun saveVideoFile(uri: Uri, chatId: String): String? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "video_${chatId}_${timestamp}.mp4"
            val videoFile = File(videoDir, fileName)
            
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(videoFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            // Crear miniatura del video (primer frame)
            createVideoThumbnail(videoFile, fileName)
            
            videoFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando video", e)
            null
        }
    }
    
    /**
     * Crea una miniatura de una imagen
     */
    private fun createThumbnail(imageFile: File, fileName: String): String? {
        return try {
            val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
            val thumbnailBitmap = Bitmap.createScaledBitmap(bitmap, THUMBNAIL_SIZE, THUMBNAIL_SIZE, true)
            
            val thumbnailFile = File(thumbnailsDir, "thumb_$fileName")
            FileOutputStream(thumbnailFile).use { out ->
                thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }
            
            thumbnailFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error creando miniatura", e)
            null
        }
    }
    
    /**
     * Crea una miniatura de un video (primer frame)
     */
    private fun createVideoThumbnail(videoFile: File, fileName: String): String? {
        // TODO: Implementar extracción del primer frame del video
        // Por ahora retornamos null
        return null
    }
    
    /**
     * Obtiene el tamaño de un archivo
     */
    fun getFileSize(filePath: String): Long {
        return try {
            File(filePath).length()
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo tamaño del archivo", e)
            0L
        }
    }
    
    /**
     * Obtiene el tipo MIME de un archivo
     */
    fun getMimeType(filePath: String): String {
        return try {
            val extension = filePath.substringAfterLast('.', "").lowercase()
            when (extension) {
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "gif" -> "image/gif"
                "mp3" -> "audio/mpeg"
                "mp4" -> "video/mp4"
                "pdf" -> "application/pdf"
                "doc" -> "application/msword"
                "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                else -> "application/octet-stream"
            }
        } catch (e: Exception) {
            "application/octet-stream"
        }
    }
    
    /**
     * Elimina un archivo
     */
    fun deleteFile(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error eliminando archivo", e)
            false
        }
    }
    
    /**
     * Limpia archivos antiguos (más de X días)
     */
    fun cleanupOldFiles(daysOld: Int) {
        try {
            val cutoffTime = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L)
            
            listOf(imagesDir, drawingsDir, audioDir, videoDir, thumbnailsDir).forEach { dir ->
                dir.listFiles()?.forEach { file ->
                    if (file.lastModified() < cutoffTime) {
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error limpiando archivos antiguos", e)
        }
    }
    
    /**
     * Obtiene el espacio total usado por los archivos del chat
     */
    fun getTotalStorageUsed(): Long {
        return try {
            var totalSize = 0L
            listOf(imagesDir, drawingsDir, audioDir, videoDir, thumbnailsDir).forEach { dir ->
                dir.listFiles()?.forEach { file ->
                    totalSize += file.length()
                }
            }
            totalSize
        } catch (e: Exception) {
            Log.e(TAG, "Error calculando espacio usado", e)
            0L
        }
    }
}
