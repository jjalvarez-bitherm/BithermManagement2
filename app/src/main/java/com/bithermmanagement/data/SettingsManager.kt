package com.bithermmanagement.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import java.io.File

class SettingsManager(private val context: Context) {
    private val TAG = "SettingsManager"
    private val prefs: SharedPreferences = context.getSharedPreferences("bitherm_settings", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    data class Settings(
        val projectId: String = "",
        val serviceAccountEmail: String = "",
        val spreadsheetId: String = "",
        val driveFolderId: String = "",
        val companyName: String = "",
        val workStartHour: Int = 8,
        val workStartMinute: Int = 0,
        val workEndHour: Int = 18,
        val workEndMinute: Int = 0,
        val primaryColor: String = "#2196F3",
        val secondaryColor: String = "#FFC107",
        val useOAuth: Boolean = false, // Por defecto usar cuenta de servicio
        val oAuthEmail: String = "",
        val useDefaultCredentials: Boolean = true, // Por defecto usar credenciales por defecto
        val customCredentialsFile: String = "" // Ruta al archivo de credenciales personalizado
    )
    
    fun getSettings(): Settings {
        val settingsJson = prefs.getString("settings", null)
        return if (settingsJson != null) {
            try {
                val settings = gson.fromJson(settingsJson, Settings::class.java)
                Log.d(TAG, "getSettings: Configuración cargada desde SharedPreferences - useOAuth=${settings.useOAuth}, oAuthEmail='${settings.oAuthEmail}'")
                settings
            } catch (e: Exception) {
                Log.e(TAG, "Error al cargar configuración: ${e.message}")
                Settings()
            }
        } else {
            // Configuración por defecto
            val defaultSettings = Settings(
                projectId = "bithermmanagement-469102",
                serviceAccountEmail = "bitherm-management@bithermmanagement-469102.iam.gserviceaccount.com",
                spreadsheetId = "1IyWGyxYDDTWY5SHh2xLBxtakSZX_xhZFo2jta4JeSW4",
                driveFolderId = "",
                companyName = "Bitherm Management",
                useOAuth = false, // Usar cuenta de servicio por defecto (no OAuth)
                useDefaultCredentials = true
            )
            Log.d(TAG, "getSettings: Usando configuración por defecto - useOAuth=${defaultSettings.useOAuth}, oAuthEmail='${defaultSettings.oAuthEmail}'")
            defaultSettings
        }
    }
    
    fun saveSettings(settings: Settings) {
        try {
            val settingsJson = gson.toJson(settings)
            prefs.edit().putString("settings", settingsJson).apply()
            Log.d(TAG, "saveSettings: Configuración guardada correctamente - useOAuth=${settings.useOAuth}, oAuthEmail='${settings.oAuthEmail}'")
        } catch (e: Exception) {
            Log.e(TAG, "Error al guardar configuración: ${e.message}")
            throw e
        }
    }
    
    fun resetSettings() {
        prefs.edit().clear().apply()
        
        // Eliminar archivos de credenciales y logo
        val credentialsFile = File(context.filesDir, "credentials.json")
        val logoFile = File(context.filesDir, "company_logo.png")
        
        if (credentialsFile.exists()) {
            credentialsFile.delete()
        }
        
        if (logoFile.exists()) {
            logoFile.delete()
        }
        
        Log.d(TAG, "Configuración restablecida")
    }
    
    fun getCredentialsFile(): File? {
        val file = File(context.filesDir, "credentials.json")
        return if (file.exists()) file else null
    }
    
    fun getCompanyLogoFile(): File? {
        val file = File(context.filesDir, "company_logo.png")
        return if (file.exists()) file else null
    }
    
    fun updateCredentialsFile(inputStream: java.io.InputStream) {
        val credentialsFile = File(context.filesDir, "credentials.json")
        inputStream.use { input ->
            credentialsFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        Log.d(TAG, "Archivo de credenciales actualizado")
    }
    
    fun updateCompanyLogo(inputStream: java.io.InputStream) {
        val logoFile = File(context.filesDir, "company_logo.png")
        inputStream.use { input ->
            logoFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        Log.d(TAG, "Logo de empresa actualizado")
    }
    
    fun getDefaultCredentialsFile(): File? {
        return try {
            val inputStream = context.assets.open("credentials_default.json")
            val tempFile = File(context.filesDir, "credentials_default_temp.json")
            inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener credenciales por defecto: ${e.message}")
            null
        }
    }
    
    fun getCredentialsInputStream(): java.io.InputStream? {
        return try {
            val settings = getSettings()
            Log.d(TAG, "getCredentialsInputStream: useOAuth=${settings.useOAuth}, useDefaultCredentials=${settings.useDefaultCredentials}")
            
            if (settings.useOAuth) {
                // Para OAuth, no necesitamos credenciales de servicio
                Log.d(TAG, "OAuth configurado - no se requieren credenciales de servicio")
                null
            } else if (settings.useDefaultCredentials) {
                // Usar credenciales por defecto
                Log.d(TAG, "Usando credenciales por defecto desde assets")
                context.assets.open("credentials_default.json")
            } else {
                // Usar credenciales personalizadas
                val customFile = File(context.filesDir, "credentials.json")
                if (customFile.exists()) {
                    Log.d(TAG, "Usando credenciales personalizadas desde filesDir")
                    customFile.inputStream()
                } else {
                    // Fallback a credenciales por defecto
                    Log.d(TAG, "No hay credenciales personalizadas, usando por defecto")
                    context.assets.open("credentials_default.json")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener credenciales: ${e.message}")
            Log.e(TAG, "Stack trace: ${e.stackTraceToString()}")
            null
        }
    }
    
    fun hasCustomCredentials(): Boolean {
        val customFile = File(context.filesDir, "credentials.json")
        return customFile.exists()
    }
    
    fun clearCustomCredentials() {
        val customFile = File(context.filesDir, "credentials.json")
        if (customFile.exists()) {
            customFile.delete()
            Log.d(TAG, "Credenciales personalizadas eliminadas")
        }
    }
    

    
    /**
     * Obtiene un GoogleSheetsManager configurado según la configuración actual
     * (OAuth o cuenta de servicio)
     */
    fun getGoogleSheetsManager(): GoogleSheetsManager? {
        return try {
            val settings = getSettings()
            val credentialsStream = getCredentialsInputStream()
            
            Log.d(TAG, "getGoogleSheetsManager: useOAuth=${settings.useOAuth}, oAuthEmail='${settings.oAuthEmail}', credentialsStream=${if (credentialsStream != null) "disponible" else "null"}")
            
            // Crear el adapter de autenticación
            val authAdapter = GoogleAuthAdapter(
                context = context,
                useOAuth = settings.useOAuth,
                oAuthEmail = settings.oAuthEmail,
                credentialsStream = credentialsStream
            )
            
            Log.d(TAG, "getGoogleSheetsManager: GoogleAuthAdapter creado exitosamente")
            
            GoogleSheetsManager(
                authAdapter = authAdapter,
                context = context
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error al crear GoogleSheetsManager: ${e.message}")
            null
        }
    }
}
