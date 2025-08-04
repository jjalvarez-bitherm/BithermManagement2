package com.bithermmanagement.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bithermmanagement.R
import com.bithermmanagement.database.entities.EquipoView

class PurgadorAdapter : RecyclerView.Adapter<PurgadorAdapter.PurgadorViewHolder>() {
    private var purgadores: List<EquipoView> = emptyList()

    fun updateData(nuevos: List<EquipoView>) {
        purgadores = nuevos
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PurgadorViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_purgador, parent, false)
        return PurgadorViewHolder(view)
    }

    override fun onBindViewHolder(holder: PurgadorViewHolder, position: Int) {
        holder.bind(purgadores[position])
    }

    override fun getItemCount(): Int = purgadores.size

    class PurgadorViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val txtId: TextView = view.findViewById(R.id.txtId)
        private val txtEstado: TextView = view.findViewById(R.id.txtEstado)
        private val txtArea: TextView = view.findViewById(R.id.txtArea)
        private val txtUnidad: TextView = view.findViewById(R.id.txtUnidad)
        private val txtMarca: TextView = view.findViewById(R.id.txtMarca)
        private val txtModelo: TextView = view.findViewById(R.id.txtModelo)

        fun bind(purgador: EquipoView) {
            txtId.text = "ID: ${purgador.id}"
            txtEstado.text = "Estado: ${purgador.estado ?: "No especificado"}"
            txtArea.text = "Área: ${purgador.area ?: "No especificada"}"
            txtUnidad.text = "Unidad: ${purgador.unidad ?: "No especificada"}"
            txtMarca.text = "Marca: ${purgador.marca ?: "No especificada"}"
            txtModelo.text = "Modelo: ${purgador.modelo ?: "No especificado"}"
        }
    }
} 