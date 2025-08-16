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

@Singleton
class GoogleDriveHelper @Inject constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "GoogleDriveHelper"
        private const val FOLDER_NAME = "BithermInspection"
        private const val MIME_TYPE_IMAGE = "image/jpeg"
    }

    private fun getDriveService(): Drive? {
        return try {
            // Usar credenciales de servicio para Drive
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

    suspend fun uploadPhoto(localFilePath: String, itemId: String): String? = withContext(Dispatchers.IO) {
        try {
            val driveService = getDriveService() ?: return@withContext null
            
            // Crear o obtener la carpeta de inspección
            val folderId = getOrCreateFolder(driveService, FOLDER_NAME)
            
            // Crear el archivo en Drive
            val fileMetadata = File().apply {
                name = "INSPECTION_${itemId}_${System.currentTimeMillis()}.jpg"
                parents = listOf(folderId)
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
} 