package com.bithermmanagement.ui.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bithermmanagement.R
import com.bithermmanagement.databinding.ActivitySettingsBinding
import com.bithermmanagement.data.SettingsManager
import com.bithermmanagement.data.GoogleSheetsManager
import com.bithermmanagement.data.GoogleOAuthManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var settingsManager: SettingsManager
    private lateinit var oAuthManager: GoogleOAuthManager
    
    private val TAG = "SettingsActivity"
    
    // Launcher para seleccionar archivo de credenciales
    private val selectCredentialsLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleCredentialsFileSelection(it) }
    }
    
    // Launcher para seleccionar logo de empresa
    private val selectLogoLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleLogoSelection(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        settingsManager = SettingsManager(this)
        oAuthManager = GoogleOAuthManager(this)
        
        setupToolbar()
        loadCurrentSettings()
        setupButtons()
        setupOAuthUI()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }
    
    private fun loadCurrentSettings() {
        // Cargar configuración actual
        val settings = settingsManager.getSettings()
        
        binding.etProjectId.setText(settings.projectId)
        binding.etServiceAccountEmail.setText(settings.serviceAccountEmail)
        binding.etSpreadsheetId.setText(settings.spreadsheetId)
        binding.etDriveFolderId.setText(settings.driveFolderId)
        binding.etCompanyName.setText(settings.companyName)
        
        // Configurar método de autenticación
        if (settings.useOAuth) {
            binding.rbOAuth.isChecked = true
        } else {
            binding.rbServiceAccount.isChecked = true
        }
        
        // Configurar opciones de credenciales
        if (settings.useDefaultCredentials) {
            binding.rbDefaultCredentials.isChecked = true
        } else {
            binding.rbCustomCredentials.isChecked = true
        }
        
        // Configurar horarios
        binding.timePickerStart.hour = settings.workStartHour
        binding.timePickerStart.minute = settings.workStartMinute
        binding.timePickerEnd.hour = settings.workEndHour
        binding.timePickerEnd.minute = settings.workEndMinute
        
        // Cargar logo si existe
        loadCompanyLogo()
        
        // Actualizar UI según selección
        updateUIForAuthenticationMethod()
    }
    
    private fun setupButtons() {
        binding.btnUploadCredentials.setOnClickListener {
            selectCredentialsLauncher.launch("application/json")
        }
        
        binding.btnDownloadCredentials.setOnClickListener {
            downloadCurrentCredentials()
        }
        
        binding.btnUploadLogo.setOnClickListener {
            selectLogoLauncher.launch("image/*")
        }
        
        binding.btnSaveSettings.setOnClickListener {
            saveSettings()
        }
        
        binding.btnTestConnection.setOnClickListener {
            testConnection()
        }
        
        binding.btnResetSettings.setOnClickListener {
            resetSettings()
        }
        
        binding.btnClearOAuthSettings.setOnClickListener {
            clearOAuthSettings()
        }
        
        binding.btnConnectGoogle.setOnClickListener {
            connectWithGoogle()
        }
        
        // Configurar listeners para los radio buttons
        binding.rbOAuth.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                updateUIForAuthenticationMethod()
                // Guardar configuración automáticamente
                saveSettings()
            }
        }
        
        binding.rbServiceAccount.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                updateUIForAuthenticationMethod()
                // Guardar configuración automáticamente
                saveSettings()
            }
        }
        
        binding.rbDefaultCredentials.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                updateUIForCredentialsOption()
                // Guardar configuración automáticamente
                saveSettings()
            }
        }
        
        binding.rbCustomCredentials.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                updateUIForCredentialsOption()
                // Guardar configuración automáticamente
                saveSettings()
            }
        }
    }
    
    private fun setupOAuthUI() {
        updateOAuthUI()
    }
    
    private fun updateOAuthUI() {
        if (oAuthManager.isSignedIn()) {
            val account = oAuthManager.getCurrentAccount()
            binding.tvGoogleAccount.text = "Conectado: ${account?.email ?: "Desconocido"}"
            binding.btnConnectGoogle.text = "Desconectar de Google"
        } else {
            binding.tvGoogleAccount.text = "No conectado"
            binding.btnConnectGoogle.text = "Conectar con Google"
        }
    }
    
    private fun updateUIForAuthenticationMethod() {
        if (binding.rbOAuth.isChecked) {
            // Mostrar opciones de OAuth
            binding.layoutServiceAccountOptions.visibility = android.view.View.GONE
            binding.btnConnectGoogle.visibility = android.view.View.VISIBLE
            binding.tvGoogleAccount.visibility = android.view.View.VISIBLE
            binding.btnUploadCredentials.visibility = android.view.View.GONE
            binding.btnDownloadCredentials.visibility = android.view.View.GONE
        } else {
            // Mostrar opciones de cuenta de servicio
            binding.layoutServiceAccountOptions.visibility = android.view.View.VISIBLE
            binding.btnConnectGoogle.visibility = android.view.View.GONE
            binding.tvGoogleAccount.visibility = android.view.View.GONE
            updateUIForCredentialsOption()
        }
    }
    
    private fun updateUIForCredentialsOption() {
        if (binding.rbDefaultCredentials.isChecked) {
            // Usar credenciales por defecto
            binding.btnUploadCredentials.visibility = android.view.View.GONE
            binding.btnDownloadCredentials.visibility = android.view.View.VISIBLE
            binding.btnDownloadCredentials.text = "Descargar credenciales por defecto"
        } else {
            // Usar credenciales personalizadas
            binding.btnUploadCredentials.visibility = android.view.View.VISIBLE
            binding.btnDownloadCredentials.visibility = android.view.View.VISIBLE
            binding.btnDownloadCredentials.text = "Descargar credenciales personalizadas"
        }
    }
    
    private fun connectWithGoogle() {
        if (oAuthManager.isSignedIn()) {
            // Desconectar
            oAuthManager.signOut()
            updateOAuthUI()
            // Guardar configuración automáticamente
            saveSettings()
            Toast.makeText(this, "Desconectado de Google", Toast.LENGTH_SHORT).show()
        } else {
            // Conectar
            val signInIntent = oAuthManager.getSignInIntent()
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }
    }
    
    companion object {
        private const val RC_SIGN_IN = 9001
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == RC_SIGN_IN) {
            val account = oAuthManager.handleSignInResult(data)
            if (account != null) {
                updateOAuthUI()
                // Guardar configuración automáticamente después de conectar
                saveSettings()
                Toast.makeText(this, "Conectado con: ${account.email}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Error al conectar con Google", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun handleCredentialsFileSelection(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val credentialsFile = File(filesDir, "credentials.json")
            
            inputStream?.use { input ->
                FileOutputStream(credentialsFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            Toast.makeText(this, "Archivo de credenciales subido correctamente", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error al subir credenciales: ${e.message}")
            Toast.makeText(this, "Error al subir credenciales: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun handleLogoSelection(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val logoFile = File(filesDir, "company_logo.png")
            
            inputStream?.use { input ->
                FileOutputStream(logoFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            loadCompanyLogo()
            Toast.makeText(this, "Logo subido correctamente", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error al subir logo: ${e.message}")
            Toast.makeText(this, "Error al subir logo: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun loadCompanyLogo() {
        val logoFile = File(filesDir, "company_logo.png")
        if (logoFile.exists()) {
            binding.ivCompanyLogo.setImageURI(Uri.fromFile(logoFile))
        }
    }
    
    private fun downloadCurrentCredentials() {
        try {
            if (binding.rbDefaultCredentials.isChecked) {
                // Descargar credenciales por defecto
                val defaultCredentialsFile = settingsManager.getDefaultCredentialsFile()
                if (defaultCredentialsFile != null) {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(Intent.EXTRA_STREAM, Uri.fromFile(defaultCredentialsFile))
                        putExtra(Intent.EXTRA_SUBJECT, "Credenciales por defecto de Bitherm Management")
                    }
                    startActivity(Intent.createChooser(intent, "Compartir credenciales por defecto"))
                } else {
                    Toast.makeText(this, "Error al obtener credenciales por defecto", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Descargar credenciales personalizadas
                val credentialsFile = File(filesDir, "credentials.json")
                if (credentialsFile.exists()) {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(Intent.EXTRA_STREAM, Uri.fromFile(credentialsFile))
                        putExtra(Intent.EXTRA_SUBJECT, "Credenciales personalizadas de Google Cloud")
                    }
                    startActivity(Intent.createChooser(intent, "Compartir credenciales personalizadas"))
                } else {
                    Toast.makeText(this, "No hay credenciales personalizadas para descargar", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al descargar credenciales: ${e.message}")
            Toast.makeText(this, "Error al descargar credenciales: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun saveSettings() {
        try {
            val useOAuth = binding.rbOAuth.isChecked
            val oAuthEmail = oAuthManager.getCurrentAccount()?.email ?: ""
            
            Log.d(TAG, "saveSettings: useOAuth=$useOAuth, oAuthEmail='$oAuthEmail'")
            
            val settings = SettingsManager.Settings(
                projectId = binding.etProjectId.text.toString(),
                serviceAccountEmail = binding.etServiceAccountEmail.text.toString(),
                spreadsheetId = binding.etSpreadsheetId.text.toString(),
                driveFolderId = binding.etDriveFolderId.text.toString(),
                companyName = binding.etCompanyName.text.toString(),
                workStartHour = binding.timePickerStart.hour,
                workStartMinute = binding.timePickerStart.minute,
                workEndHour = binding.timePickerEnd.hour,
                workEndMinute = binding.timePickerEnd.minute,
                useOAuth = useOAuth,
                oAuthEmail = oAuthEmail,
                useDefaultCredentials = binding.rbDefaultCredentials.isChecked,
                customCredentialsFile = if (binding.rbCustomCredentials.isChecked) "credentials.json" else ""
            )
            
            settingsManager.saveSettings(settings)
            Log.d(TAG, "Configuración guardada exitosamente: useOAuth=${settings.useOAuth}, oAuthEmail='${settings.oAuthEmail}'")
            Toast.makeText(this, "Configuración guardada correctamente", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error al guardar configuración: ${e.message}")
            Toast.makeText(this, "Error al guardar configuración: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun testConnection() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                if (binding.rbOAuth.isChecked) {
                    // Probar conexión con OAuth
                    if (!oAuthManager.isSignedIn()) {
                        Toast.makeText(this@SettingsActivity, "Debes conectar con Google primero", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    
                    val testResult = oAuthManager.testConnection()
                    if (testResult) {
                        Toast.makeText(this@SettingsActivity, "Conexión OAuth exitosa", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@SettingsActivity, "Error en la conexión OAuth", Toast.LENGTH_LONG).show()
                    }
                } else {
                    // Probar conexión con cuenta de servicio
                    val inputStream = settingsManager.getCredentialsInputStream()
                    if (inputStream == null) {
                        Toast.makeText(this@SettingsActivity, "No se pueden obtener las credenciales", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    
                    val sheetsManager = settingsManager.getGoogleSheetsManager()
                    
                    if (sheetsManager == null) {
                        Toast.makeText(this@SettingsActivity, "No se pudo crear GoogleSheetsManager", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    
                    // Intentar hacer una llamada de prueba
                    val testResult = withContext(Dispatchers.IO) {
                        try {
                            sheetsManager.sheetsServicePublic.spreadsheets().get(binding.etSpreadsheetId.text.toString()).execute()
                            true
                        } catch (e: Exception) {
                            Log.e(TAG, "Error en prueba de conexión: ${e.message}")
                            false
                        }
                    }
                    
                    if (testResult) {
                        val credentialsType = if (binding.rbDefaultCredentials.isChecked) "por defecto" else "personalizadas"
                        Toast.makeText(this@SettingsActivity, "Conexión exitosa con credenciales $credentialsType", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@SettingsActivity, "Error en la conexión", Toast.LENGTH_LONG).show()
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error al probar conexión: ${e.message}")
                Toast.makeText(this@SettingsActivity, "Error al probar conexión: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun resetSettings() {
        // Mostrar diálogo de confirmación
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Restablecer configuración")
            .setMessage("¿Estás seguro de que quieres restablecer toda la configuración? Esta acción no se puede deshacer.")
            .setPositiveButton("Sí, restablecer") { _, _ ->
                settingsManager.resetSettings()
                loadCurrentSettings()
                Toast.makeText(this, "Configuración restablecida", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun clearOAuthSettings() {
        // Mostrar diálogo de confirmación
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Limpiar configuración OAuth")
            .setMessage("¿Estás seguro de que quieres limpiar la configuración de OAuth? Esto te permitirá configurar OAuth desde cero.")
            .setPositiveButton("Sí, limpiar") { _, _ ->
                try {
                    // Limpiar solo la configuración de OAuth
                    val currentSettings = settingsManager.getSettings()
                    val cleanSettings = currentSettings.copy(
                        useOAuth = false,
                        oAuthEmail = "",
                        useDefaultCredentials = false
                    )
                    settingsManager.saveSettings(cleanSettings)
                    
                    // Recargar la UI
                    loadCurrentSettings()
                    Toast.makeText(this, "Configuración OAuth limpiada", Toast.LENGTH_SHORT).show()
                    
                    Log.d(TAG, "clearOAuthSettings: Configuración OAuth limpiada exitosamente")
                } catch (e: Exception) {
                    Log.e(TAG, "Error al limpiar configuración OAuth: ${e.message}")
                    Toast.makeText(this, "Error al limpiar configuración: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
