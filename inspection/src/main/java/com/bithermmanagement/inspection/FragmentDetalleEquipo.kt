package com.bithermmanagement.inspection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bithermmanagement.inspection.R
import com.bithermmanagement.core.database.entities.EquipoView
import com.bithermmanagement.core.data.db.AppDatabase
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FragmentDetalleEquipo : Fragment() {
    @Inject
    lateinit var database: AppDatabase
    
    private var equipoId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            equipoId = it.getString("equipo_id")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_detalle_equipo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        equipoId?.let { id ->
            lifecycleScope.launch {
                val dao = database.inspeccionDao()
                val equipo = dao.getEquipoViewPorId(id)
                
                equipo?.let { mostrarDetallesEquipo(view, it) }
            }
        }
    }

    private fun mostrarDetallesEquipo(view: View, equipo: EquipoView) {
        // Mapear los campos del EquipoView a los TextViews del layout
        val campos = mapOf(
            "txtId" to equipo.id,
            "txtEstado" to equipo.estado,
            "txtArea" to equipo.area,
            "txtUnidad" to equipo.unidad,
            "txtMarca" to equipo.marca,
            "txtModelo" to equipo.modelo,
            "txtTipo" to equipo.tipo,
            "txtDiametro" to equipo.diametro,
            "txtConexion" to equipo.conexion,
            "txtPresEntrada" to equipo.presEntrada,
            "txtPresSalida" to equipo.presSalida,
            "txtDescarga" to equipo.descarga,
            "txtAplicacion" to equipo.aplicacion,
            "txtServicio" to equipo.servicio,
            "txtUbicacion" to equipo.ubicacion,
            "txtFechaInspeccion" to equipo.fechasteado,
            "txtNota" to equipo.nota,
            "txtInspector" to equipo.inspector,
            "txtDetector" to equipo.detector,
            "txtIncidencias" to equipo.incidencias,
            "txtGps" to equipo.gps,
            "txtFoto" to equipo.foto,
            "txtInstalacion" to equipo.instalacion,
            "txtLinea" to equipo.linea,
            "txtAislamiento" to equipo.aislamiento
        )

        campos.forEach { (viewId, valor) ->
            val textView = view.findViewById<TextView>(resources.getIdentifier(viewId, "id", requireContext().packageName))
            textView?.text = valor ?: "N/A"
        }
    }

    companion object {
        fun newInstance(equipoId: String) = FragmentDetalleEquipo().apply {
            arguments = Bundle().apply {
                putString("equipo_id", equipoId)
            }
        }
    }
} 