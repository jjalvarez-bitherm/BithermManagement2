package com.bithermmanagement.camera

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import com.bithermmanagement.multimedia.EditorFotosFragment
import com.bithermmanagement.utils.ImageUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Manager centralizado para manejo de cámara con diferentes tipos de captura
 */
class CameraManager(private val fragment: Fragment) {
    
    companion object {
        const val TIPO_GPS = "GPS"
        const val TIPO_DENUNCIA = "DENUNCIA"
        const val TIPO_NOTA = "NOTA" 
        const val TIPO_PROYECTO = "PROYECTO"
        
        private const val TAG = "CameraManager"
    }
    
    private var currentPhotoPath: String? = null
    private var currentPhotoType: String? = null
    private var takePictureLauncher: ActivityResultLauncher<Intent>? = null
    
    // Callbacks para comunicación
    var onPhotoTaken: ((String, String) -> Unit)? = null
    var onPhotoEdited: ((String, String) -> Unit)? = null
    
    /**
     * Configura el launcher para tomar fotos
     */
    fun setupLauncher(launcher: ActivityResultLauncher<Intent>) {
        takePictureLauncher = launcher
    }
    
    /**
     * Toma una foto del tipo especificado
     */
    fun takePhoto(type: String, equipoId: String? = null) {
        Log.d(TAG, "Tomando foto tipo: $type")
        currentPhotoType = type
        
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val photoFile = createImageFile(type, equipoId)
        currentPhotoPath = photoFile.absolutePath
        
        val photoURI = Uri.fromFile(photoFile)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
        
        takePictureLauncher?.launch(intent)
    }
    
    /**
     * Procesa el resultado de la cámara
     */
    fun handleCameraResult(success: Boolean) {
        if (success && currentPhotoPath != null && currentPhotoType != null) {
            val file = File(currentPhotoPath!!)
            if (file.exists()) {
                Log.d(TAG, "Foto capturada exitosamente: $currentPhotoPath")
                
                // Notificar que se tomó la foto
                onPhotoTaken?.invoke(currentPhotoPath!!, currentPhotoType!!)
                
                // Abrir editor automáticamente
                openPhotoEditor(currentPhotoPath!!, currentPhotoType!!)
            }
        }
    }
    
    /**
     * Abre el editor de fotos
     */
    private fun openPhotoEditor(photoPath: String, photoType: String) {
        Log.d(TAG, "Abriendo editor para foto tipo: $photoType")
        
        val editorFragment = EditorFotosFragment().apply {
            arguments = android.os.Bundle().apply {
                putString("ruta_foto", photoPath)
                putString("tipo_foto", photoType)
                putString("equipo_id", "")
                putBoolean("auto_add_metadata", true)
            }
        }
        
        fragment.parentFragmentManager.beginTransaction()
            .replace(android.R.id.content, editorFragment)
            .addToBackStack("photo_editor")
            .commit()
    }
    
    /**
     * Crea archivo de imagen con nomenclatura por tipo
     */
    private fun createImageFile(type: String, equipoId: String?): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val prefix = equipoId?.let { "${it}_" } ?: ""
        val fileName = "${prefix}${type}_${timeStamp}.jpg"
        
        val storageDir = getStorageDirectoryForType(type)
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
        
        return File(storageDir, fileName)
    }
    
    /**
     * Obtiene el directorio de almacenamiento por tipo
     */
    private fun getStorageDirectoryForType(type: String): File {
        val baseDir = fragment.requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File(baseDir, "WorkCamera/$type")
    }
    
    /**
     * Obtiene todas las fotos de un tipo específico
     */
    fun getPhotosByType(type: String, equipoId: String? = null): List<String> {
        val dir = getStorageDirectoryForType(type)
        if (!dir.exists()) return emptyList()
        
        return dir.listFiles { file ->
            file.name.endsWith(".jpg") && 
            (equipoId == null || file.name.startsWith("${equipoId}_"))
        }?.map { it.absolutePath }?.sortedDescending() ?: emptyList()
    }
    
    /**
     * Obtiene todas las fotos organizadas por tipo
     */
    fun getAllPhotosGrouped(equipoId: String? = null): Map<String, List<String>> {
        return mapOf(
            TIPO_GPS to getPhotosByType(TIPO_GPS, equipoId),
            TIPO_DENUNCIA to getPhotosByType(TIPO_DENUNCIA, equipoId),
            TIPO_NOTA to getPhotosByType(TIPO_NOTA, equipoId),
            TIPO_PROYECTO to getPhotosByType(TIPO_PROYECTO, equipoId)
        )
    }
}