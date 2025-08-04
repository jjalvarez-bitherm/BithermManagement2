package com.bithermmanagement.navigation

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bithermmanagement.core.data.db.MenuEntity
import com.bithermmanagement.navigation.databinding.ItemMainMenuBinding

class MainMenuAdapter(
    private val showIcons: Boolean,
    private val onItemClicked: (MenuEntity) -> Unit
) : ListAdapter<MenuEntity, MainMenuAdapter.MenuViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val binding = ItemMainMenuBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MenuViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        val menuEntity = getItem(position)
        holder.bind(menuEntity, showIcons, onItemClicked)
    }

    class MenuViewHolder(private val binding: ItemMainMenuBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(menuEntity: MenuEntity, showIcons: Boolean, onItemClicked: (MenuEntity) -> Unit) {
            binding.tvTitle.text = menuEntity.visibleName
            try {
                val colorHex = getColorHex(menuEntity.colorCard)
                binding.cardBackground.setBackgroundColor(Color.parseColor(colorHex))
            } catch (e: IllegalArgumentException) {
                binding.cardBackground.setBackgroundColor(Color.GRAY)
            }

            if (showIcons) {
                binding.ivIcon.visibility = View.VISIBLE
                val iconRes = getIconForName(menuEntity.visibleName)
                if (iconRes != 0) {
                    binding.ivIcon.setImageResource(iconRes)
                } else {
                    binding.ivIcon.visibility = View.GONE // Ocultar si no hay icono específico
                }
            } else {
                binding.ivIcon.visibility = View.GONE
            }

            itemView.setOnClickListener {
                onItemClicked(menuEntity)
            }
        }

        private fun getIconForName(name: String): Int {
            return when (name) {
                "Gestión Usuarios" -> R.drawable.ic_users
                "Gestión Inspección" -> R.drawable.ic_inspection
                "Gestión Recursos" -> R.drawable.ic_resources
                "Gestión Económica" -> R.drawable.ic_economy
                "Gestión Tareas" -> R.drawable.ic_tasks
                "Gestión Notas" -> R.drawable.ic_notes
                else -> 0
            }
        }
        
        private fun getColorHex(colorName: String?): String {
            if (colorName == null) return "#607D8B" // Color por defecto si es nulo
            return when (colorName.lowercase().trim()) {
                "azul" -> "#2196F3"
                "verde" -> "#4CAF50"
                "naranja" -> "#FF9800"
                "amarillo" -> "#FFC107"
                "morado", "purple" -> "#9C27B0"
                "gris" -> "#9E9E9E"
                "rojo" -> "#F44336"
                "rosa" -> "#E91E63"
                else -> if (colorName.startsWith("#")) colorName else "#607D8B" // Si es un hex, lo usa, si no, gris
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<MenuEntity>() {
            override fun areItemsTheSame(oldItem: MenuEntity, newItem: MenuEntity): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: MenuEntity, newItem: MenuEntity): Boolean {
                return oldItem == newItem
            }
        }
    }
} 