package com.bithermmanagement.ausencias.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bithermmanagement.ausencias.databinding.ItemAbsenceRequestBinding
import com.bithermmanagement.ausencias.models.AbsenceRequest
import com.bithermmanagement.ausencias.models.RequestStatus
import com.bithermmanagement.ausencias.models.RequestPriority
import java.text.SimpleDateFormat
import java.util.*

class AbsenceRequestAdapter(
    private val onApprove: (AbsenceRequest) -> Unit,
    private val onReject: (AbsenceRequest) -> Unit,
    private val onCancel: (AbsenceRequest) -> Unit,
    private val onEdit: (AbsenceRequest) -> Unit,
    private val onViewDetails: (AbsenceRequest) -> Unit
) : RecyclerView.Adapter<AbsenceRequestAdapter.RequestViewHolder>() {
    
    private var requests: List<AbsenceRequest> = emptyList()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    
    fun updateRequests(newRequests: List<AbsenceRequest>) {
        requests = newRequests
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val binding = ItemAbsenceRequestBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RequestViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        holder.bind(requests[position])
    }
    
    override fun getItemCount(): Int = requests.size
    
    inner class RequestViewHolder(
        private val binding: ItemAbsenceRequestBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(request: AbsenceRequest) {
            binding.apply {
                // Información del empleado
                textViewEmployeeName.text = request.employeeName
                textViewEmployeeEmail.text = request.employeeEmail
                textViewEmployeeRole.text = request.employeeRole
                
                // Fechas
                textViewStartDate.text = dateFormat.format(request.startDate)
                textViewEndDate.text = dateFormat.format(request.endDate)
                textViewTotalDays.text = "${request.totalDays} días"
                
                // Tipo de ausencia
                textViewAbsenceType.text = request.absenceType.displayName
                chipAbsenceType.chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor(request.absenceType.color)
                )
                
                // Estado
                textViewStatus.text = request.status.displayName
                chipStatus.chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor(request.status.color)
                )
                
                // Prioridad
                textViewPriority.text = request.priority.displayName
                chipPriority.chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor(request.priority.color)
                )
                
                // Descripción
                textViewDescription.text = request.description
                
                // Fecha de creación
                textViewCreatedAt.text = "Creada: ${dateFormat.format(request.createdAt)}"
                
                // Configurar botones según el estado
                setupActionButtons(request)
                
                // Configurar click listener para ver detalles
                root.setOnClickListener {
                    onViewDetails(request)
                }
            }
        }
        
        private fun setupActionButtons(request: AbsenceRequest) {
            binding.apply {
                // Ocultar todos los botones primero
                buttonApprove.visibility = android.view.View.GONE
                buttonReject.visibility = android.view.View.GONE
                buttonCancel.visibility = android.view.View.GONE
                buttonEdit.visibility = android.view.View.GONE
                
                when (request.status) {
                    RequestStatus.PENDIENTE -> {
                        // Solo mostrar botones de aprobar/rechazar para administradores
                        buttonApprove.visibility = android.view.View.VISIBLE
                        buttonReject.visibility = android.view.View.VISIBLE
                        buttonCancel.visibility = android.view.View.VISIBLE
                    }
                    RequestStatus.EN_REVISION -> {
                        // Mostrar botones de aprobar/rechazar
                        buttonApprove.visibility = android.view.View.VISIBLE
                        buttonReject.visibility = android.view.View.VISIBLE
                        buttonCancel.visibility = android.view.View.VISIBLE
                    }
                    RequestStatus.APROBADA -> {
                        // Solo mostrar botón de cancelar
                        buttonCancel.visibility = android.view.View.VISIBLE
                    }
                    RequestStatus.RECHAZADA -> {
                        // Mostrar botón de editar para reenviar
                        buttonEdit.visibility = android.view.View.VISIBLE
                    }
                    RequestStatus.CANCELADA -> {
                        // No mostrar botones de acción
                    }
                }
                
                // Configurar click listeners
                buttonApprove.setOnClickListener {
                    onApprove(request)
                }
                
                buttonReject.setOnClickListener {
                    onReject(request)
                }
                
                buttonCancel.setOnClickListener {
                    onCancel(request)
                }
                
                buttonEdit.setOnClickListener {
                    onEdit(request)
                }
            }
        }
    }
}
