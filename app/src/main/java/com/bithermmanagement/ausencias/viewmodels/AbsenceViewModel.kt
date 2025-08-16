package com.bithermmanagement.ausencias.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bithermmanagement.ausencias.models.*
import com.bithermmanagement.ausencias.repository.AbsenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class AbsenceViewModel @Inject constructor(
    private val absenceRepository: AbsenceRepository
) : ViewModel() {
    
    // ===== ESTADO DE LA UI =====
    private val _uiState = MutableStateFlow(AbsenceUiState())
    val uiState: StateFlow<AbsenceUiState> = _uiState.asStateFlow()
    
    // ===== FILTROS Y BÚSQUEDAS =====
    private val _selectedDate = MutableStateFlow(Date())
    val selectedDate: StateFlow<Date> = _selectedDate.asStateFlow()
    
    private val _dateRange = MutableStateFlow(Pair(Date(), Date()))
    val dateRange: StateFlow<Pair<Date, Date>> = _dateRange.asStateFlow()
    
    private val _selectedStatus = MutableStateFlow<RequestStatus?>(null)
    val selectedStatus: StateFlow<RequestStatus?> = _selectedStatus.asStateFlow()
    
    private val _selectedType = MutableStateFlow<AbsenceType?>(null)
    val selectedType: StateFlow<AbsenceType?> = _selectedType.asStateFlow()
    
    private val _selectedPriority = MutableStateFlow<RequestPriority?>(null)
    val selectedPriority: StateFlow<RequestPriority?> = _selectedPriority.asStateFlow()
    
    // ===== DATOS =====
    private val _absences = MutableStateFlow<List<AbsenceRecord>>(emptyList())
    val absences: StateFlow<List<AbsenceRecord>> = _absences.asStateFlow()
    
    private val _requests = MutableStateFlow<List<AbsenceRequest>>(emptyList())
    val requests: StateFlow<List<AbsenceRequest>> = _requests.asStateFlow()
    
    private val _pendingRequests = MutableStateFlow<List<AbsenceRequest>>(emptyList())
    val pendingRequests: StateFlow<List<AbsenceRequest>> = _pendingRequests.asStateFlow()
    
    private val _logs = MutableStateFlow<List<AbsenceLog>>(emptyList())
    val logs: StateFlow<List<AbsenceLog>> = _logs.asStateFlow()
    
    // ===== ESTADÍSTICAS =====
    private val _statistics = MutableStateFlow<Map<String, Int>>(emptyMap())
    val statistics: StateFlow<Map<String, Int>> = _statistics.asStateFlow()
    
    // ===== ESTADO DE CARGA =====
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    init {
        loadInitialData()
        observeData()
    }
    
    // ===== INICIALIZACIÓN =====
    
    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Cargar estadísticas
                loadStatistics()
                
                // Cargar solicitudes pendientes
                loadPendingRequests()
                
                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = "Error al cargar datos: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    private fun observeData() {
        // Observar cambios en los filtros y actualizar datos
        combine(
            selectedDate,
            selectedStatus,
            selectedType,
            selectedPriority
        ) { date, status, type, priority ->
            // Aplicar filtros
            applyFilters(date, status, type, priority)
        }.launchIn(viewModelScope)
    }
    
    // ===== CARGA DE DATOS =====
    
    fun loadAbsencesForDate(date: Date) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val absences = absenceRepository.getAbsencesForDate(date).first()
                _absences.value = absences
                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = "Error al cargar ausencias: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    fun loadAbsencesForDateRange(startDate: Date, endDate: Date) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val absences = absenceRepository.getAbsencesForDateRange(startDate, endDate).first()
                _absences.value = absences
                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = "Error al cargar ausencias: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    fun loadRequestsByEmployee(employeeId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val requests = absenceRepository.getRequestsByEmployee(employeeId).first()
                _requests.value = requests
                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = "Error al cargar solicitudes: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    fun loadPendingRequests() {
        viewModelScope.launch {
            try {
                val requests = absenceRepository.getPendingRequests().first()
                _pendingRequests.value = requests
            } catch (e: Exception) {
                _errorMessage.value = "Error al cargar solicitudes pendientes: ${e.message}"
            }
        }
    }
    
    fun loadStatistics(employeeId: String? = null) {
        viewModelScope.launch {
            try {
                val stats = absenceRepository.getAbsenceStatistics(employeeId)
                _statistics.value = stats
            } catch (e: Exception) {
                _errorMessage.value = "Error al cargar estadísticas: ${e.message}"
            }
        }
    }
    
    // ===== OPERACIONES =====
    
    fun createAbsenceRequest(
        employeeId: String,
        employeeName: String,
        employeeEmail: String,
        employeeRole: String,
        startDate: Date,
        endDate: Date,
        absenceType: AbsenceType,
        description: String,
        priority: RequestPriority = RequestPriority.NORMAL
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                val requestId = absenceRepository.createAbsenceRequest(
                    employeeId = employeeId,
                    employeeName = employeeName,
                    employeeEmail = employeeEmail,
                    employeeRole = employeeRole,
                    startDate = startDate,
                    endDate = endDate,
                    absenceType = absenceType,
                    description = description,
                    priority = priority
                )
                
                // Recargar datos
                loadPendingRequests()
                loadStatistics()
                
                _uiState.value = _uiState.value.copy(
                    lastCreatedRequestId = requestId,
                    showSuccessMessage = true
                )
                
                _isLoading.value = false
                
                // Ocultar mensaje de éxito después de un tiempo
                kotlinx.coroutines.delay(3000)
                _uiState.value = _uiState.value.copy(showSuccessMessage = false)
                
            } catch (e: Exception) {
                _errorMessage.value = "Error al crear solicitud: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    fun approveRequest(requestId: Long, approvedBy: String, approvedByRole: String, notes: String? = null) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                absenceRepository.approveAbsenceRequest(requestId, approvedBy, approvedByRole, notes)
                
                // Recargar datos
                loadPendingRequests()
                loadStatistics()
                
                _uiState.value = _uiState.value.copy(
                    showSuccessMessage = true,
                    successMessage = "Solicitud aprobada correctamente"
                )
                
                _isLoading.value = false
                
                // Ocultar mensaje de éxito después de un tiempo
                kotlinx.coroutines.delay(3000)
                _uiState.value = _uiState.value.copy(showSuccessMessage = false)
                
            } catch (e: Exception) {
                _errorMessage.value = "Error al aprobar solicitud: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    fun rejectRequest(requestId: Long, rejectedBy: String, rejectedByRole: String, reason: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                absenceRepository.rejectAbsenceRequest(requestId, rejectedBy, rejectedByRole, reason)
                
                // Recargar datos
                loadPendingRequests()
                loadStatistics()
                
                _uiState.value = _uiState.value.copy(
                    showSuccessMessage = true,
                    successMessage = "Solicitud rechazada correctamente"
                )
                
                _isLoading.value = false
                
                // Ocultar mensaje de éxito después de un tiempo
                kotlinx.coroutines.delay(3000)
                _uiState.value = _uiState.value.copy(showSuccessMessage = false)
                
            } catch (e: Exception) {
                _errorMessage.value = "Error al rechazar solicitud: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    fun cancelRequest(requestId: Long, cancelledBy: String, cancelledByRole: String, reason: String? = null) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                absenceRepository.cancelAbsenceRequest(requestId, cancelledBy, cancelledByRole, reason)
                
                // Recargar datos
                loadPendingRequests()
                loadStatistics()
                
                _uiState.value = _uiState.value.copy(
                    showSuccessMessage = true,
                    successMessage = "Solicitud cancelada correctamente"
                )
                
                _isLoading.value = false
                
                // Ocultar mensaje de éxito después de un tiempo
                kotlinx.coroutines.delay(3000)
                _uiState.value = _uiState.value.copy(showSuccessMessage = false)
                
            } catch (e: Exception) {
                _errorMessage.value = "Error al cancelar solicitud: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    // ===== FILTROS =====
    
    fun setSelectedDate(date: Date) {
        _selectedDate.value = date
        loadAbsencesForDate(date)
    }
    
    fun setDateRange(startDate: Date, endDate: Date) {
        _dateRange.value = Pair(startDate, endDate)
        loadAbsencesForDateRange(startDate, endDate)
    }
    
    fun setSelectedStatus(status: RequestStatus?) {
        _selectedStatus.value = status
    }
    
    fun setSelectedType(type: AbsenceType?) {
        _selectedType.value = type
    }
    
    fun setSelectedPriority(priority: RequestPriority?) {
        _selectedPriority.value = priority
    }
    
    private fun applyFilters(date: Date, status: RequestStatus?, type: AbsenceType?, priority: RequestPriority?) {
        // Aplicar filtros combinados
        viewModelScope.launch {
            try {
                var filteredRequests = absenceRepository.getAllRequests().first()
                
                // Filtrar por estado
                if (status != null) {
                    filteredRequests = filteredRequests.filter { it.status == status }
                }
                
                // Filtrar por tipo
                if (type != null) {
                    filteredRequests = filteredRequests.filter { it.absenceType == type }
                }
                
                // Filtrar por prioridad
                if (priority != null) {
                    filteredRequests = filteredRequests.filter { it.priority == priority }
                }
                
                _requests.value = filteredRequests
                
            } catch (e: Exception) {
                _errorMessage.value = "Error al aplicar filtros: ${e.message}"
            }
        }
    }
    
    // ===== UTILIDADES =====
    
    fun clearError() {
        _errorMessage.value = null
    }
    
    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(showSuccessMessage = false)
    }
}

// ===== ESTADO DE LA UI =====

data class AbsenceUiState(
    val lastCreatedRequestId: Long = 0L,
    val showSuccessMessage: Boolean = false,
    val successMessage: String = "",
    val selectedTab: Int = 0
)
