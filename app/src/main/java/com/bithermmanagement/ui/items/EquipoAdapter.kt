package com.bithermmanagement.ui.items

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bithermmanagement.R
import com.google.android.material.card.MaterialCardView
import org.json.JSONObject
import java.io.InputStream
import com.bithermmanagement.database.entities.EquipoView

class EquipoAdapter(
    private val context: Context,
    private var equipos: List<EquipoView>,
    private val coloresEstados: Map<String, Int>,
    private val onEquipoClick: (EquipoView) -> Unit
) : RecyclerView.Adapter<EquipoAdapter.EquipoViewHolder>() {

    fun updateData(nuevos: List<EquipoView>) {
        Log.d("EquipoAdapter", "updateData: actualizando con ${nuevos.size} equipos")
        equipos = nuevos.sortedBy { it.orden }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EquipoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_equipo_comprimido, parent, false)
        return EquipoViewHolder(view, onEquipoClick)
    }

    override fun onBindViewHolder(holder: EquipoViewHolder, position: Int) {
        val equipo = equipos[position]
        Log.d("EquipoAdapter", "onBindViewHolder: posición=$position, equipoId=${equipo.id}, estado=${equipo.estado}")
        holder.bind(equipo, coloresEstados)
    }

    override fun getItemCount(): Int = equipos.size

    fun swapItems(from: Int, to: Int) {
        if (from == to) return
        Log.d("EquipoAdapter", "swapItems: moviendo de posición $from a $to")
        val mutable = equipos.toMutableList()
        val item = mutable.removeAt(from)
        mutable.add(to, item)
        equipos = mutable
        notifyItemMoved(from, to)
    }

    fun getEquipos(): List<EquipoView> = equipos

    class EquipoViewHolder(view: View, val onEquipoClick: (EquipoView) -> Unit) : RecyclerView.ViewHolder(view) {
        private val txtTag: TextView = view.findViewById(R.id.txtTag)
        private val txtEstado: TextView = view.findViewById(R.id.txtEstado)
        private val card: MaterialCardView = view as MaterialCardView
        private var equipoActual: EquipoView? = null

        init {
            card.setOnClickListener {
                Log.d("EquipoAdapter", "CLICK en Card: id=${equipoActual?.id ?: "null"}, estado=${equipoActual?.estado ?: "null"}")
                equipoActual?.let { onEquipoClick(it) }
            }
        }

        fun bind(equipo: EquipoView, coloresEstados: Map<String, Int>) {
            equipoActual = equipo
            txtTag.text = equipo.id
            
            // Mostrar SOLO estado de inspección (estado) - NO usar flota para colorear
            val estadoTexto = equipo.estado ?: ""
            txtEstado.text = estadoTexto
            
            // Usar SOLO estado de inspección para colorear (flota no se usa para colorear)
            val estadoKey = (equipo.estado ?: "").uppercase().trim()
            val color = coloresEstados[estadoKey] ?: 0xFFFFFFFF.toInt()
            
            Log.d("EquipoAdapter", "Equipo ${equipo.id}: estado='${equipo.estado}', flota='${equipo.flota}', estadoKey='$estadoKey', color=${String.format("#%06X", 0xFFFFFF and color)}, coloresEstados.size=${coloresEstados.size}")
            
            // Si no se encontró color y hay estados disponibles, mostrar las claves disponibles
            if (color == 0xFFFFFFFF.toInt() && coloresEstados.isNotEmpty() && estadoKey.isNotEmpty()) {
                Log.w("EquipoAdapter", "No se encontró color para estado '$estadoKey'. Claves disponibles: ${coloresEstados.keys.take(10).joinToString(", ")}")
            }
            
            // Aplicar el color de fondo al card
            card.setCardBackgroundColor(color)
            
            // Ajustar el color del texto según el fondo
            val isDarkBackground = isColorDark(color)
            val textColor = if (isDarkBackground) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
            txtTag.setTextColor(textColor)
            txtEstado.setTextColor(textColor)
        }
        
        private fun isColorDark(color: Int): Boolean {
            val darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
            return darkness >= 0.5
        }
    }
}

// Modelo de datos de ejemplo
// data class Equipo(val tag: String, val estado: String) 