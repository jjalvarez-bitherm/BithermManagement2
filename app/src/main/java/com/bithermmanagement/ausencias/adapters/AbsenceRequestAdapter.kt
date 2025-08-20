package com.bithermmanagement.ausencias.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bithermmanagement.R
import com.bithermmanagement.ausencias.models.AbsenceRequest
import com.bithermmanagement.ausencias.models.AbsenceType
import com.bithermmanagement.ausencias.models.RequestPriority
import com.bithermmanagement.ausencias.models.RequestStatus
import java.text.SimpleDateFormat
import java.util.*

class AbsenceRequestAdapter(
    private var requests: List<AbsenceRequest> = emptyList(),
    private val onApprove: ((AbsenceRequest) -> Unit)? = null,
    private val onReject: ((AbsenceRequest) -> Unit)? = null,
    private val onCancel: ((AbsenceRequest) -> Unit)? = null,
    private val onEdit: ((AbsenceRequest) -> Unit)? = null
) : RecyclerView.Adapter<AbsenceRequestAdapter.RequestViewHolder>() {

    class RequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewEmployeeName: TextView = itemView.findViewById(R.id.textViewEmployeeName)
        val textViewEmployeeEmail: TextView = itemView.findViewById(R.id.textViewEmployeeEmail)
        val textViewEmployeeRole: TextView = itemView.findViewById(R.id.textViewEmployeeRole)
        val textViewStartDate: TextView = itemView.findViewById(R.id.textViewStartDate)
        val textViewEndDate: TextView = itemView.findViewById(R.id.textViewEndDate)
        val textViewTotalDays: TextView = itemView.findViewById(R.id.textViewTotalDays)
        val chipAbsenceType: com.google.android.material.chip.Chip = itemView.findViewById(R.id.chipAbsenceType)
        val textViewDescription: TextView = itemView.findViewById(R.id.textViewDescription)
        val chipStatus: com.google.android.material.chip.Chip = itemView.findViewById(R.id.chipStatus)
        val chipPriority: com.google.android.material.chip.Chip = itemView.findViewById(R.id.chipPriority)
        val textViewCreatedAt: TextView = itemView.findViewById(R.id.textViewCreatedAt)
        val buttonApprove: Button = itemView.findViewById(R.id.buttonApprove)
        val buttonReject: Button = itemView.findViewById(R.id.buttonReject)
        val buttonCancel: Button = itemView.findViewById(R.id.buttonCancel)
        val buttonEdit: Button = itemView.findViewById(R.id.buttonEdit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_absence_request, parent, false)
        return RequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val request = requests[position]
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        
        // Configurar información del empleado
        holder.textViewEmployeeName.text = request.employeeName
        holder.textViewEmployeeEmail.text = request.employeeEmail
        holder.textViewEmployeeRole.text = request.employeeRole
        
        // Configurar fechas
        holder.textViewStartDate.text = dateFormat.format(request.startDate)
        holder.textViewEndDate.text = dateFormat.format(request.endDate)
        holder.textViewTotalDays.text = "${request.totalDays} días"
        
        // Configurar tipo de ausencia
        holder.chipAbsenceType.text = request.absenceType.displayName
        holder.chipAbsenceType.setTextColor(
            android.graphics.Color.parseColor(request.absenceType.color)
        )
        
        // Configurar descripción
        holder.textViewDescription.text = request.description
        
        // Configurar estado
        holder.chipStatus.text = request.status.displayName
        holder.chipStatus.setChipBackgroundColorResource(
            android.graphics.Color.parseColor(request.status.color)
        )
        
        // Configurar prioridad
        holder.chipPriority.text = request.priority.displayName
        holder.chipPriority.setChipBackgroundColorResource(
            android.graphics.Color.parseColor(request.priority.color)
        )
        
        // Configurar fecha de creación
        holder.textViewCreatedAt.text = "Creado: ${dateFormat.format(request.createdAt)}"
        
        // Configurar botones según el estado
        when (request.status) {
            RequestStatus.PENDIENTE -> {
                holder.buttonApprove.visibility = View.VISIBLE
                holder.buttonReject.visibility = View.VISIBLE
                holder.buttonCancel.visibility = View.VISIBLE
                holder.buttonEdit.visibility = View.VISIBLE
            }
            RequestStatus.EN_REVISION -> {
                holder.buttonApprove.visibility = View.VISIBLE
                holder.buttonReject.visibility = View.VISIBLE
                holder.buttonCancel.visibility = View.VISIBLE
                holder.buttonEdit.visibility = View.GONE
            }
            RequestStatus.APROBADA -> {
                holder.buttonApprove.visibility = View.GONE
                holder.buttonReject.visibility = View.GONE
                holder.buttonCancel.visibility = View.VISIBLE
                holder.buttonEdit.visibility = View.GONE
            }
            RequestStatus.RECHAZADA -> {
                holder.buttonApprove.visibility = View.GONE
                holder.buttonReject.visibility = View.GONE
                holder.buttonCancel.visibility = View.VISIBLE
                holder.buttonEdit.visibility = View.VISIBLE
            }
            RequestStatus.CANCELADA -> {
                holder.buttonApprove.visibility = View.GONE
                holder.buttonReject.visibility = View.GONE
                holder.buttonCancel.visibility = View.GONE
                holder.buttonEdit.visibility = View.GONE
            }
            RequestStatus.PASADA -> {
                holder.buttonApprove.visibility = View.GONE
                holder.buttonReject.visibility = View.GONE
                holder.buttonCancel.visibility = View.GONE
                holder.buttonEdit.visibility = View.GONE
            }
        }
        
        // Configurar click listeners
        holder.buttonApprove.setOnClickListener { onApprove?.invoke(request) }
        holder.buttonReject.setOnClickListener { onReject?.invoke(request) }
        holder.buttonCancel.setOnClickListener { onCancel?.invoke(request) }
        holder.buttonEdit.setOnClickListener { onEdit?.invoke(request) }
    }

    override fun getItemCount(): Int = requests.size

    fun updateData(newRequests: List<AbsenceRequest>) {
        requests = newRequests
        notifyDataSetChanged()
    }
}
