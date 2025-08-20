package com.bithermmanagement.ausencias.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bithermmanagement.R
import com.bithermmanagement.ausencias.models.AbsenceRecord
import com.bithermmanagement.ausencias.models.AbsenceType
import java.text.SimpleDateFormat
import java.util.*

class AbsenceListAdapter : RecyclerView.Adapter<AbsenceListAdapter.AbsenceViewHolder>() {

    private var absences: List<AbsenceRecord> = emptyList()
    private val dateFormat = SimpleDateFormat("dd MMM", Locale("es", "ES"))

    class AbsenceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewAbsenceType: TextView = itemView.findViewById(R.id.textViewAbsenceType)
        val textViewEmployee: TextView = itemView.findViewById(R.id.textViewEmployee)
        val textViewDates: TextView = itemView.findViewById(R.id.textViewDates)
        val textViewDescription: TextView = itemView.findViewById(R.id.textViewDescription)
        val textViewStatus: TextView = itemView.findViewById(R.id.textViewStatus)
        val indicatorType: View = itemView.findViewById(R.id.indicatorType)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AbsenceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_absence_card, parent, false)
        return AbsenceViewHolder(view)
    }

    override fun onBindViewHolder(holder: AbsenceViewHolder, position: Int) {
        val absence = absences[position]
        
                                        // Tipo de ausencia con abreviación - solo el tipo en negrita, nombre normal
                val abbreviation = getAbsenceTypeAbbreviation(absence.absenceType)
                holder.textViewAbsenceType.text = "($abbreviation)"
                holder.textViewEmployee.text = absence.employeeName
                holder.textViewEmployee.visibility = View.VISIBLE
        
        // Fechas
        if (absence.startDate == absence.endDate) {
            holder.textViewDates.text = dateFormat.format(absence.startDate)
        } else {
            holder.textViewDates.text = "${dateFormat.format(absence.startDate)} - ${dateFormat.format(absence.endDate)}"
        }
        
        // Descripción
        holder.textViewDescription.text = absence.description ?: "Sin descripción"
        
        // Estado
        when (absence.status) {
            com.bithermmanagement.ausencias.models.AbsenceStatus.APROBADA -> {
                holder.textViewStatus.text = "✔ Aprobada"
                holder.textViewStatus.setTextColor(holder.itemView.context.getColor(R.color.green_600))
                holder.textViewStatus.setBackgroundResource(R.drawable.bg_status_approved)
            }
            com.bithermmanagement.ausencias.models.AbsenceStatus.PENDIENTE -> {
                holder.textViewStatus.text = "⏳ Pendiente"
                holder.textViewStatus.setTextColor(holder.itemView.context.getColor(R.color.orange_600))
                holder.textViewStatus.setBackgroundResource(R.drawable.bg_status_pending)
            }
            com.bithermmanagement.ausencias.models.AbsenceStatus.RECHAZADA -> {
                holder.textViewStatus.text = "✗ Rechazada"
                holder.textViewStatus.setTextColor(holder.itemView.context.getColor(R.color.red_600))
                holder.textViewStatus.setBackgroundResource(R.drawable.bg_status_rejected)
            }
            else -> {
                holder.textViewStatus.text = "❓ Desconocido"
                holder.textViewStatus.setTextColor(holder.itemView.context.getColor(R.color.gray_600))
                holder.textViewStatus.setBackgroundResource(R.drawable.bg_status_unknown)
            }
        }
        
        // Color del indicador según el tipo
        val indicatorColor = when (absence.absenceType) {
            AbsenceType.VACACIONES -> holder.itemView.context.getColor(R.color.absence_vacaciones)
            AbsenceType.PERMISO -> holder.itemView.context.getColor(R.color.absence_permiso)
            AbsenceType.ENFERMEDAD -> holder.itemView.context.getColor(R.color.absence_enfermedad)
            else -> holder.itemView.context.getColor(R.color.absence_otros)
        }
        holder.indicatorType.setBackgroundColor(indicatorColor)
    }

    override fun getItemCount(): Int = absences.size

    fun updateAbsences(newAbsences: List<AbsenceRecord>) {
        absences = newAbsences
        notifyDataSetChanged()
    }
    
    private fun getAbsenceTypeAbbreviation(absenceType: AbsenceType): String {
        return when (absenceType) {
            AbsenceType.VACACIONES -> "V"
            AbsenceType.ENFERMEDAD -> "BJ"
            AbsenceType.PERMISO -> "PR"
            AbsenceType.ASUNTOS_PERSONALES -> "AP"
            AbsenceType.FORMACION -> "F"
            AbsenceType.OTROS -> "O"
        }
    }
}
