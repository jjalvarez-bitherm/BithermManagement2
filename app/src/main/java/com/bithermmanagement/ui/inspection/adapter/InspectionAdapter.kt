package com.bithermmanagement.ui.inspection.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bithermmanagement.databinding.ItemInspectionCardBinding
import com.bithermmanagement.data.InspectionItem

class InspectionAdapter(private val items: List<InspectionItem>) : RecyclerView.Adapter<InspectionAdapter.ViewHolder>() {
    inner class ViewHolder(private val binding: ItemInspectionCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: InspectionItem) {
            binding.tvTag.text = item.tag
            binding.tvStatus.text = item.status
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemInspectionCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
} 