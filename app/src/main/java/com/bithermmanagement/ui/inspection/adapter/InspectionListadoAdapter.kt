package com.bithermmanagement.ui.inspection.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bithermmanagement.R
import com.bithermmanagement.data.InspectionItem
import com.bithermmanagement.databinding.ItemInspectionListadoBinding
import com.google.android.material.card.MaterialCardView
import android.widget.Filter
import android.widget.Filterable
import androidx.core.content.ContextCompat

class InspectionListadoAdapter(
    private val onItemClick: (InspectionItem) -> Unit,
    private val onEditClick: (InspectionItem) -> Unit,
    private val onPhotoClick: (InspectionItem) -> Unit,
    private val onSelectionChanged: (List<InspectionItem>) -> Unit
) : RecyclerView.Adapter<InspectionListadoAdapter.ViewHolder>(), Filterable {

    companion object {
        private const val TAG = "InspectionListadoAdapter"
    }

    private var items: List<InspectionItem> = emptyList()
    private var filteredItems: List<InspectionItem> = emptyList()
    private var selectedItems: MutableSet<String> = mutableSetOf()
    private var expandedPosition = RecyclerView.NO_POSITION

    // Opciones de estado con colores
    private val statusOptions = listOf(
        "ESTADO", "Bien", "Fuera de servicio", "No Vapor", "Baja temperatura", 
        "Fuga continua", "Anulado", "Antes fugaba", "No encontrado", "Inaccesible", 
        "Mal instalado", "Ciclo corto", "Falta purgador", "Fuga parcial"
    )

    inner class ViewHolder(private val binding: ItemInspectionListadoBinding) : RecyclerView.ViewHolder(binding.root) {
        
        init {
            // Configurar spinner de estado
            val statusAdapter = ArrayAdapter(itemView.context, android.R.layout.simple_spinner_item, statusOptions)
            statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.statusSpinner.adapter = statusAdapter

            // Listener para expandir/colapsar
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    toggleExpansion(position)
                }
            }

            // Configurar checkbox de selección
            binding.cbSeleccionar.setOnCheckedChangeListener { _, isChecked ->
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = filteredItems[position]
                    if (isChecked) {
                        selectedItems.add(item.id)
                    } else {
                        selectedItems.remove(item.id)
                    }
                    onSelectionChanged(getSelectedItems())
                }
            }

            // Configurar botones
            binding.btnEdit.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onEditClick(filteredItems[position])
                }
            }

            binding.btnPhotos.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onPhotoClick(filteredItems[position])
                }
            }

            // Configurar listeners para cambios de datos
            setupDataChangeListeners()
        }

        private fun setupDataChangeListeners() {
            binding.statusSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (position > 0) {
                        val item = filteredItems.getOrNull(adapterPosition) ?: return
                        val newStatus = parent?.getItemAtPosition(position).toString()
                        if (item.status != newStatus) {
                            item.status = newStatus
                            item.isModified = true
                            updateCardAppearance(item)
                        }
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            // TextWatchers para campos editables
            binding.etLocation.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    val item = filteredItems.getOrNull(adapterPosition) ?: return
                    item.location = s?.toString() ?: ""
                    item.isModified = true
                }
            })

            binding.etBrand.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    val item = filteredItems.getOrNull(adapterPosition) ?: return
                    item.brand = s?.toString() ?: ""
                    item.isModified = true
                }
            })

            binding.etModel.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    val item = filteredItems.getOrNull(adapterPosition) ?: return
                    item.model = s?.toString() ?: ""
                    item.isModified = true
                }
            })

            binding.etNotes.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    val item = filteredItems.getOrNull(adapterPosition) ?: return
                    item.notes = s?.toString() ?: ""
                    item.isModified = true
                }
            })
        }

        fun bind(item: InspectionItem, isExpanded: Boolean) {
            // Poblar datos básicos
            binding.tvTag.text = item.tag
            binding.tvStatus.text = item.status
            binding.tvDescription.text = item.description

            // Poblar datos expandidos
            binding.etLocation.setText(item.location)
            binding.etBrand.setText(item.brand)
            binding.etModel.setText(item.model)
            binding.etNotes.setText(item.notes)
            binding.tvGpsLocation.text = "GPS: ${item.gpsLocation ?: "--"}"

            // Configurar spinner de estado
            val currentStatusPosition = statusOptions.indexOf(item.status).coerceAtLeast(0)
            binding.statusSpinner.setSelection(currentStatusPosition, false)

            // Configurar checkbox de selección
            binding.cbSeleccionar.isChecked = selectedItems.contains(item.id)

            // Configurar visibilidad
            binding.expandedLayout.visibility = if (isExpanded) View.VISIBLE else View.GONE

            // Aplicar estilo de tarjeta
            updateCardAppearance(item)

            // Configurar indicador de expansión
            binding.ivExpandArrow.setImageResource(
                if (isExpanded) android.R.drawable.arrow_up_float 
                else android.R.drawable.arrow_down_float
            )

            // Mostrar indicador de modificado
            if (item.isModified) {
                binding.tvModifiedIndicator.visibility = View.VISIBLE
            } else {
                binding.tvModifiedIndicator.visibility = View.GONE
            }
        }

        private fun updateCardAppearance(item: InspectionItem) {
            val cardView = binding.cardView
            val statusColor = item.getStatusColor()
            
            // Aplicar color de fondo basado en el estado
            cardView.setCardBackgroundColor(statusColor)
            
            // Ajustar colores de texto según el fondo
            val textColor = if (isDarkColor(statusColor)) Color.WHITE else Color.BLACK
            binding.tvTag.setTextColor(textColor)
            binding.tvStatus.setTextColor(textColor)
            binding.tvDescription.setTextColor(textColor)
        }

        private fun isDarkColor(color: Int): Boolean {
            val darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
            return darkness >= 0.5
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemInspectionListadoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = filteredItems[position]
        val isExpanded = position == expandedPosition
        holder.bind(item, isExpanded)
    }

    override fun getItemCount(): Int = filteredItems.size

    private fun toggleExpansion(position: Int) {
        val previousExpanded = expandedPosition
        expandedPosition = if (expandedPosition == position) RecyclerView.NO_POSITION else position
        
        // Notificar cambios
        if (previousExpanded != RecyclerView.NO_POSITION) {
            notifyItemChanged(previousExpanded)
        }
        if (expandedPosition != RecyclerView.NO_POSITION) {
            notifyItemChanged(expandedPosition)
        }
    }

    fun updateData(newItems: List<InspectionItem>) {
        items = newItems
        filteredItems = newItems
        selectedItems.clear()
        expandedPosition = RecyclerView.NO_POSITION
        notifyDataSetChanged()
    }

    fun getSelectedItems(): List<InspectionItem> {
        return filteredItems.filter { selectedItems.contains(it.id) }
    }

    fun selectAll() {
        selectedItems.clear()
        selectedItems.addAll(filteredItems.map { it.id })
        notifyDataSetChanged()
        onSelectionChanged(getSelectedItems())
    }

    fun deselectAll() {
        selectedItems.clear()
        notifyDataSetChanged()
        onSelectionChanged(getSelectedItems())
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val filterPattern = constraint?.toString()?.lowercase() ?: ""
                val filteredList = if (filterPattern.isEmpty()) {
                    items
                } else {
                    items.filter { item ->
                        item.tag.lowercase().contains(filterPattern) ||
                        item.description.lowercase().contains(filterPattern) ||
                        item.status.lowercase().contains(filterPattern) ||
                        item.location.lowercase().contains(filterPattern) ||
                        item.brand.lowercase().contains(filterPattern) ||
                        item.model.lowercase().contains(filterPattern)
                    }
                }
                
                return FilterResults().apply {
                    values = filteredList
                    count = filteredList.size
                }
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                @Suppress("UNCHECKED_CAST")
                filteredItems = results?.values as? List<InspectionItem> ?: emptyList()
                selectedItems.clear()
                expandedPosition = RecyclerView.NO_POSITION
                notifyDataSetChanged()
            }
        }
    }
} 