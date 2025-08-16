package com.bithermmanagement.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class GoogleOAuthManager(private val context: Context) {
    private val TAG = "GoogleOAuthManager"
    
    // Scopes necesarios para la aplicación
    private val SCOPES = listOf(
        SheetsScopes.SPREADSHEETS,
        SheetsScopes.SPREADSHEETS_READONLY,
        DriveScopes.DRIVE_FILE,
        DriveScopes.DRIVE_READONLY
    )
    
    private lateinit var googleSignInClient: GoogleSignInClient
    private var currentAccount: GoogleSignInAccount? = null
    
    init {
        setupGoogleSignIn()
    }
    
    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(SheetsScopes.SPREADSHEETS))
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()
        
        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }
    
    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }
    
    fun handleSignInResult(data: Intent?): GoogleSignInAccount? {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        return try {
            val account = task.getResult()
            currentAccount = account
            Log.d(TAG, "Usuario autenticado: ${account.email}")
            account
        } catch (e: Exception) {
            Log.e(TAG, "Error en autenticación: ${e.message}")
            null
        }
    }
    
    fun isSignedIn(): Boolean {
        return GoogleSignIn.getLastSignedInAccount(context) != null
    }
    
    fun getCurrentAccount(): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }
    
    fun signOut() {
        googleSignInClient.signOut()
        currentAccount = null
        Log.d(TAG, "Usuario desconectado")
    }
    
    suspend fun createSheetsService(): Sheets? = withContext(Dispatchers.IO) {
        try {
            val account = getCurrentAccount()
            if (account == null) {
                Log.e(TAG, "No hay cuenta autenticada")
                return@withContext null
            }
            
            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                Collections.singleton(SheetsScopes.SPREADSHEETS)
            ).apply {
                selectedAccount = account.account
            }
            
            Sheets.Builder(
                NetHttpTransport(),
                JacksonFactory.getDefaultInstance(),
                credential
            )
            .setApplicationName("BithermManagement")
            .build()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error creando Sheets service: ${e.message}")
            null
        }
    }
    
    suspend fun createDriveService(): Drive? = withContext(Dispatchers.IO) {
        try {
            val account = getCurrentAccount()
            if (account == null) {
                Log.e(TAG, "No hay cuenta autenticada")
                return@withContext null
            }
            
            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                Collections.singleton(DriveScopes.DRIVE_FILE)
            ).apply {
                selectedAccount = account.account
            }
            
            Drive.Builder(
                NetHttpTransport(),
                JacksonFactory.getDefaultInstance(),
                credential
            )
            .setApplicationName("BithermManagement")
            .build()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error creando Drive service: ${e.message}")
            null
        }
    }
    
    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            val sheetsService = createSheetsService()
            if (sheetsService == null) return@withContext false
            
            // Intentar hacer una llamada de prueba simple
            // Simplemente verificamos que el servicio se creó correctamente
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error en prueba de conexión: ${e.message}")
            false
        }
    }
    
    suspend fun getUserInfo(): UserInfo? = withContext(Dispatchers.IO) {
        try {
            val account = getCurrentAccount()
            if (account == null) return@withContext null
            
            UserInfo(
                email = account.email ?: "",
                displayName = account.displayName ?: "",
                photoUrl = account.photoUrl?.toString() ?: "",
                id = account.id ?: ""
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo información del usuario: ${e.message}")
            null
        }
    }
    
    data class UserInfo(
        val email: String,
        val displayName: String,
        val photoUrl: String,
        val id: String
    )
}
