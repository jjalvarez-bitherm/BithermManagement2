package com.bithermmanagement.ausencias.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bithermmanagement.ausencias.adapters.AbsenceRequestAdapter
import com.bithermmanagement.ausencias.databinding.FragmentAusenciasGestionBinding
import com.bithermmanagement.ausencias.models.RequestStatus
import com.bithermmanagement.ausencias.models.RequestPriority
import com.bithermmanagement.ausencias.models.AbsenceType
import com.bithermmanagement.ausencias.viewmodels.AbsenceViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FragmentAusenciasGestion : Fragment() {
    
    private var _binding: FragmentAusenciasGestionBinding? = null
    private val binding get() = _binding!!
    
    @Inject
    lateinit var absenceViewModel: AbsenceViewModel
    
    private lateinit var requestAdapter: AbsenceRequestAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAusenciasGestionBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        setupObservers()
        setupClickListeners()
        
        // Cargar datos iniciales
        loadInitialData()
    }
    
    private fun setupUI() {
        // Configurar RecyclerView
        requestAdapter = AbsenceRequestAdapter(
            onApprove = { request -> showApproveDialog(request.id) },
            onReject = { request -> showRejectDialog(request.id) },
            onCancel = { request -> showCancelDialog(request.id) },
            onEdit = { request -> navigateToEditRequest(request.id) },
            onViewDetails = { request -> navigateToRequestDetails(request.id) }
        )
        
        binding.recyclerViewRequests.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = requestAdapter
        }
        
        // Configurar filtros iniciales
        setupFilterChips()
    }
    
    private fun setupFilterChips() {
        // Configurar chips de estado
        binding.chipGroupStatus.apply {
            setOnCheckedStateChangeListener { _, checkedIds ->
                if (checkedIds.isNotEmpty()) {
                    val status = when (checkedIds.first()) {
                        binding.chipPendiente.id -> RequestStatus.PENDIENTE
                        binding.chipEnRevision.id -> RequestStatus.EN_REVISION
                        binding.chipAprobada.id -> RequestStatus.APROBADA
                        binding.chipRechazada.id -> RequestStatus.RECHAZADA
                        binding.chipCancelada.id -> RequestStatus.CANCELADA
                        else -> null
                    }
                    absenceViewModel.setSelectedStatus(status)
                } else {
                    absenceViewModel.setSelectedStatus(null)
                }
            }
        }
        
        // Configurar chips de tipo
        binding.chipGroupType.apply {
            setOnCheckedStateChangeListener { _, checkedIds ->
                if (checkedIds.isNotEmpty()) {
                    val type = when (checkedIds.first()) {
                        binding.chipVacaciones.id -> AbsenceType.VACACIONES
                        binding.chipPermiso.id -> AbsenceType.PERMISO
                        binding.chipEnfermedad.id -> AbsenceType.ENFERMEDAD
                        binding.chipAsuntosPersonales.id -> AbsenceType.ASUNTOS_PERSONALES
                        binding.chipFormacion.id -> AbsenceType.FORMACION
                        binding.chipOtros.id -> AbsenceType.OTROS
                        else -> null
                    }
                    absenceViewModel.setSelectedType(type)
                } else {
                    absenceViewModel.setSelectedType(null)
                }
            }
        }
        
        // Configurar chips de prioridad
        binding.chipGroupPriority.apply {
            setOnCheckedStateChangeListener { _, checkedIds ->
                if (checkedIds.isNotEmpty()) {
                    val priority = when (checkedIds.first()) {
                        binding.chipBaja.id -> RequestPriority.BAJA
                        binding.chipNormal.id -> RequestPriority.NORMAL
                        binding.chipAlta.id -> RequestPriority.ALTA
                        binding.chipUrgente.id -> RequestPriority.URGENTE
                        else -> null
                    }
                    absenceViewModel.setSelectedPriority(priority)
                } else {
                    absenceViewModel.setSelectedPriority(null)
                }
            }
        }
    }
    
    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            absenceViewModel.requests.collectLatest { requests ->
                requestAdapter.updateRequests(requests)
                updateEmptyState(requests.isEmpty())
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            absenceViewModel.pendingRequests.collectLatest { pendingRequests ->
                updatePendingCount(pendingRequests.size)
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            absenceViewModel.isLoading.collectLatest { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            absenceViewModel.errorMessage.collectLatest { error ->
                error?.let {
                    Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                    absenceViewModel.clearError()
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            absenceViewModel.uiState.collectLatest { uiState ->
                if (uiState.showSuccessMessage) {
                    Toast.makeText(context, uiState.successMessage, Toast.LENGTH_SHORT).show()
                    absenceViewModel.clearSuccessMessage()
                }
            }
        }
    }
    
    private fun setupClickListeners() {
        // Botón de nueva ausencia
        binding.fabNewAbsence.setOnClickListener {
            navigateToNewAbsence()
        }
        
        // Botón de sincronizar
        binding.buttonSync.setOnClickListener {
            syncWithGoogleSheets()
        }
        
        // Botón de limpiar filtros
        binding.buttonClearFilters.setOnClickListener {
            clearAllFilters()
        }
        
        // Botón de exportar
        binding.buttonExport.setOnClickListener {
            exportData()
        }
    }
    
    private fun loadInitialData() {
        // Cargar todas las solicitudes
        absenceViewModel.loadRequestsByEmployee("") // Cargar todas
        absenceViewModel.loadPendingRequests()
        absenceViewModel.loadStatistics()
    }
    
    private fun updateEmptyState(isEmpty: Boolean) {
        binding.apply {
            if (isEmpty) {
                emptyStateContainer.visibility = View.VISIBLE
                recyclerViewRequests.visibility = View.GONE
            } else {
                emptyStateContainer.visibility = View.GONE
                recyclerViewRequests.visibility = View.VISIBLE
            }
        }
    }
    
    private fun updatePendingCount(count: Int) {
        binding.textViewPendingCount.text = count.toString()
        
        // Mostrar badge si hay solicitudes pendientes
        if (count > 0) {
            binding.badgePending.visibility = View.VISIBLE
            binding.badgePending.text = count.toString()
        } else {
            binding.badgePending.visibility = View.GONE
        }
    }
    
    private fun showApproveDialog(requestId: Long) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Aprobar Solicitud")
            .setMessage("¿Estás seguro de que quieres aprobar esta solicitud de ausencia?")
            .setPositiveButton("Aprobar") { _, _ ->
                approveRequest(requestId)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun showRejectDialog(requestId: Long) {
        // Mostrar diálogo con campo de texto para la razón
        val reasonInput = android.widget.EditText(context).apply {
            hint = "Motivo del rechazo"
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Rechazar Solicitud")
            .setMessage("¿Estás seguro de que quieres rechazar esta solicitud?")
            .setView(reasonInput)
            .setPositiveButton("Rechazar") { _, _ ->
                val reason = reasonInput.text.toString()
                if (reason.isNotBlank()) {
                    rejectRequest(requestId, reason)
                } else {
                    Toast.makeText(context, "Debes especificar un motivo", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun showCancelDialog(requestId: Long) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Cancelar Solicitud")
            .setMessage("¿Estás seguro de que quieres cancelar esta solicitud?")
            .setPositiveButton("Cancelar") { _, _ ->
                cancelRequest(requestId)
            }
            .setNegativeButton("No", null)
            .show()
    }
    
    private fun approveRequest(requestId: Long) {
        // Obtener información del usuario actual (esto se implementará con el sistema de autenticación)
        val currentUserId = "admin" // Temporal
        val currentUserRole = "Administrador" // Temporal
        
        absenceViewModel.approveRequest(requestId, currentUserId, currentUserRole)
    }
    
    private fun rejectRequest(requestId: Long, reason: String) {
        val currentUserId = "admin" // Temporal
        val currentUserRole = "Administrador" // Temporal
        
        absenceViewModel.rejectRequest(requestId, currentUserId, currentUserRole, reason)
    }
    
    private fun cancelRequest(requestId: Long) {
        val currentUserId = "admin" // Temporal
        val currentUserRole = "Administrador" // Temporal
        
        absenceViewModel.cancelRequest(requestId, currentUserId, currentUserRole)
    }
    
    private fun navigateToNewAbsence() {
        // Navegar al fragmento de nueva ausencia
        Toast.makeText(context, "Nueva ausencia", Toast.LENGTH_SHORT).show()
    }
    
    private fun navigateToEditRequest(requestId: Long) {
        // Navegar al fragmento de edición
        Toast.makeText(context, "Editar solicitud $requestId", Toast.LENGTH_SHORT).show()
    }
    
    private fun navigateToRequestDetails(requestId: Long) {
        // Navegar al fragmento de detalles
        Toast.makeText(context, "Ver detalles de solicitud $requestId", Toast.LENGTH_SHORT).show()
    }
    
    private fun syncWithGoogleSheets() {
        // Sincronizar con Google Sheets
        Toast.makeText(context, "Sincronizando con Google Sheets...", Toast.LENGTH_SHORT).show()
    }
    
    private fun clearAllFilters() {
        // Limpiar todos los filtros
        binding.chipGroupStatus.clearCheck()
        binding.chipGroupType.clearCheck()
        binding.chipGroupPriority.clearCheck()
        
        absenceViewModel.setSelectedStatus(null)
        absenceViewModel.setSelectedType(null)
        absenceViewModel.setSelectedPriority(null)
    }
    
    private fun exportData() {
        // Exportar datos
        Toast.makeText(context, "Exportando datos...", Toast.LENGTH_SHORT).show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
