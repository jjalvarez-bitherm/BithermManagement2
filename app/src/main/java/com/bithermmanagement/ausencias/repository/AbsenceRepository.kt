package com.bithermmanagement.ausencias.repository

import com.bithermmanagement.ausencias.dao.AbsenceRecordDao
import com.bithermmanagement.ausencias.dao.AbsenceRequestDao
import com.bithermmanagement.ausencias.dao.AbsenceLogDao
import com.bithermmanagement.ausencias.models.*
import com.bithermmanagement.ausencias.services.GoogleSheetsTransferService
import kotlinx.coroutines.flow.Flow
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import java.util.Calendar

@Singleton
class AbsenceRepository @Inject constructor(
    private val absenceRecordDao: AbsenceRecordDao,
    private val absenceRequestDao: AbsenceRequestDao,
    private val absenceLogDao: AbsenceLogDao,
    private val googleSheetsTransferService: GoogleSheetsTransferService
) {
    
    // ===== ABSENCE RECORDS =====
    
    fun getAllAbsences(): Flow<List<AbsenceRecord>> = absenceRecordDao.getAllAbsences()
    
    fun getAbsencesByEmployee(employeeId: String): Flow<List<AbsenceRecord>> = 
        absenceRecordDao.getAbsencesByEmployee(employeeId)
    
    fun getAbsencesByStatus(status: AbsenceStatus): Flow<List<AbsenceRecord>> = 
        absenceRecordDao.getAbsencesByStatus(status)
    
    fun getAbsencesByType(type: AbsenceType): Flow<List<AbsenceRecord>> = 
        absenceRecordDao.getAbsencesByType(type)
    
    fun getAbsencesForDate(date: Date): Flow<List<AbsenceRecord>> = 
        absenceRecordDao.getAbsencesForDate(date)
    
    fun getAbsencesForDateRange(startDate: Date, endDate: Date): Flow<List<AbsenceRecord>> = 
        absenceRecordDao.getAbsencesForDateRange(startDate, endDate)
    
    suspend fun getAbsencesByMonth(year: Int, month: Int): List<AbsenceRecord> {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.time
        
        calendar.add(Calendar.MONTH, 1)
        val endOfMonth = calendar.time
        
        return absenceRecordDao.getAbsencesByMonth(startOfMonth, endOfMonth)
    }
    
    suspend fun getAbsenceById(id: Long): AbsenceRecord? = absenceRecordDao.getAbsenceById(id)
    
    suspend fun insertAbsence(absence: AbsenceRecord): Long = absenceRecordDao.insertAbsence(absence)
    
    suspend fun updateAbsence(absence: AbsenceRecord) = absenceRecordDao.updateAbsence(absence)
    
    suspend fun deleteAbsence(absence: AbsenceRecord) = absenceRecordDao.deleteAbsence(absence)
    
    suspend fun getUnsyncedAbsences(): List<AbsenceRecord> = absenceRecordDao.getUnsyncedAbsences()
    
    // ===== ABSENCE REQUESTS =====
    
    fun getAllRequests(): Flow<List<AbsenceRequest>> = absenceRequestDao.getAllRequests()
    
    fun getRequestsByEmployee(employeeId: String): Flow<List<AbsenceRequest>> = 
        absenceRequestDao.getRequestsByEmployee(employeeId)
    
    fun getRequestsByStatus(status: RequestStatus): Flow<List<AbsenceRequest>> = 
        absenceRequestDao.getRequestsByStatus(status)
    
    fun getPendingRequests(): Flow<List<AbsenceRequest>> = absenceRequestDao.getPendingRequests()
    
    fun getApprovalPendingRequests(): Flow<List<AbsenceRequest>> = absenceRequestDao.getApprovalPendingRequests()
    
    suspend fun getRequestById(id: Long): AbsenceRequest? = absenceRequestDao.getRequestById(id)
    
    suspend fun insertRequest(request: AbsenceRequest): Long = absenceRequestDao.insertRequest(request)
    
    suspend fun updateRequest(request: AbsenceRequest) = absenceRequestDao.updateRequest(request)
    
    suspend fun deleteRequest(request: AbsenceRequest) = absenceRequestDao.deleteRequest(request)
    
    suspend fun getUnsyncedRequests(): List<AbsenceRequest> = absenceRequestDao.getUnsyncedRequests()
    
    // ===== ABSENCE LOGS =====
    
    fun getAllLogs(): Flow<List<AbsenceLog>> = absenceLogDao.getAllLogs()
    
    fun getLogsByAbsenceId(absenceId: Long): Flow<List<AbsenceLog>> = 
        absenceLogDao.getLogsByAbsenceId(absenceId)
    
    fun getLogsByAction(action: LogAction): Flow<List<AbsenceLog>> = 
        absenceLogDao.getLogsByAction(action)
    
    fun getLogsByUser(userId: String): Flow<List<AbsenceLog>> = 
        absenceLogDao.getLogsByUser(userId)
    
    suspend fun insertLog(log: AbsenceLog): Long = absenceLogDao.insertLog(log)
    
    suspend fun getUnsyncedLogs(): List<AbsenceLog> = absenceLogDao.getUnsyncedLogs()
    
    // ===== OPERACIONES COMPUESTAS =====
    
    suspend fun createAbsenceRequest(
        employeeId: String,
        employeeName: String,
        employeeEmail: String,
        employeeRole: String,
        startDate: Date,
        endDate: Date,
        absenceType: AbsenceType,
        description: String,
        priority: RequestPriority = RequestPriority.NORMAL
    ): Long {
        val totalDays = calculateTotalDays(startDate, endDate)
        
        val request = AbsenceRequest(
            employeeId = employeeId,
            employeeName = employeeName,
            employeeEmail = employeeEmail,
            employeeRole = employeeRole,
            startDate = startDate,
            endDate = endDate,
            totalDays = totalDays,
            absenceType = absenceType,
            description = description,
            priority = priority
        )
        
        val requestId = insertRequest(request)
        
        // Crear log de la acción
        val log = AbsenceLog(
            absenceId = requestId,
            absenceType = absenceType,
            action = LogAction.CREAR,
            actionDescription = "Solicitud de ausencia creada",
            performedBy = employeeId,
            performedByRole = employeeRole,
            additionalInfo = "Tipo: ${absenceType.displayName}, Días: $totalDays"
        )
        
        insertLog(log)
        
        // Guardar en Google Sheets AUSENCIAS-LOGS
        try {
            val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            val requestDateFormat = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
            
            googleSheetsTransferService.writeNewAbsenceRequestToLogs(
                employeeId = employeeId,
                employeeName = employeeName,
                absenceType = absenceType.displayName,
                startDate = dateFormat.format(startDate),
                endDate = dateFormat.format(endDate),
                workingDays = totalDays,
                description = description,
                requestDate = requestDateFormat.format(Date())
            )
            
        } catch (e: Exception) {
            // Si falla el guardado en Google Sheets, no fallamos la operación local
            // pero registramos el error
            android.util.Log.e("AbsenceRepository", "Error guardando en Google Sheets: ${e.message}", e)
        }
        
        return requestId
    }
    
    suspend fun approveAbsenceRequest(
        requestId: Long,
        approvedBy: String,
        approvedByRole: String,
        approvalNotes: String? = null
    ) {
        val request = getRequestById(requestId) ?: return
        
        val updatedRequest = request.copy(
            status = RequestStatus.APROBADA,
            approvedBy = approvedBy,
            approvedDate = Date()
        )
        
        updateRequest(updatedRequest)
        
        // Crear log de aprobación
        val log = AbsenceLog(
            absenceId = requestId,
            absenceType = request.absenceType,
            action = LogAction.APROBAR,
            actionDescription = "Solicitud aprobada",
            performedBy = approvedBy,
            performedByRole = approvedByRole,
            additionalInfo = approvalNotes
        )
        
        insertLog(log)
    }
    
    suspend fun rejectAbsenceRequest(
        requestId: Long,
        rejectedBy: String,
        rejectedByRole: String,
        rejectionReason: String
    ) {
        val request = getRequestById(requestId) ?: return
        
        val updatedRequest = request.copy(
            status = RequestStatus.RECHAZADA,
            rejectionReason = rejectionReason
        )
        
        updateRequest(updatedRequest)
        
        // Crear log de rechazo
        val log = AbsenceLog(
            absenceId = requestId,
            absenceType = request.absenceType,
            action = LogAction.RECHAZAR,
            actionDescription = "Solicitud rechazada",
            performedBy = rejectedBy,
            performedByRole = rejectedByRole,
            additionalInfo = rejectionReason
        )
        
        insertLog(log)
    }
    
    suspend fun cancelAbsenceRequest(
        requestId: Long,
        cancelledBy: String,
        cancelledByRole: String,
        cancellationReason: String? = null
    ) {
        val request = getRequestById(requestId) ?: return
        
        val updatedRequest = request.copy(
            status = RequestStatus.CANCELADA
        )
        
        updateRequest(updatedRequest)
        
        // Crear log de cancelación
        val log = AbsenceLog(
            absenceId = requestId,
            absenceType = request.absenceType,
            action = LogAction.CANCELAR,
            actionDescription = "Solicitud cancelada",
            performedBy = cancelledBy,
            performedByRole = cancelledByRole,
            additionalInfo = cancellationReason
        )
        
        insertLog(log)
    }
    
    // ===== UTILIDADES =====
    
    private fun calculateTotalDays(startDate: Date, endDate: Date): Int {
        val diffInMillis = endDate.time - startDate.time
        val diffInDays = diffInMillis / (24 * 60 * 60 * 1000)
        return (diffInDays + 1).toInt() // +1 porque incluimos el día de inicio
    }
    
    suspend fun getAbsenceStatistics(employeeId: String? = null): Map<String, Int> {
        val stats = mutableMapOf<String, Int>()
        
        if (employeeId != null) {
            // Estadísticas por empleado
            stats["PENDIENTES"] = absenceRequestDao.getEmployeeRequestCount(employeeId, RequestStatus.PENDIENTE)
            stats["APROBADAS"] = absenceRequestDao.getEmployeeRequestCount(employeeId, RequestStatus.APROBADA)
            stats["RECHAZADAS"] = absenceRequestDao.getEmployeeRequestCount(employeeId, RequestStatus.RECHAZADA)
        } else {
            // Estadísticas generales
            stats["PENDIENTES"] = absenceRequestDao.getRequestCountByStatus(RequestStatus.PENDIENTE)
            stats["APROBADAS"] = absenceRequestDao.getRequestCountByStatus(RequestStatus.APROBADA)
            stats["RECHAZADAS"] = absenceRequestDao.getRequestCountByStatus(RequestStatus.RECHAZADA)
        }
        
        return stats
    }
}
