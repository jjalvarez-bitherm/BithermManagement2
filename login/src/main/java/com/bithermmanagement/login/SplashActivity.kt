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
import com.bithermmanagement.login.databinding.ActivitySplashBinding
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

    private lateinit var binding: ActivitySplashBinding
    private val viewModel: LoginViewModel by viewModels()
    @Inject
    lateinit var sessionManager: SessionManager
    @Inject
    lateinit var loginRepository: LoginRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Establecer la fecha y hora de compilación
        val buildTime = BuildConfig.BUILD_TIME
        val buildDate = Date(buildTime)
        val format = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        binding.tvBuildInfo.text = "Compilado: ${format.format(buildDate)}"

        lifecycleScope.launch {
            delay(3000)
            checkSessionAndNavigate()
        }
    }

    private suspend fun checkSessionAndNavigate() {
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

