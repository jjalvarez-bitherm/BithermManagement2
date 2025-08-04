package com.bithermmanagement.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bithermmanagement.R
import com.bithermmanagement.ui.login.LoginActivity
import java.text.SimpleDateFormat
import java.util.*

class SplashActivity : AppCompatActivity() {
    private val SPLASH_DELAY: Long = 1500 // 1.5 segundos

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Mostrar timestamp de compilación
        mostrarTimestampCompilacion()

        // Navegar a LoginActivity después de 1.5 segundos
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }, SPLASH_DELAY)
    }

    private fun mostrarTimestampCompilacion() {
        val tvBuildTimestamp = findViewById<TextView>(R.id.tvBuildTimestamp)
        
        // Generar timestamp actual (simula la fecha de compilación)
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val timestamp = sdf.format(Date())
        
        // Mostrar el timestamp
        tvBuildTimestamp.text = "Build: $timestamp"
    }
} 