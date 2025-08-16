package com.bithermmanagement.data

import android.content.Context
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import java.io.InputStream
import com.google.api.services.drive.model.File
import java.io.File as JavaFile
import java.io.FileInputStream
import com.google.api.client.http.FileContent
import java.util.*

class GoogleDriveManager(
    private val credentialsStream: InputStream?, 
    private val context: Context,
    private val useOAuth: Boolean = false,
    private val oAuthEmail: String = ""
) {
    private val driveService: Drive

    init {
        val credentials = if (useOAuth) {
            // Para OAuth, usar GoogleAccountCredential
            val oAuthManager = GoogleOAuthManager(context)
            if (!oAuthManager.isSignedIn()) {
                throw IllegalStateException("Usuario no autenticado con Google")
            }
            val account = oAuthManager.getCurrentAccount()
            if (account?.email != oAuthEmail) {
                throw IllegalStateException("Email no coincide con la cuenta autenticada")
            }
            
            GoogleAccountCredential.usingOAuth2(
                context,
                Collections.singleton(DriveScopes.DRIVE_READONLY)
            ).apply {
                selectedAccount = account.account
            }
        } else {
            // Para Service Account, usar credenciales del archivo
            if (credentialsStream == null) {
                throw IllegalStateException("Se requieren credenciales para cuenta de servicio")
            }
            GoogleCredential.fromStream(credentialsStream)
                .createScoped(listOf(DriveScopes.DRIVE_READONLY))
        }
        
        driveService = Drive.Builder(
            NetHttpTransport(),
            JacksonFactory.getDefaultInstance(),
            credentials
        )
            .setApplicationName("BithermManagement")
            .build()
    }

    data class SpreadsheetInfo(val id: String, val name: String)

    suspend fun listarSpreadsheetsApp(): List<SpreadsheetInfo> {
        val result = mutableListOf<SpreadsheetInfo>()
        val query = "mimeType='application/vnd.google-apps.spreadsheet' and name contains '(APP)' and trashed=false"
        var pageToken: String? = null
        do {
            val request = driveService.files().list()
                .setQ(query)
                .setFields("nextPageToken, files(id, name)")
                .setPageToken(pageToken)
            val files = request.execute().files
            if (files != null) {
                for (file in files) {
                    if (file.name.endsWith("(APP)")) {
                        result.add(SpreadsheetInfo(file.id, file.name))
                    }
                }
            }
            pageToken = request.execute().nextPageToken
        } while (pageToken != null)
        return result
    }

    suspend fun subirFotoAPurgadores(localPath: String, fileName: String, folderId: String = "13gCt4wtoL3SjVMioMgU6tu1KY6A8BXj_"): Pair<String, String>? {
        return try {
            val fileMetadata = File()
            fileMetadata.name = fileName
            fileMetadata.parents = listOf(folderId)
            val filePath = JavaFile(localPath)
            val mediaContent = FileContent("image/jpeg", filePath)
            val file = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute()
            val fileId = file.id
            // Hacer el archivo público
            val permission = com.google.api.services.drive.model.Permission()
            permission.type = "anyone"
            permission.role = "reader"
            driveService.permissions().create(fileId, permission).execute()
            val url = "https://drive.google.com/uc?id=$fileId"
            Pair(fileId, url)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun listarFotosPurgadores(folderId: String = "13gCt4wtoL3SjVMioMgU6tu1KY6A8BXj_"): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        val query = "'$folderId' in parents and mimeType contains 'image/' and trashed=false"
        var pageToken: String? = null
        do {
            val request = driveService.files().list()
                .setQ(query)
                .setFields("nextPageToken, files(id, name)")
                .setPageToken(pageToken)
            val files = request.execute().files
            if (files != null) {
                for (file in files) {
                    result.add(Pair(file.name, "https://drive.google.com/uc?id=${file.id}"))
                }
            }
            pageToken = request.execute().nextPageToken
        } while (pageToken != null)
        return result
    }
} 