package com.bithermmanagement.ui.estancia

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bithermmanagement.R

class ModoEstanciaActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modo_estancia)
        
        setupUI()
    }
    
    private fun setupUI() {
        val tvTitulo = findViewById<TextView>(R.id.tv_titulo)
        val btnVolver = findViewById<Button>(R.id.btn_volver)
        val btnEscanearQR = findViewById<Button>(R.id.btn_escanear_qr)
        
        tvTitulo.text = "Modo Estancia"
        
        btnVolver.setOnClickListener {
            finish()
        }
        
        btnEscanearQR.setOnClickListener {
            // TODO: Implementar escáner de QR
            tvTitulo.text = "Escaneando QR..."
        }
    }
}
