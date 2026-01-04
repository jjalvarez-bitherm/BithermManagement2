package com.bithermmanagement.ui.cercania

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bithermmanagement.R

class ModoCercaniaActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modo_cercania)
        
        setupUI()
    }
    
    private fun setupUI() {
        val tvTitulo = findViewById<TextView>(R.id.tv_titulo)
        val btnVolver = findViewById<Button>(R.id.btn_volver)
        
        tvTitulo.text = "Modo Cercanía"
        
        btnVolver.setOnClickListener {
            finish()
        }
        
        findViewById<Button>(R.id.btn_configurar).setOnClickListener {
            startActivity(Intent(this, com.bithermmanagement.ui.profile.ProfileActivity::class.java))
        }
    }
}
