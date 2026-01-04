package com.bithermmanagement.login

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.bithermmanagement.core.data.LoginRepository
import com.bithermmanagement.core.data.SessionManager
import com.bithermmanagement.navigation.NavigationActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    private val viewModel: LoginViewModel by viewModels()
    @Inject
    lateinit var sessionManager: SessionManager
    @Inject
    lateinit var loginRepository: LoginRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Establecer la fecha y hora de compilación de forma segura
        try {
            val buildInfoTextView = findViewById<android.widget.TextView>(R.id.tvBuildInfo)
            if (buildInfoTextView != null) {
                val buildTime = BuildConfig.BUILD_TIME
                val buildDate = Date(buildTime)
                val format = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                buildInfoTextView.text = "Compilado: ${format.format(buildDate)}"
            }
        } catch (e: Exception) {
            android.util.Log.e("SplashActivity", "Error al establecer la fecha de compilación: ${e.message}")
        }
        
        // Iniciar navegación después del splash
        lifecycleScope.launch {
            delay(3000)
            checkSessionAndNavigate()
        }
    }

    private suspend fun checkSessionAndNavigate() {
        // Verificar que la inyección se haya completado
        if (!::sessionManager.isInitialized || !::loginRepository.isInitialized) {
            Log.e("SplashActivity", "Dependencias no inicializadas, navegando a login")
            navigateToLogin()
            return
        }
        
        if (sessionManager.shouldRememberMe()) {
            val (user, pass) = sessionManager.getCredentials()
            if (user != null && pass != null) {
                val userData = loginRepository.validateCredentials(user, pass)
                if (userData != null) {
                    Log.d("SplashActivity", "Auto-login exitoso para: ${userData.nombre}")
                    sessionManager.setLoggedIn(true)
                    navigateToMain(userData)
                    return
                } else {
                    Log.d("SplashActivity", "Auto-login fallido. Limpiando sesión.")
                    sessionManager.clearSessionOnLogout()
                }
            }
        }
        Log.d("SplashActivity", "No hay auto-login. Navegando a la pantalla de login.")
        navigateToLogin()
    }

    private fun navigateToMain(userData: com.bithermmanagement.core.data.UserData) {
        val intent = Intent(this, NavigationActivity::class.java).apply {
            putExtra("USER_NAME", userData.nombre)
            putExtra("USER_ROLPOUND", userData.rolPound)
        }
        startActivity(intent)
        finish()
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}

