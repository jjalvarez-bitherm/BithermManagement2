package com.bithermmanagement.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.bithermmanagement.R
import com.bithermmanagement.data.GoogleSheetsManager
import com.bithermmanagement.data.UserData
import com.bithermmanagement.databinding.ActivityLoginBinding
import com.bithermmanagement.ui.MainMenuActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executor
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import android.provider.Settings
import android.app.AlertDialog
import android.util.Log
import com.bithermmanagement.ui.login.BuildInfo

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private lateinit var googleSheetsManager: GoogleSheetsManager
    private var loginAttempts = 0
    private val MAX_LOGIN_ATTEMPTS = 6

    // Nuevo sistema de permisos
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            binding.btnLogin.isEnabled = true
            binding.btnBiometric.isEnabled = true
            Toast.makeText(this, "Permisos concedidos. Ya puedes usar la app.", Toast.LENGTH_SHORT).show()
        } else {
            val permisosFaltantes = permissions.entries.filter { !it.value }.map { it.key }
            if (permisosFaltantes.isNotEmpty()) {
                binding.btnLogin.isEnabled = false
                binding.btnBiometric.isEnabled = false
                if (permisosFaltantes.any { !ActivityCompat.shouldShowRequestPermissionRationale(this, it) }) {
                    mostrarDialogoPermisosDenegados()
                } else {
                    Toast.makeText(this, "Debes conceder todos los permisos para usar la app", Toast.LENGTH_LONG).show()
                }
            } else {
                binding.btnLogin.isEnabled = true
                binding.btnBiometric.isEnabled = true
            }
        }
    }

    private fun getRequiredPermissions(): Array<String> {
        val perms = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA,
            Manifest.permission.CALL_PHONE
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            perms.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.P) {
                perms.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
        return perms.toTypedArray()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("LoginActivity", "onCreate llamado")
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Solicitar solo los permisos que falten
        val requiredPermissions = getRequiredPermissions()
        val permisosFaltantes = requiredPermissions.filter {
            ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
        if (permisosFaltantes.isNotEmpty()) {
            requestPermissionLauncher.launch(permisosFaltantes)
            binding.btnLogin.isEnabled = false
            binding.btnBiometric.isEnabled = false
        } else {
            binding.btnLogin.isEnabled = true
            binding.btnBiometric.isEnabled = true
        }

        // Cargar credenciales guardadas
        val prefs = getSharedPreferences("bitherm_prefs", MODE_PRIVATE)
        val savedUser = prefs.getString("saved_user", "") ?: ""
        val savedPass = prefs.getString("saved_pass", "") ?: ""
        val remember = prefs.getBoolean("remember", false)
        binding.etUsername.setText(savedUser)
        binding.etPassword.setText(savedPass)
        binding.cbRemember.isChecked = remember
        Log.d("LoginActivity", "Credenciales guardadas: user=$savedUser, remember=$remember")

        // Login automático si hay credenciales y recordar está activado
        if (remember && savedUser.isNotEmpty() && savedPass.isNotEmpty()) {
            performLogin(savedUser, savedPass)
        }

        // Inicializar GoogleSheetsManager con las credenciales
        Log.d("LoginActivity", "Inicializando GoogleSheetsManager")
        val credentialsStream = assets.open("credentials.json")
        googleSheetsManager = GoogleSheetsManager(credentialsStream, this)

        // Configurar pantalla completa
        window.insetsController?.let { controller ->
            controller.hide(WindowInsets.Type.statusBars())
            controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } ?: run {
            // Fallback para versiones anteriores
            @Suppress("DEPRECATION")
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        setupBiometricAuthentication()
        setupLoginButton()
        
        // Establecer timestamp de compilación
        try {
            val buildTime = BuildInfo.BUILD_TIMESTAMP
            binding.tvBuildInfo.text = "Build: $buildTime"
        } catch (e: Exception) {
            // Fallback si no existe BuildInfo
            binding.tvBuildInfo.text = "Build: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(java.util.Date())}"
        }
        
        Log.d("LoginActivity", "setupBiometricAuthentication y setupLoginButton llamados")
    }

    private fun setupBiometricAuthentication() {
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(this@LoginActivity, 
                        "Error de autenticación: $errString", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    // Aquí implementaremos la lógica de autenticación biométrica
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(this@LoginActivity,
                        "Autenticación fallida", Toast.LENGTH_SHORT).show()
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Autenticación biométrica")
            .setSubtitle("Inicie sesión usando su huella digital")
            .setNegativeButtonText("Usar contraseña")
            .build()
    }

    private fun setupLoginButton() {
        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString()
            val password = binding.etPassword.text.toString()
            Log.d("LoginActivity", "Botón login pulsado: user=$username")
            if (username.isEmpty() || password.isEmpty()) {
                Log.d("LoginActivity", "Campos vacíos")
                Toast.makeText(this, "Por favor, rellene todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            performLogin(username, password)
        }

        binding.btnBiometric.setOnClickListener {
            Log.d("LoginActivity", "Botón login biométrico pulsado")
            biometricPrompt.authenticate(promptInfo)
        }
    }

    private fun performLogin(username: String, password: String) {
        Log.d("LoginActivity", "Iniciando performLogin para $username")
        if (loginAttempts >= MAX_LOGIN_ATTEMPTS) {
            Log.d("LoginActivity", "Demasiados intentos fallidos")
            Toast.makeText(this, "Demasiados intentos fallidos. Intente más tarde.", 
                Toast.LENGTH_LONG).show()
            return
        }

        binding.btnLogin.isEnabled = false
        CoroutineScope(Dispatchers.Main).launch {
            Log.d("LoginActivity", "Llamando a googleSheetsManager.getUserData")
            val userData = withContext(Dispatchers.IO) {
                googleSheetsManager.getUserData(username, password)
            }
            Log.d("LoginActivity", "Resultado getUserData: $userData")
            if (userData != null) {
                Log.d("LoginActivity", "Login exitoso para $username")
                loginAttempts = 0
                // Guardar credenciales si está marcado
                val prefs = getSharedPreferences("bitherm_prefs", MODE_PRIVATE)
                if (binding.cbRemember.isChecked) {
                    prefs.edit()
                        .putString("saved_user", username)
                        .putString("saved_pass", password)
                        .putBoolean("remember", true)
                        .apply()
                } else {
                    prefs.edit()
                        .remove("saved_user")
                        .remove("saved_pass")
                        .putBoolean("remember", false)
                        .apply()
                }
                // Guardar identificador APP y nombre completo
                prefs.edit()
                    .putString("username", userData.app)
                    .putString("user_name", userData.nombre + " " + userData.apellidos)
                    .putString("sw_web", userData.swWeb)
                    .putString("equipo_asignado", userData.equipoAsignado)
                    .apply()
                Log.d("LoginActivity", "Sincronizando menús para usuario ${userData.app}")
                withContext(Dispatchers.IO) {
                    com.bithermmanagement.data.MenuRepository.syncMenusForUser(
                        this@LoginActivity,
                        userData.app,
                        userData.getRolPound(),
                        googleSheetsManager
                    )
                }
                Log.d("LoginActivity", "Navegando a MainMenuActivity")
                navigateToStartPage(userData)
            } else {
                Log.d("LoginActivity", "Login fallido para $username")
                loginAttempts++
                Toast.makeText(this@LoginActivity, 
                    "Usuario o contraseña incorrectos. Intentos restantes: "+
                    "${MAX_LOGIN_ATTEMPTS - loginAttempts}", 
                    Toast.LENGTH_SHORT).show()
            }
            binding.btnLogin.isEnabled = true
        }
    }

    private fun navigateToStartPage(userData: UserData) {
        Log.d("LoginActivity", "navigateToStartPage llamado para ${userData.app}")
        val intent = Intent(this, MainMenuActivity::class.java)
        intent.putExtra("USER_DATA", userData)
        startActivity(intent)
        finish()
    }

    private fun mostrarDialogoPermisosDenegados() {
        AlertDialog.Builder(this)
            .setTitle("Permisos necesarios")
            .setMessage("Para usar la app debes conceder todos los permisos requeridos. Ve a Ajustes > Aplicaciones > BithermManagement > Permisos y actívalos.")
            .setPositiveButton("Abrir ajustes") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = android.net.Uri.fromParts("package", packageName, null)
                startActivity(intent)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun comprobarPermisosYMostrarDialogo() {
        val permisos = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            // ... otros permisos necesarios
        )
        val permisosDenegados = permisos.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (permisosDenegados.isNotEmpty()) {
            mostrarDialogoPermisosDenegados()
        }
    }
} 