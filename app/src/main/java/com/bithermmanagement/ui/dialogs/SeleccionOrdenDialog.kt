package com.bithermmanagement.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bithermmanagement.R
import com.bithermmanagement.ui.adapters.CampoOrden
import com.bithermmanagement.ui.adapters.CampoOrdenAdapter

class SeleccionOrdenDialog(
    context: Context,
    private val camposOrden: List<String>,
    private val campoActual: String?,
    private val onCampoSeleccionado: (String) -> Unit
) : Dialog(context) {

    private lateinit var rvCamposOrden: RecyclerView
    private lateinit var btnCancelar: Button
    private lateinit var btnConfirmar: Button
    private lateinit var adapter: CampoOrdenAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_seleccion_orden)

        rvCamposOrden = findViewById(R.id.rvCamposOrden)
        btnCancelar = findViewById(R.id.btnCancelar)
        btnConfirmar = findViewById(R.id.btnConfirmar)

        setupRecyclerView()
        setupButtons()
    }

    private fun setupRecyclerView() {
        val campos = camposOrden.map { nombre ->
            CampoOrden(
                nombre = nombre,
                descripcion = getDescripcionCampo(nombre),
                esSeleccionado = nombre == campoActual
            )
        }

        adapter = CampoOrdenAdapter(campos) { campo ->
            // El campo se selecciona automáticamente en el adaptador
        }

        rvCamposOrden.layoutManager = LinearLayoutManager(context)
        rvCamposOrden.adapter = adapter
    }

    private fun setupButtons() {
        btnCancelar.setOnClickListener {
            dismiss()
        }

        btnConfirmar.setOnClickListener {
            val campoSeleccionado = adapter.getCampoSeleccionado()
            if (campoSeleccionado != null) {
                onCampoSeleccionado(campoSeleccionado.nombre)
            }
            dismiss()
        }
    }

    private fun getDescripcionCampo(nombre: String): String {
        return when (nombre) {
            "orden_default" -> "Orden por defecto del sistema"
            "orden_juan" -> "Orden personalizado de Juan"
            "orden_paco" -> "Orden personalizado de Paco"
            else -> "Orden personalizado"
        }
    }
} 