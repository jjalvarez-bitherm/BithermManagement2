package com.bithermmanagement.ui.profile.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView
import com.bithermmanagement.R

class LanguagesAdapter(
    private val onLanguageChange: (String) -> Unit
) : RecyclerView.Adapter<LanguagesAdapter.LanguageViewHolder>() {
    
    private val languages = mutableListOf<String>()
    private val availableLanguages = listOf(
        "Español", "Inglés"
    )
    
    class LanguageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val btnLanguage: Button = itemView.findViewById(R.id.btn_language)
        val btnRemove: ImageButton = itemView.findViewById(R.id.btn_remove_language)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_language, parent, false)
        return LanguageViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        val language = availableLanguages[position]
        val isSelected = languages.contains(language)
        
        holder.btnLanguage.text = language
        holder.btnLanguage.isSelected = isSelected
        
        if (isSelected) {
            holder.btnLanguage.setBackgroundResource(R.drawable.button_primary)
            holder.btnLanguage.setTextColor(holder.itemView.context.getColor(android.R.color.white))
        } else {
            holder.btnLanguage.setBackgroundResource(R.drawable.button_secondary)
            holder.btnLanguage.setTextColor(holder.itemView.context.getColor(android.R.color.white))
        }
        
        holder.btnLanguage.setOnClickListener {
            if (isSelected) {
                languages.remove(language)
            } else {
                languages.add(language)
            }
            notifyItemChanged(position)
            onLanguageChange(language)
        }
        
        holder.btnRemove.setOnClickListener {
            languages.remove(language)
            notifyItemChanged(position)
        }
    }
    
    override fun getItemCount(): Int = availableLanguages.size
    
    fun getSelectedLanguages(): List<String> = languages.toList()
    
    fun setSelectedLanguages(selectedLanguages: List<String>) {
        languages.clear()
        languages.addAll(selectedLanguages)
        notifyDataSetChanged()
    }
}
