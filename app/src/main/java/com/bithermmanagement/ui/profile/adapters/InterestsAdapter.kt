package com.bithermmanagement.ui.profile.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bithermmanagement.R

class InterestsAdapter(
    private val onInterestClick: (String) -> Unit
) : RecyclerView.Adapter<InterestsAdapter.InterestViewHolder>() {
    
    private val interests = mutableListOf<String>()
    private val availableInterests = listOf(
        "Música", "Deportes", "Viajes", "Arte", "Tecnología", "Cine", "Literatura",
        "Cocina", "Fotografía", "Baile", "Gaming", "Fitness", "Naturaleza",
        "Moda", "Historia", "Ciencia", "Filosofía", "Música clásica", "Rock",
        "Pop", "Jazz", "Electrónica", "Fútbol", "Basketball", "Tenis",
        "Natación", "Ciclismo", "Running", "Yoga", "Pilates", "Meditación"
    )
    
    class InterestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val btnInterest: Button = itemView.findViewById(R.id.btn_interest)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InterestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_interest, parent, false)
        return InterestViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: InterestViewHolder, position: Int) {
        val interest = availableInterests[position]
        val isSelected = interests.contains(interest)
        
        holder.btnInterest.text = interest
        holder.btnInterest.isSelected = isSelected
        
        if (isSelected) {
            holder.btnInterest.setBackgroundResource(R.drawable.button_primary)
            holder.btnInterest.setTextColor(holder.itemView.context.getColor(R.color.white))
        } else {
            holder.btnInterest.setBackgroundResource(R.drawable.button_secondary)
            holder.btnInterest.setTextColor(holder.itemView.context.getColor(R.color.white))
        }
        
        holder.btnInterest.setOnClickListener {
            if (isSelected) {
                interests.remove(interest)
            } else {
                interests.add(interest)
            }
            notifyItemChanged(position)
            onInterestClick(interest)
        }
    }
    
    override fun getItemCount(): Int = availableInterests.size
    
    fun getSelectedInterests(): List<String> = interests.toList()
    
    fun setSelectedInterests(selectedInterests: List<String>) {
        interests.clear()
        interests.addAll(selectedInterests)
        notifyDataSetChanged()
    }
}
