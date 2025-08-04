package com.bithermmanagement.fichaje.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bithermmanagement.R
import com.bithermmanagement.fichaje.models.OTSelectionItem

class OTSelectionAdapter(
    private val items: List<OTSelectionItem>,
    private val onItemClick: (OTSelectionItem) -> Unit
) : RecyclerView.Adapter<OTSelectionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val checkBox: CheckBox = view.findViewById(R.id.checkBox)
        val titleText: TextView = view.findViewById(R.id.titleText)
        val descriptionText: TextView = view.findViewById(R.id.descriptionText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ot_selection, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        
        holder.titleText.text = item.name
        holder.checkBox.isChecked = item.isSelected
        
        // Configurar descripción según el tipo de OT
        when {
            item.isSpecial -> {
                holder.descriptionText.text = "OT Especial"
                holder.descriptionText.setTextColor(holder.itemView.context.getColor(android.R.color.holo_red_dark))
            }
            item.isAdministrative -> {
                holder.descriptionText.text = "OT Administrativa"
                holder.descriptionText.setTextColor(holder.itemView.context.getColor(android.R.color.holo_blue_dark))
            }
            item.isCommon -> {
                holder.descriptionText.text = "OT Común"
                holder.descriptionText.setTextColor(holder.itemView.context.getColor(android.R.color.holo_green_dark))
            }
            else -> {
                holder.descriptionText.text = "OT Específica"
                holder.descriptionText.setTextColor(holder.itemView.context.getColor(android.R.color.holo_orange_dark))
            }
        }
        
        holder.itemView.setOnClickListener {
            item.isSelected = !item.isSelected
            holder.checkBox.isChecked = item.isSelected
            onItemClick(item)
        }
        
        holder.checkBox.setOnClickListener {
            item.isSelected = holder.checkBox.isChecked
            onItemClick(item)
        }
    }

    override fun getItemCount() = items.size
} 