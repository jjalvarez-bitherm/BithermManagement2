package com.bithermmanagement.ui.profile.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView
import com.bithermmanagement.R

class HobbiesAdapter(
    private val onHobbyChange: (String) -> Unit
) : RecyclerView.Adapter<HobbiesAdapter.HobbyViewHolder>() {
    
    private val hobbies = mutableListOf<String>()
    
    class HobbyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val etHobby: EditText = itemView.findViewById(R.id.et_hobby)
        val btnRemove: ImageButton = itemView.findViewById(R.id.btn_remove_hobby)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HobbyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hobby, parent, false)
        return HobbyViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: HobbyViewHolder, position: Int) {
        val hobby = hobbies[position]
        
        holder.etHobby.setText(hobby)
        holder.etHobby.hint = "Hobby ${position + 1}"
        
        holder.etHobby.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val newHobby = holder.etHobby.text.toString().trim()
                if (newHobby.isNotEmpty() && newHobby != hobby) {
                    hobbies[position] = newHobby
                    onHobbyChange(newHobby)
                }
            }
        }
        
        holder.btnRemove.setOnClickListener {
            hobbies.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, hobbies.size)
        }
    }
    
    override fun getItemCount(): Int = hobbies.size
    
    fun addHobby() {
        hobbies.add("")
        notifyItemInserted(hobbies.size - 1)
    }
    
    fun getHobbies(): List<String> = hobbies.filter { it.isNotEmpty() }
    
    fun setHobbies(hobbiesList: List<String>) {
        hobbies.clear()
        hobbies.addAll(hobbiesList)
        notifyDataSetChanged()
    }
}
