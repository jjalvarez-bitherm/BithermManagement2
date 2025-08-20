package com.bithermmanagement.ausencias.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.bithermmanagement.ausencias.models.*
import com.bithermmanagement.ausencias.repository.AbsenceRepository
import com.bithermmanagement.ausencias.services.GoogleSheetsTransferService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class AbsenceViewModel @Inject constructor(
    private val absenceRepository: AbsenceRepository,
    private val googleSheetsTransferService: GoogleSheetsTransferService
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
                
                // Recargar ausencias del calendario para el mes actual
                val calendar = Calendar.getInstance()
                calendar.time = startDate
                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH)
                loadAbsencesByMonth(year, month)
                
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
    
    // Métodos para filtros múltiples
    fun setStatusFilter(statuses: List<RequestStatus>) {
        // Implementar filtro múltiple de estados
    }
    
    fun setTypeFilter(types: List<AbsenceType>) {
        // Implementar filtro múltiple de tipos
    }
    
    fun setPriorityFilter(priorities: List<RequestPriority>) {
        // Implementar filtro múltiple de prioridades
    }
    
    fun clearAllFilters() {
        _selectedStatus.value = null
        _selectedType.value = null
        _selectedPriority.value = null
    }
    
    // Métodos para cargar datos
    fun loadAbsenceRequests() {
        loadPendingRequests()
    }
    
    fun loadAbsencesByMonth(year: Int, month: Int) {
        viewModelScope.launch {
            try {
                // SIEMPRE cargar desde AUSENCIAS-LOG (datos correctos) en lugar de base de datos local
                _isLoading.value = true
                Log.d("AbsenceViewModel", "Cargando ausencias desde AUSENCIAS-LOG para $month/$year")
                
                val monthAbsences = loadAbsencesFromGoogleSheets(year, month)
                _absences.value = monthAbsences
                
                // Cargar estadísticas para el mes
                loadStatisticsForMonth(year, month)
                
                _isLoading.value = false
                Log.d("AbsenceViewModel", "Datos cargados desde AUSENCIAS-LOG: ${monthAbsences.size} registros")
            } catch (e: Exception) {
                Log.e("AbsenceViewModel", "Error cargando desde AUSENCIAS-LOG, usando fallback local", e)
                // Solo como fallback si falla Google Sheets, usar base de datos local
                try {
                    val localAbsences = absenceRepository.getAbsencesByMonth(year, month)
                    _absences.value = localAbsences
                    loadStatisticsForMonth(year, month)
                    Log.d("AbsenceViewModel", "Fallback: datos cargados desde base de datos local: ${localAbsences.size} registros")
                } catch (localError: Exception) {
                    _errorMessage.value = "Error al cargar ausencias del mes: ${localError.message}"
                }
                _isLoading.value = false
            }
        }
    }
    
    suspend fun loadAllAbsencesFromLogs(): List<AbsenceRecord> {
        try {
            Log.d("AbsenceViewModel", "Cargando todas las ausencias desde AUSENCIAS-LOGS")
            
            // Usar el servicio para leer todos los datos de AUSENCIAS-LOGS (sin filtro de mes)
            val allAbsences = googleSheetsTransferService.readAllAbsencesFromLogs()
            
            Log.d("AbsenceViewModel", "Todas las ausencias cargadas: ${allAbsences.size} registros")
            
            return allAbsences
        } catch (e: Exception) {
            Log.e("AbsenceViewModel", "Error cargando todas las ausencias desde Google Sheets: ${e.message}", e)
            // Si falla, retornar lista vacía
            return emptyList()
        }
    }
    
    suspend fun loadHolidaysForMonth(year: Int, month: Int): Map<String, String> {
        try {
            Log.d("AbsenceViewModel", "Cargando festivos para $month/$year desde JSON local")
            
            // Usar el servicio para leer festivos del JSON local
            val holidays = googleSheetsTransferService.readHolidaysFromJson(year, month)
            
            Log.d("AbsenceViewModel", "Festivos cargados para $month/$year: ${holidays.size}")
            
            return holidays
        } catch (e: Exception) {
            Log.e("AbsenceViewModel", "Error cargando festivos desde JSON: ${e.message}", e)
            // Si falla, retornar mapa vacío
            return emptyMap()
        }
    }
    
    private suspend fun loadAbsencesFromGoogleSheets(year: Int, month: Int): List<AbsenceRecord> {
        try {
            Log.d("AbsenceViewModel", "Cargando ausencias desde AUSENCIAS-LOGS para $month/$year")
            
            // Usar el servicio para leer datos de AUSENCIAS-LOGS
            val absenceRecords = googleSheetsTransferService.readAbsencesFromLogs(year, month)
            
            Log.d("AbsenceViewModel", "Ausencias cargadas: ${absenceRecords.size} registros")
            
            return absenceRecords
        } catch (e: Exception) {
            Log.e("AbsenceViewModel", "Error cargando ausencias desde Google Sheets: ${e.message}", e)
            // Si falla, intentar cargar desde base de datos local como fallback
            return absenceRepository.getAbsencesByMonth(year, month)
        }
    }
    
    private fun loadStatisticsForMonth(year: Int, month: Int) {
        viewModelScope.launch {
            try {
                val monthAbsences = _absences.value
                
                val stats = mutableMapOf<String, Int>()
                stats["enfermedad"] = monthAbsences.count { it.absenceType == AbsenceType.ENFERMEDAD }
                stats["permiso"] = monthAbsences.count { it.absenceType == AbsenceType.PERMISO }
                stats["vacaciones_disfrutadas"] = monthAbsences.count { 
                    it.absenceType == AbsenceType.VACACIONES && it.status == AbsenceStatus.APROBADA 
                }
                stats["vacaciones_pendientes"] = monthAbsences.count { 
                    it.absenceType == AbsenceType.VACACIONES && it.status == AbsenceStatus.PENDIENTE 
                }
                stats["solicitudes"] = monthAbsences.count { it.status == AbsenceStatus.PENDIENTE }
                
                _statistics.value = stats
            } catch (e: Exception) {
                _errorMessage.value = "Error al cargar estadísticas: ${e.message}"
            }
        }
    }
    
    fun syncWithGoogleSheets() {
        viewModelScope.launch {
            try {
                Log.d("AbsenceViewModel", "Iniciando sincronización con Google Sheets")
                _isLoading.value = true
                _errorMessage.value = null
                
                // Ejecutar la transferencia real de datos
                Log.d("AbsenceViewModel", "Llamando a transferDataFromGoogleSheets")
                transferDataFromGoogleSheets()
                
                Log.d("AbsenceViewModel", "Transferencia completada exitosamente")
                _isLoading.value = false
                
            } catch (e: Exception) {
                Log.e("AbsenceViewModel", "Error en sincronización: ${e.message}", e)
                _errorMessage.value = "Error en sincronización: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    fun forceSyncAllAbsences() {
        viewModelScope.launch {
            try {
                Log.d("AbsenceViewModel", "Iniciando sincronización FORZADA de todas las ausencias")
                _isLoading.value = true
                _errorMessage.value = null
                
                // Ejecutar la transferencia forzada de datos
                Log.d("AbsenceViewModel", "Llamando a forceTransferAllAbsences")
                val absenceRecords = googleSheetsTransferService.forceTransferAllAbsences()
                
                Log.d("AbsenceViewModel", "Transferencia forzada completada, ${absenceRecords.size} registros")
                
                // Guardar en base de datos local
                Log.d("AbsenceViewModel", "Guardando en base de datos local")
                saveToLocalDatabase(absenceRecords)
                
                Log.d("AbsenceViewModel", "Proceso forzado completado exitosamente")
                _isLoading.value = false
                
            } catch (e: Exception) {
                Log.e("AbsenceViewModel", "Error en sincronización forzada: ${e.message}", e)
                _errorMessage.value = "Error en sincronización forzada: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    private suspend fun transferDataFromGoogleSheets() {
        try {
            Log.d("AbsenceViewModel", "Iniciando transferencia de datos desde Google Sheets")
            
            // Usar el servicio real para transferir datos
            Log.d("AbsenceViewModel", "Llamando al servicio de transferencia")
            val absenceRecords = googleSheetsTransferService.transferDataFromCuadroToLogs()
            
            Log.d("AbsenceViewModel", "Transferencia completada, ${absenceRecords.size} registros obtenidos")
            
            // Guardar en base de datos local
            Log.d("AbsenceViewModel", "Guardando en base de datos local")
            saveToLocalDatabase(absenceRecords)
            
            Log.d("AbsenceViewModel", "Proceso completado exitosamente")
            
        } catch (e: Exception) {
            Log.e("AbsenceViewModel", "Error en transferencia: ${e.message}", e)
            throw Exception("Error en transferencia: ${e.message}")
        }
    }
    
    private suspend fun saveToLocalDatabase(records: List<AbsenceRecord>) {
        Log.d("AbsenceViewModel", "Guardando ${records.size} registros en base de datos local")
        
        // Guardar en base de datos local
        records.forEachIndexed { index, record ->
            try {
                absenceRepository.insertAbsence(record)
                if (index < 3) { // Log solo los primeros 3 para no saturar
                    Log.d("AbsenceViewModel", "Registro ${index + 1} guardado: ${record.employeeName} - ${record.absenceType.displayName}")
                }
            } catch (e: Exception) {
                Log.e("AbsenceViewModel", "Error guardando registro ${index + 1}: ${e.message}")
                throw e
            }
        }
        
        Log.d("AbsenceViewModel", "Todos los registros guardados exitosamente")
    }
    
    fun editAbsenceRequest(request: AbsenceRequest) {
        // Implementar edición
    }
    
    fun approveAbsenceRequest(request: AbsenceRequest) {
        approveRequest(request.id, "admin", "Administrador")
    }
    
    fun rejectAbsenceRequest(request: AbsenceRequest) {
        rejectRequest(request.id, "admin", "Administrador", "Rechazado por administrador")
    }
    
    fun cancelAbsenceRequest(request: AbsenceRequest) {
        cancelRequest(request.id, "admin", "Administrador", "Cancelado por administrador")
    }
    
    fun createAbsenceRequest(request: AbsenceRequest) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                val requestId = absenceRepository.createAbsenceRequest(
                    employeeId = request.employeeId,
                    employeeName = request.employeeName,
                    employeeEmail = request.employeeEmail,
                    employeeRole = request.employeeRole,
                    startDate = request.startDate,
                    endDate = request.endDate,
                    absenceType = request.absenceType,
                    description = request.description,
                    priority = request.priority
                )
                
                // Actualizar estado
                _uiState.value = _uiState.value.copy(
                    lastCreatedRequestId = requestId,
                    showSuccessMessage = true,
                    successMessage = "Solicitud creada correctamente"
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
    
    fun calculateWorkingDays(startDate: Date, endDate: Date, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            try {
                val workingDays = googleSheetsTransferService.calculateWorkingDays(startDate, endDate)
                onResult(workingDays)
            } catch (e: Exception) {
                // En caso de error, calcular días naturales
                val calendar = Calendar.getInstance()
                calendar.time = startDate
                var days = 0
                val endCalendar = Calendar.getInstance()
                endCalendar.time = endDate
                
                while (!calendar.after(endCalendar)) {
                    if (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY && 
                        calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
                        days++
                    }
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
                onResult(days)
            }
        }
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
