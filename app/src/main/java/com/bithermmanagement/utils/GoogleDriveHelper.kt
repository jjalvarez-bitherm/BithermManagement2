package com.bithermmanagement.utils

import android.content.Context
import android.util.Log
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import com.bithermmanagement.database.AppDatabase
import com.bithermmanagement.database.entities.Equipo

@Singleton
class GoogleDriveHelper @Inject constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "GoogleDriveHelper"
        private const val FOLDER_NAME = "BithermInspection"
        private const val MIME_TYPE_IMAGE = "image/jpeg"
    }

    private fun getDriveService(): Drive? {
        return try {
            // Intentar usar OAuth primero
            val oAuthService = getOAuthDriveService()
            if (oAuthService != null) {
                Log.d(TAG, "Usando OAuth para Drive service")
                return oAuthService
            }
            
            // Fallback a Service Account
            Log.d(TAG, "Fallback a Service Account para Drive service")
            val inputStream = context.assets.open("credentials_default.json")
            val credentials = GoogleCredentials.fromStream(inputStream)
                .createScoped(listOf(DriveScopes.DRIVE_FILE))

            Drive.Builder(
                NetHttpTransport(),
                GsonFactory(),
                HttpCredentialsAdapter(credentials)
            )
                .setApplicationName("BithermManagement")
                .build()
        } catch (e: Exception) {
            Log.e(TAG, "Error al crear Drive service: ${e.message}")
            null
        }
    }
    
    private fun getOAuthDriveService(): Drive? {
        return try {
            // Obtener GoogleAuthAdapter existente
            val authAdapter = com.bithermmanagement.data.GoogleAuthAdapter(
                context = context,
                useOAuth = true,
                oAuthEmail = "jjalvarez.bitherm@gmail.com",
                credentialsStream = null
            )
            
            val credentials = authAdapter.createCredentials()
            Drive.Builder(
                NetHttpTransport(),
                GsonFactory(),
                credentials
            )
                .setApplicationName("BithermManagement")
                .build()
        } catch (e: Exception) {
            Log.e(TAG, "Error al crear OAuth Drive service: ${e.message}")
            null
        }
    }

    suspend fun uploadPhoto(localFilePath: String, itemId: String, tipoFoto: String = "EQUIPO"): String? = withContext(Dispatchers.IO) {
        try {
            val driveService = getDriveService() ?: return@withContext null
            
            // Obtener información del equipo desde la base de datos
            val equipoInfo = getEquipoInfo(itemId)
            if (equipoInfo == null) {
                Log.e(TAG, "No se pudo obtener información del equipo: $itemId")
                return@withContext null
            }
            
            // Crear estructura de carpetas: UNIDAD/AREA/EQUIPO
            val carpetaEquipo = createFolderStructure(driveService, equipoInfo.unidad, equipoInfo.area, itemId)
            if (carpetaEquipo == null) {
                Log.e(TAG, "No se pudo crear la estructura de carpetas para: $itemId")
                return@withContext null
            }
            
            // Generar nombre del archivo: TAG_tipofoto_fecha.jpg
            val fecha = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
            val nombreArchivo = "${itemId}_${tipoFoto}_${fecha}.jpg"
            
            Log.d(TAG, "Subiendo foto: $nombreArchivo a carpeta: $carpetaEquipo")
            
            // Crear el archivo en Drive
            val fileMetadata = File().apply {
                name = nombreArchivo
                parents = listOf(carpetaEquipo)
                mimeType = MIME_TYPE_IMAGE
            }

            // Leer el archivo local
            val fileInputStream = FileInputStream(localFilePath)
            val fileContent = ByteArrayOutputStream()
            fileInputStream.use { input ->
                input.copyTo(fileContent)
            }

            // Subir el archivo
            val mediaContent = com.google.api.client.http.ByteArrayContent(MIME_TYPE_IMAGE, fileContent.toByteArray())
            val uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id, webViewLink")
                .execute()

            Log.d(TAG, "Foto subida exitosamente: ${uploadedFile.id}")
            return@withContext uploadedFile.id
            
        } catch (e: Exception) {
            Log.e(TAG, "Error al subir foto: ${e.message}")
            return@withContext null
        }
    }

    private fun getOrCreateFolder(driveService: Drive, folderName: String): String {
        return try {
            // Buscar si la carpeta ya existe
            val result = driveService.files().list()
                .setQ("name='$folderName' and mimeType='application/vnd.google-apps.folder' and trashed=false")
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute()

            if (result.files.isNotEmpty()) {
                // La carpeta ya existe, retornar su ID
                result.files[0].id
            } else {
                // Crear nueva carpeta
                val folderMetadata = File().apply {
                    name = folderName
                    mimeType = "application/vnd.google-apps.folder"
                }

                val folder = driveService.files().create(folderMetadata)
                    .setFields("id")
                    .execute()

                Log.d(TAG, "Carpeta creada: ${folder.id}")
                folder.id
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al crear/obtener carpeta: ${e.message}")
            throw e
        }
    }

    suspend fun deletePhoto(fileId: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val driveService = getDriveService() ?: return@withContext false
            driveService.files().delete(fileId).execute()
            Log.d(TAG, "Foto eliminada: $fileId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error al eliminar foto: ${e.message}")
            false
        }
    }

    suspend fun getPhotoUrl(fileId: String): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val driveService = getDriveService() ?: return@withContext null
            val file = driveService.files().get(fileId)
                .setFields("webViewLink")
                .execute()
            file.webViewLink
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener URL de foto: ${e.message}")
            null
        }
    }
    
    /**
     * Obtiene la información del equipo desde la base de datos
     */
    private suspend fun getEquipoInfo(equipoId: String): EquipoInfo? {
        return try {
            val db = AppDatabase.getDatabase(context)
            val equipo = db.inspeccionDao().getEquipoPorId(equipoId)
            if (equipo != null) {
                EquipoInfo(
                    id = equipo.id,
                    unidad = equipo.unidad ?: "SIN_UNIDAD",
                    area = equipo.area ?: "SIN_AREA"
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo información del equipo: ${e.message}")
            null
        }
    }
    
    /**
     * Crea la estructura de carpetas: AREA/UNIDAD/EQUIPO
     */
    private suspend fun createFolderStructure(driveService: Drive, unidad: String, area: String, equipoId: String): String? {
        return try {
            val baseFolderId = "13gCt4wtoL3SjVMioMgU6tu1KY6A8BXj_"
            
            // Crear carpeta AREA
            val carpetaArea = getOrCreateFolder(driveService, area, baseFolderId)
            Log.d(TAG, "Carpeta AREA: $area -> $carpetaArea")
            
            // Crear carpeta UNIDAD dentro de AREA
            val carpetaUnidad = getOrCreateFolder(driveService, unidad, carpetaArea)
            Log.d(TAG, "Carpeta UNIDAD: $unidad -> $carpetaUnidad")
            
            // Crear carpeta EQUIPO dentro de UNIDAD
            val carpetaEquipo = getOrCreateFolder(driveService, equipoId, carpetaUnidad)
            Log.d(TAG, "Carpeta EQUIPO: $equipoId -> $carpetaEquipo")
            
            carpetaEquipo
        } catch (e: Exception) {
            Log.e(TAG, "Error creando estructura de carpetas: ${e.message}")
            null
        }
    }
    
    /**
     * Obtiene o crea una carpeta dentro de una carpeta padre
     */
    private suspend fun getOrCreateFolder(driveService: Drive, folderName: String, parentFolderId: String): String {
        return try {
            // Buscar si la carpeta ya existe
            val result = driveService.files().list()
                .setQ("name='$folderName' and mimeType='application/vnd.google-apps.folder' and '$parentFolderId' in parents and trashed=false")
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute()

            if (result.files.isNotEmpty()) {
                // La carpeta ya existe, retornar su ID
                result.files[0].id
            } else {
                // Crear nueva carpeta
                val folderMetadata = File().apply {
                    name = folderName
                    mimeType = "application/vnd.google-apps.folder"
                    parents = listOf(parentFolderId)
                }
                
                val folder = driveService.files().create(folderMetadata)
                    .setFields("id")
                    .execute()
                
                Log.d(TAG, "Carpeta creada: $folderName -> ${folder.id}")
                folder.id
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener/crear carpeta '$folderName': ${e.message}")
            throw e
        }
    }
    
    /**
     * Data class para información del equipo
     */
    data class EquipoInfo(
        val id: String,
        val unidad: String,
        val area: String
    )
} 