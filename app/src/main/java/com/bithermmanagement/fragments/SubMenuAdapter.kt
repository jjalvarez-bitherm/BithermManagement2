package com.bithermmanagement.fragments

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.bithermmanagement.R
import com.bithermmanagement.data.GoogleSheetsManager
import com.bithermmanagement.database.entities.SubMenuEntity
import com.bithermmanagement.ui.items.FragmentInspeccionActual
import com.bithermmanagement.ui.items.FragmentInspeccionBusqueda
import com.bithermmanagement.ui.items.FragmentInspeccionListado

class SubMenuAdapter(private val subMenus: List<SubMenuEntity>) : RecyclerView.Adapter<SubMenuAdapter.SubMenuViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubMenuViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_submenu_card, parent, false)
        return SubMenuViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubMenuViewHolder, position: Int) {
        val item = subMenus[position]
        holder.title.text = item.nombreVisible
        holder.icon.setImageResource(getIconForSubMenu(item.item))
        try {
            holder.card.setCardBackgroundColor(Color.parseColor(item.colorHex))
        } catch (e: Exception) {
            holder.card.setCardBackgroundColor(ContextCompat.getColor(holder.card.context, R.color.menuUser))
        }
        holder.card.setOnClickListener {
            val activity = holder.card.context as? FragmentActivity
            val fragment = getFragmentForSubMenu(item)
            activity?.supportFragmentManager?.beginTransaction()
                ?.replace(R.id.fragment_container, fragment)
                ?.addToBackStack(null)
                ?.commit()
        }
    }

    override fun getItemCount(): Int = subMenus.size

    class SubMenuViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: CardView = view.findViewById(R.id.card_submenu)
        val icon: ImageView = view.findViewById(R.id.icon_submenu)
        val title: TextView = view.findViewById(R.id.title_submenu)
    }

    private fun getIconForSubMenu(item: String): Int {
        return when {
            item.contains("Usuarios", true) -> R.drawable.ic_person
            item.contains("Inspeccion", true) -> R.drawable.ic_inspection
            item.contains("Recursos", true) -> R.drawable.ic_equipment
            item.contains("Economica", true) -> R.drawable.ic_money
            item.contains("Tareas", true) -> R.drawable.ic_tasks
            item.contains("Notas", true) -> R.drawable.ic_note
            item.contains("Multimedia", true) -> R.drawable.ic_camera
            item.contains("Ausencias", true) -> R.drawable.ic_calendar
            else -> R.drawable.ic_menu
        }
    }

    private fun getFragmentForSubMenu(item: SubMenuEntity): androidx.fragment.app.Fragment {
        return when (item.item) {
            "fragmentUsuariosPerfil" -> com.bithermmanagement.fragments.fragmentUsuariosPerfil()
            "fragmentUsuariosFichaje" -> com.bithermmanagement.fragments.fragmentUsuariosFichaje()
            "fragmentInspeccionActual" -> com.bithermmanagement.ui.items.FragmentInspeccionActual()
            "fragmentInspeccionBusqueda" -> FragmentInspeccionBusqueda()
            // Fragments del sistema de ausencias
            "fragmentAusenciasCuadrante" -> com.bithermmanagement.ausencias.fragments.FragmentAusenciasCuadrante()
            "fragmentAusenciasGestion" -> com.bithermmanagement.ausencias.fragments.FragmentAusenciasGestion()
            "fragmentNuevaAusencia" -> com.bithermmanagement.ausencias.fragments.FragmentNuevaAusencia()
            else -> {
                try {
                    val fragmentClass = Class.forName("com.bithermmanagement.fragments.${item.item}")
                    val constructor = fragmentClass.getConstructor()
                    constructor.newInstance() as androidx.fragment.app.Fragment
                } catch (e: Exception) {
                    // Si no existe el fragmento, mostrar un placeholder
                    com.bithermmanagement.fragments.FragmentItemTitle.newInstance(item.nombreVisible)
                }
            }
        }
    }
} 