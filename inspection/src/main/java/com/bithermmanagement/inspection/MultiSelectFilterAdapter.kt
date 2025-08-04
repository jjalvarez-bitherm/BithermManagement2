package com.bithermmanagement.inspection

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bithermmanagement.inspection.R

class MultiSelectFilterAdapter(
    private var opciones: List<String>,
    private val seleccionados: MutableSet<String>,
    private val onSelectionChanged: () -> Unit
) : RecyclerView.Adapter<MultiSelectFilterAdapter.OpcionViewHolder>() {

    private var opcionesFiltradas: List<String> = opciones
    private val seleccionOriginal = HashSet(seleccionados)
    private var mostrarMarcarCoincidentes = true

    fun filter(query: String) {
        opcionesFiltradas = if (query.isBlank()) opciones else opciones.filter { it.contains(query, true) }
        notifyDataSetChanged()
    }

    fun getOriginalSelection(): Set<String> = seleccionOriginal

    fun setMostrarMarcarCoincidentes(valor: Boolean) { mostrarMarcarCoincidentes = valor }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OpcionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_multi_select_option, parent, false)
        return OpcionViewHolder(view)
    }

    override fun getItemCount(): Int = opcionesFiltradas.size + if (mostrarMarcarCoincidentes) 1 else 0

    override fun onBindViewHolder(holder: OpcionViewHolder, position: Int) {
        if (mostrarMarcarCoincidentes && position == 0) {
            holder.txtOpcion.text = "Marcar ${opcionesFiltradas.size} coincidentes"
            holder.checkOpcion.visibility = View.VISIBLE
            val todosMarcados = opcionesFiltradas.isNotEmpty() && opcionesFiltradas.all { seleccionados.contains(it) }
            holder.checkOpcion.isChecked = todosMarcados
            holder.checkOpcion.setOnClickListener {
                if (holder.checkOpcion.isChecked) {
                    opcionesFiltradas.forEach { seleccionados.add(it) }
                } else {
                    opcionesFiltradas.forEach { seleccionados.remove(it) }
                }
                notifyDataSetChanged()
                onSelectionChanged()
            }
            holder.itemView.setOnClickListener {
                holder.checkOpcion.performClick()
            }
            return
        }
        val realPos = if (mostrarMarcarCoincidentes) position - 1 else position
        val opcion = opcionesFiltradas[realPos]
        holder.txtOpcion.text = opcion
        holder.checkOpcion.visibility = View.VISIBLE
        holder.checkOpcion.isChecked = seleccionados.contains(opcion)
        holder.itemView.setOnClickListener {
            toggle(opcion)
        }
        holder.checkOpcion.setOnClickListener {
            toggle(opcion)
        }
    }

    private fun toggle(opcion: String) {
        if (seleccionados.contains(opcion)) seleccionados.remove(opcion) else seleccionados.add(opcion)
        notifyDataSetChanged()
        onSelectionChanged()
    }

    class OpcionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val checkOpcion: CheckBox = view.findViewById(R.id.checkOpcion)
        val txtOpcion: TextView = view.findViewById(R.id.txtOpcion)
    }
} 