package com.bithermmanagement.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bithermmanagement.R
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.bithermmanagement.ui.favoritos.FragmentFavoritos
import com.bithermmanagement.fragments.FragmentItemTitle
import com.bithermmanagement.fragments.FragmentSubMenuList
import android.graphics.Color
import com.bithermmanagement.database.entities.MenuEntity
import com.bithermmanagement.database.entities.SubMenuEntity

class MainMenuAdapter(
    private val mainMenus: List<MenuEntity>
) : RecyclerView.Adapter<MainMenuAdapter.MenuViewHolder>() {
    // Ordenar los menús principales por el índice (columna B)
    private val orderedMenus = mainMenus.sortedBy { it.peso }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_main_menu_card, parent, false)
        return MenuViewHolder(view)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        val menu = orderedMenus[position]
        holder.title.text = menu.nombreVisible
        holder.icon.setImageResource(getIconForMenu(menu.fragment))
        try {
            holder.card.setCardBackgroundColor(Color.parseColor(menu.colorHex))
        } catch (e: Exception) {
            holder.card.setCardBackgroundColor(ContextCompat.getColor(holder.card.context, R.color.primary))
        }
        holder.card.setOnClickListener {
            val activity = holder.card.context as? FragmentActivity
            if (menu.fragment == "fragmentUsuariosPerfil") {
                activity?.supportFragmentManager?.beginTransaction()?.replace(R.id.fragment_container, com.bithermmanagement.fragments.fragmentUsuariosPerfil())?.addToBackStack(null)?.commit()
            } else if (menu.fragment == "fragmentUsuariosFichaje") {
                activity?.supportFragmentManager?.beginTransaction()?.replace(R.id.fragment_container, com.bithermmanagement.fragments.fragmentUsuariosFichaje())?.addToBackStack(null)?.commit()
            } else {
                // Navegar al submenú correspondiente
                activity?.supportFragmentManager?.beginTransaction()?.replace(R.id.fragment_container, com.bithermmanagement.fragments.FragmentSubMenuList.newInstance(menu.fragment))?.addToBackStack(null)?.commit()
            }
        }
    }

    private fun getIconForMenu(fragment: String): Int {
        return when {
            fragment.contains("Usuarios", true) -> R.drawable.ic_person
            fragment.contains("Inspeccion", true) -> R.drawable.ic_inspection
            fragment.contains("Recursos", true) -> R.drawable.ic_equipment
            fragment.contains("Economica", true) -> R.drawable.ic_money
            fragment.contains("Tareas", true) -> R.drawable.ic_tasks
            fragment.contains("Notas", true) -> R.drawable.ic_note
            fragment.contains("Multimedia", true) -> R.drawable.ic_camera
            fragment.contains("Ausencias", true) -> R.drawable.ic_calendar
            else -> R.drawable.ic_menu
        }
    }

    // Función para adaptar el nombre del fragmento
    private fun adaptarNombre(nombre: String): String {
        return nombre.removePrefix("fragment").replaceFirstChar { it.lowercase() }
    }

    override fun getItemCount(): Int {
        return orderedMenus.size
    }

    class MenuViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: CardView = view.findViewById(R.id.card_menu)
        val icon: ImageView = view.findViewById(R.id.icon_menu)
        val title: TextView = view.findViewById(R.id.title_menu)
    }

    // Adaptador para los favoritos (cards horizontales)
    class FavoritosAdapter(private val favoritos: List<SubMenuEntity>) : RecyclerView.Adapter<FavoritosAdapter.FavoritoViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoritoViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_main_menu_card, parent, false)
            return FavoritoViewHolder(view)
        }
        override fun onBindViewHolder(holder: FavoritoViewHolder, position: Int) {
            val item = favoritos[position]
            holder.title.text = item.nombreVisible
            holder.icon.setImageResource(getIconForMenu(item.menuPrincipal))
            try {
                holder.card.setCardBackgroundColor(Color.parseColor(item.colorHex))
            } catch (e: Exception) {
                holder.card.setCardBackgroundColor(ContextCompat.getColor(holder.card.context, R.color.primary))
            }
            holder.card.setOnClickListener {
                val activity = holder.card.context as? FragmentActivity
                if (item.item == "fragmentUsuariosPerfil") {
                    activity?.supportFragmentManager?.beginTransaction()?.replace(R.id.fragment_container, com.bithermmanagement.fragments.fragmentUsuariosPerfil())?.addToBackStack(null)?.commit()
                } else {
                    try {
                        val fragmentClass = Class.forName("com.bithermmanagement.fragments.${item.item}")
                        val constructor = fragmentClass.getConstructor()
                        val fragment = constructor.newInstance() as androidx.fragment.app.Fragment
                        activity?.supportFragmentManager?.beginTransaction()?.replace(R.id.fragment_container, fragment)?.addToBackStack(null)?.commit()
                    } catch (e: Exception) {
                        activity?.supportFragmentManager?.beginTransaction()?.replace(R.id.fragment_container, com.bithermmanagement.fragments.FragmentItemTitle.newInstance(item.nombreVisible))?.addToBackStack(null)?.commit()
                    }
                }
            }
        }
        override fun getItemCount(): Int = favoritos.size
        class FavoritoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val card: CardView = view.findViewById(R.id.card_menu)
            val icon: ImageView = view.findViewById(R.id.icon_menu)
            val title: TextView = view.findViewById(R.id.title_menu)
        }
        private fun getIconForMenu(fragment: String): Int {
            return when {
                fragment.contains("Usuarios", true) -> R.drawable.ic_person
                fragment.contains("Inspeccion", true) -> R.drawable.ic_inspection
                fragment.contains("Recursos", true) -> R.drawable.ic_equipment
                fragment.contains("Economica", true) -> R.drawable.ic_money
                fragment.contains("Tareas", true) -> R.drawable.ic_tasks
                fragment.contains("Notas", true) -> R.drawable.ic_note
                fragment.contains("Multimedia", true) -> R.drawable.ic_camera
                fragment.contains("Ausencias", true) -> R.drawable.ic_calendar
                else -> R.drawable.ic_menu
            }
        }
    }
} 