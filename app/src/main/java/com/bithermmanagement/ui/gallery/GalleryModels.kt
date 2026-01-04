package com.bithermmanagement.ui.gallery

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// Data class para representar una foto individual
data class GalleryPhoto(
    val file: File,
    val timestamp: Long = file.lastModified(),
    val type: String = "",
    val isSubfolder: Boolean = false
)

// Data class para representar una carpeta de fotos con información de layout
data class GalleryFolder(
    val name: String,
    val photos: List<GalleryPhoto>,
    val coverPhoto: GalleryPhoto? = photos.firstOrNull { !it.isSubfolder }, // Solo fotos, no subcarpetas
    val spanSizeX: Int = 1, // Ancho en columnas (1 o 2)
    val spanSizeY: Int = 1, // Alto en filas (siempre 1 para este caso)
    val isUnderConstruction: Boolean = false
)

// Utilidad para escanear fotos del sistema de archivos de la aplicación únicamente
object PhotoScanner {
    
    suspend fun scanAppPhotos(context: Context): List<GalleryFolder> = withContext(Dispatchers.IO) {
        val folders = mutableMapOf<String, MutableList<GalleryPhoto>>()
        android.util.Log.d("PhotoScanner", "=== INICIANDO ESCANEO DE FOTOS ===")
        
        // Solo carpetas internas de la app - NO acceder a galería del dispositivo
        val appDirs = listOf(
            File(context.filesDir, "photos"),
            File(context.filesDir, "proyectos"),
            File(context.filesDir, "purgadores"),
            File(context.filesDir, "denuncias"),
            File(context.filesDir, "gps_fotos"),
            File(context.filesDir, "foto_notas"),
            File(context.filesDir, "monitorizacion"),
            // Directorios externos solo para fotos no-proyecto
            File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), "WorkCamera/GPS"),
            File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), "WorkCamera/DENUNCIA"),
            File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), "WorkCamera/NOTA")
        )
        
        appDirs.forEach { dir ->
            if (dir.exists() && dir.isDirectory) {
                android.util.Log.d("PhotoScanner", "Escaneando directorio: ${dir.absolutePath}")
                scanDirectory(dir, folders)
            } else {
                android.util.Log.d("PhotoScanner", "Directorio NO EXISTE o no es directorio: ${dir.absolutePath}")
            }
        }
        
        android.util.Log.d("PhotoScanner", "Fotos encontradas hasta ahora: ${folders.size} carpetas")
        folders.forEach { (key, value) ->
            android.util.Log.d("PhotoScanner", "$key: ${value.size} elementos")
        }
        
        // Agregar subcarpetas de todas las carpetas relevantes
        addSubfolders(File(context.filesDir, "proyectos"), "PROYECTOS", folders)
        addSubfolders(File(context.filesDir, "purgadores"), "PURGADORES", folders)
        addSubfolders(File(context.filesDir, "foto_notas"), "FOTO NOTA", folders)
        addSubfolders(File(context.filesDir, "monitorizacion"), "MONITORIZACIÓN", folders)
        
        // Crear estructura de carpetas predefinida con layout específico
        val result = createPredefinedFolders(folders)
        android.util.Log.d("PhotoScanner", "=== RESULTADO FINAL ===")
        result.forEach { folder ->
            android.util.Log.d("PhotoScanner", "${folder.name}: ${folder.photos.size} elementos (${folder.photos.count { it.isSubfolder }} subcarpetas)")
        }
        result
    }
    
    private fun createPredefinedFolders(scannedFolders: MutableMap<String, MutableList<GalleryPhoto>>): List<GalleryFolder> {
        return listOf(
            // Fila 1: DENUNCIAS (1x2 - ancho completo)
            GalleryFolder(
                name = "DENUNCIAS",
                photos = scannedFolders.getOrElse("DENUNCIAS") { mutableListOf() },
                spanSizeX = 2,
                spanSizeY = 1
            ),
            
            // Fila 2: GPS (1x1) y FOTO NOTA (1x1)
            GalleryFolder(
                name = "GPS",
                photos = scannedFolders.getOrElse("GPS") { mutableListOf() },
                spanSizeX = 1,
                spanSizeY = 1
            ),
            GalleryFolder(
                name = "FOTO NOTA",
                photos = scannedFolders.getOrElse("FOTO NOTA") { mutableListOf() },
                spanSizeX = 1,
                spanSizeY = 1
            ),
            
            // Fila 3: PROYECTOS (1x2 - ancho completo)
            GalleryFolder(
                name = "PROYECTOS",
                photos = scannedFolders.getOrElse("PROYECTOS") { mutableListOf() },
                spanSizeX = 2,
                spanSizeY = 1
            ),
            
            // Fila 4: PURGADORES (1x1) y MONITORIZACIÓN (1x1)
            GalleryFolder(
                name = "PURGADORES",
                photos = scannedFolders.getOrElse("PURGADORES") { mutableListOf() },
                spanSizeX = 1,
                spanSizeY = 1
            ),
            GalleryFolder(
                name = "MONITORIZACIÓN",
                photos = scannedFolders.getOrElse("MONITORIZACIÓN") { mutableListOf() },
                spanSizeX = 1,
                spanSizeY = 1,
                isUnderConstruction = true
            )
        )
    }
    
    private fun addSubfolders(dir: File, parentType: String, folders: MutableMap<String, MutableList<GalleryPhoto>>) {
        android.util.Log.d("PhotoScanner", "Escaneando subcarpetas en: ${dir.absolutePath} para $parentType")
        if (dir.exists() && dir.isDirectory) {
            val subDirs = dir.listFiles()?.filter { it.isDirectory } ?: emptyList()
            android.util.Log.d("PhotoScanner", "Encontradas ${subDirs.size} subcarpetas en $parentType")
            
            subDirs.forEach { subDir ->
                android.util.Log.d("PhotoScanner", "Agregando subcarpeta: ${subDir.name} a $parentType")
                folders.getOrPut(parentType) { mutableListOf() }.add(
                    GalleryPhoto(
                        file = subDir,
                        type = parentType,
                        isSubfolder = true
                    )
                )
                // ✅ FIX: NO escanear recursivamente las subcarpetas aquí
                // Las fotos dentro de subcarpetas solo deben verse al abrir la subcarpeta
                // Si se escanean aquí, aparecen duplicadas en la vista principal
                // scanDirectory(subDir, folders) // ← REMOVIDO para evitar duplicados
            }
        } else {
            android.util.Log.d("PhotoScanner", "Directorio no existe o no es directorio: ${dir.absolutePath}")
        }
    }

    private fun scanDirectory(dir: File, folders: MutableMap<String, MutableList<GalleryPhoto>>) {
        // ✅ FIX: NO escanear recursivamente - solo escanear fotos en el nivel actual
        // Las subcarpetas se manejan explícitamente con addSubfolders()
        dir.listFiles()?.forEach { file ->
            when {
                file.isDirectory -> {
                    // NO recursivo - las subcarpetas se agregan con addSubfolders()
                    android.util.Log.d("PhotoScanner", "Omitiendo subdirectorio: ${file.name}")
                }
                isImageFile(file) -> {
                    val folderName = determineFolderType(file)
                    folders.getOrPut(folderName) { mutableListOf() }.add(
                        GalleryPhoto(
                            file = file, 
                            type = folderName,
                            isSubfolder = false
                        )
                    )
                }
            }
        }
    }
    
    private fun isImageFile(file: File): Boolean {
        val extension = file.extension.lowercase()
        return extension in listOf("jpg", "jpeg", "png", "gif", "bmp", "webp")
    }
    
    private fun determineFolderType(file: File): String {
        val path = file.absolutePath.lowercase()
        val name = file.nameWithoutExtension.lowercase()
        
        return when {
            path.contains("denuncia") || name.contains("denuncia") -> "DENUNCIAS"
            path.contains("gps") || name.contains("gps") -> "GPS"
            path.contains("proyecto") || name.contains("proyecto") -> "PROYECTOS"
            path.contains("nota") || name.contains("nota") || name.contains("fotonota") -> "FOTO NOTA"
            path.contains("purgador") || name.contains("purgador") -> "PURGADORES"
            path.contains("monitoriz") || name.contains("monitoriz") -> "MONITORIZACIÓN"
            // Detectar por ruta del WorkCamera
            path.contains("workcamera/denuncias") -> "DENUNCIAS"
            path.contains("workcamera/gps") -> "GPS"
            path.contains("workcamera/nota") -> "FOTO NOTA"
            path.contains("workcamera/proyecto") -> "PROYECTOS"
            path.contains("workcamera/purgador") -> "PURGADORES"
            path.contains("workcamera/monitor") -> "MONITORIZACIÓN"
            else -> "DENUNCIAS" // Por defecto asignar a denuncias
        }
    }
}