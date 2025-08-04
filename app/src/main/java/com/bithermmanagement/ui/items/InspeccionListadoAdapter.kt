package com.bithermmanagement.ui.items

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bithermmanagement.R
import com.bithermmanagement.database.entities.Equipo

class InspeccionListadoAdapter(
    private var items: List<Equipo>,
    private val onItemClick: (Equipo) -> Unit
) : RecyclerView.Adapter<InspeccionListadoAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_inspection_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        Log.d("InspeccionListadoAdapter", "onBindViewHolder: posición=$position, equipoId=${item.id}, estado=${item.estado}")
        
        holder.tvTag.text = item.id
        holder.tvStatus.text = item.estado ?: ""
        // Color de fondo del card según el estado
        val color = when (item.estado?.lowercase()) {
            "bien" -> Color.parseColor("#C8E6C9") // verde claro
            "baja temperatura" -> Color.parseColor("#BBDEFB") // azul claro
            "fuga" -> Color.parseColor("#FFCDD2") // rojo claro
            "fuera de servicio" -> Color.parseColor("#FFF9C4") // amarillo claro
            else -> Color.WHITE
        }
        holder.cardView.setCardBackgroundColor(color)
        // Click en la card
        holder.cardView.setOnClickListener {
            Log.d("InspeccionListadoAdapter", "CLICK en Card: id=${item.id ?: "null"}, estado=${item.estado ?: "null"}, posición=$position")
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = items.size

    fun actualizarLista(nuevaLista: List<Equipo>) {
        Log.d("InspeccionListadoAdapter", "actualizarLista: actualizando con ${nuevaLista.size} equipos")
        items = nuevaLista
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardView: CardView = view.findViewById(R.id.cardView)
        val tvTag: TextView = view.findViewById(R.id.tvTag)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
    }
} 