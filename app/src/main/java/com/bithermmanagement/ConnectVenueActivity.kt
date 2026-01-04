package com.bithermmanagement

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bithermmanagement.ui.cercania.ModoCercaniaActivity
import com.bithermmanagement.ui.estancia.ModoEstanciaActivity

class ConnectVenueActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connect_venue)
        
        setupUI()
    }
    
    private fun setupUI() {
        val btnModoCercania = findViewById<Button>(R.id.btn_modo_cercania)
        val btnModoEstancia = findViewById<Button>(R.id.btn_modo_estancia)
        val tvTitulo = findViewById<TextView>(R.id.tv_titulo)
        
        tvTitulo.text = "MeetMe"
        
        btnModoCercania.setOnClickListener {
            startActivity(Intent(this, ModoCercaniaActivity::class.java))
        }
        
        btnModoEstancia.setOnClickListener {
            startActivity(Intent(this, ModoEstanciaActivity::class.java))
        }
    }
}
