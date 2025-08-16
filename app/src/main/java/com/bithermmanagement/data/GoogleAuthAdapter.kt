package com.bithermmanagement.data

import android.content.Context
import android.util.Log
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import java.io.InputStream
import java.util.*

/**
 * Adapter para manejar autenticación de Google APIs
 * Soporta tanto OAuth 2.0 como Service Account
 */
class GoogleAuthAdapter(
    private val context: Context,
    private val useOAuth: Boolean,
    private val oAuthEmail: String,
    private val credentialsStream: InputStream?
) {
    private val oAuthManager = GoogleOAuthManager(context)
    private val TAG = "GoogleAuthAdapter"
    
    /**
     * Crea las credenciales según el tipo de autenticación configurado
     */
    fun createCredentials(): HttpRequestInitializer {
        Log.d(TAG, "createCredentials: useOAuth=$useOAuth, oAuthEmail='$oAuthEmail'")
        return if (useOAuth) {
            createOAuthCredentials()
        } else {
            createServiceAccountCredentials()
        }
    }
    
    /**
     * Crea credenciales OAuth 2.0
     */
    private fun createOAuthCredentials(): HttpRequestInitializer {
        Log.d(TAG, "createOAuthCredentials: Creando credenciales OAuth 2.0 para: '$oAuthEmail'")
        
        if (oAuthEmail.isEmpty()) {
            Log.e(TAG, "createOAuthCredentials: oAuthEmail está vacío")
            throw IllegalStateException("Se requiere email de OAuth para crear credenciales")
        }
        
        return try {
            // Verificar que el usuario esté autenticado con Google
            if (!oAuthManager.isSignedIn()) {
                Log.e(TAG, "createOAuthCredentials: Usuario no está autenticado con Google")
                throw IllegalStateException("Debes autenticarte con Google primero. Ve a Configuración > Conectar con Google")
            }
            
            val currentAccount = oAuthManager.getCurrentAccount()
            if (currentAccount == null) {
                Log.e(TAG, "createOAuthCredentials: No se puede obtener la cuenta actual")
                throw IllegalStateException("No se puede obtener la cuenta de Google autenticada")
            }
            
            // Verificar que el email coincida
            if (currentAccount.email != oAuthEmail) {
                Log.e(TAG, "createOAuthCredentials: Email no coincide. Esperado: '$oAuthEmail', Actual: '${currentAccount.email}'")
                throw IllegalStateException("El email configurado no coincide con la cuenta autenticada")
            }
            
            // Crear credenciales OAuth 2.0 reales usando GoogleAccountCredential
            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                Collections.singleton(SheetsScopes.SPREADSHEETS)
            ).apply {
                selectedAccount = currentAccount.account
            }
            
            Log.d(TAG, "createOAuthCredentials: Credenciales OAuth 2.0 creadas exitosamente para: '${currentAccount.email}'")
            Log.d(TAG, "createOAuthCredentials: Account: ${currentAccount.account}, DisplayName: ${currentAccount.displayName}")
            
            credential
            
        } catch (e: Exception) {
            Log.e(TAG, "createOAuthCredentials: Error al crear credenciales OAuth 2.0: ${e.message}")
            Log.e(TAG, "Stack trace: ${e.stackTraceToString()}")
            throw IllegalStateException("Error al crear credenciales OAuth 2.0: ${e.message}")
        }
    }
    
    /**
     * Crea credenciales de Service Account
     */
    private fun createServiceAccountCredentials(): HttpRequestInitializer {
        Log.d(TAG, "createServiceAccountCredentials: Creando credenciales de Service Account")
        
        if (credentialsStream == null) {
            Log.e(TAG, "createServiceAccountCredentials: credentialsStream es null")
            throw IllegalStateException("Se requieren credenciales para cuenta de servicio")
        }
        
        return try {
            val creds = GoogleCredential.fromStream(credentialsStream)
            Log.d(TAG, "Credenciales de Service Account cargadas exitosamente")
            Log.d(TAG, "Client Email: ${creds.serviceAccountId}")
            Log.d(TAG, "Project ID: ${creds.serviceAccountProjectId}")
            
            val scopedCreds = creds.createScoped(listOf(SheetsScopes.SPREADSHEETS))
            Log.d(TAG, "Credenciales con scope aplicado exitosamente")
            scopedCreds
        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar credenciales de Service Account: ${e.message}")
            Log.e(TAG, "Stack trace: ${e.stackTraceToString()}")
            throw IllegalStateException("Error en las credenciales de Service Account. Por favor, verifica la configuración.")
        }
    }
    
    /**
     * Crea un servicio de Sheets configurado
     */
    fun createSheetsService(): Sheets {
        Log.d(TAG, "createSheetsService: Iniciando creación de Sheets service...")
        val credentials = createCredentials()
        
        Log.d(TAG, "createSheetsService: Credenciales creadas, construyendo Sheets service...")
        val sheetsService = Sheets.Builder(
            NetHttpTransport(),
            JacksonFactory.getDefaultInstance(),
            credentials
        )
        .setApplicationName("BithermManagement")
        .build()
        
        Log.d(TAG, "createSheetsService: Sheets service creado exitosamente")
        return sheetsService
    }
}
