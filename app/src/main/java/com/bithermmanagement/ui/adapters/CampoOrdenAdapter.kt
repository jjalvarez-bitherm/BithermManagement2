package com.bithermmanagement.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bithermmanagement.R

data class CampoOrden(
    val nombre: String,
    val descripcion: String,
    val esSeleccionado: Boolean = false
)

class CampoOrdenAdapter(
    private var campos: List<CampoOrden>,
    private val onCampoSeleccionado: (CampoOrden) -> Unit
) : RecyclerView.Adapter<CampoOrdenAdapter.ViewHolder>() {

    private var selectedPosition = -1

    init {
        // Encontrar el campo seleccionado inicialmente
        campos.forEachIndexed { index, campo ->
            if (campo.esSeleccionado) {
                selectedPosition = index
            }
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val radioButton: RadioButton = view.findViewById(R.id.rbCampoOrden)
        val tvNombreCampo: TextView = view.findViewById(R.id.tvNombreCampo)
        val tvDescripcionCampo: TextView = view.findViewById(R.id.tvDescripcionCampo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_campo_orden, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val campo = campos[position]
        
        holder.tvNombreCampo.text = campo.nombre
        holder.tvDescripcionCampo.text = campo.descripcion
        holder.radioButton.isChecked = position == selectedPosition

        holder.itemView.setOnClickListener {
            val adapterPosition = holder.getAdapterPosition()
            if (adapterPosition != RecyclerView.NO_POSITION) {
                val previousPosition = selectedPosition
                selectedPosition = adapterPosition
                notifyItemChanged(previousPosition)
                notifyItemChanged(selectedPosition)
                onCampoSeleccionado(campos[adapterPosition])
            }
        }

        holder.radioButton.setOnClickListener {
            val adapterPosition = holder.getAdapterPosition()
            if (adapterPosition != RecyclerView.NO_POSITION) {
                val previousPosition = selectedPosition
                selectedPosition = adapterPosition
                notifyItemChanged(previousPosition)
                notifyItemChanged(selectedPosition)
                onCampoSeleccionado(campos[adapterPosition])
            }
        }
    }

    override fun getItemCount() = campos.size

    fun getCampoSeleccionado(): CampoOrden? {
        return if (selectedPosition >= 0 && selectedPosition < campos.size) {
            campos[selectedPosition]
        } else null
    }

    fun actualizarCampos(nuevosCampos: List<CampoOrden>) {
        campos = nuevosCampos
        notifyDataSetChanged()
    }
} 