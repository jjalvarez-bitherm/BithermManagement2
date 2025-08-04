package com.bithermmanagement.fichaje.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bithermmanagement.R
import com.bithermmanagement.fichaje.models.OTDistributionItem

class OTDistributionAdapter(
    private val items: List<OTDistributionItem>,
    private val onHoursChanged: (OTDistributionItem, Float) -> Unit
) : RecyclerView.Adapter<OTDistributionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleText: TextView = view.findViewById(R.id.titleText)
        val hoursEditText: EditText = view.findViewById(R.id.hoursEditText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hours_distribution, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        
        holder.titleText.text = item.title
        holder.hoursEditText.setText(item.hours.toString())
        
        holder.hoursEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                try {
                    val hours = holder.hoursEditText.text.toString().toFloatOrNull() ?: 0f
                    item.hours = hours
                    onHoursChanged(item, hours)
                } catch (e: NumberFormatException) {
                    holder.hoursEditText.setText("0")
                    item.hours = 0f
                    onHoursChanged(item, 0f)
                }
            }
        }
    }

    override fun getItemCount() = items.size
} 