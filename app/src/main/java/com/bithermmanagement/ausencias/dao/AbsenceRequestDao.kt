package com.bithermmanagement.ausencias.dao

import androidx.room.*
import com.bithermmanagement.ausencias.models.AbsenceRequest
import com.bithermmanagement.ausencias.models.RequestStatus
import com.bithermmanagement.ausencias.models.RequestPriority
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface AbsenceRequestDao {
    
    // Consultas básicas
    @Query("SELECT * FROM absence_requests ORDER BY createdAt DESC")
    fun getAllRequests(): Flow<List<AbsenceRequest>>
    
    @Query("SELECT * FROM absence_requests WHERE id = :id")
    suspend fun getRequestById(id: Long): AbsenceRequest?
    
    @Query("SELECT * FROM absence_requests WHERE employeeId = :employeeId ORDER BY createdAt DESC")
    fun getRequestsByEmployee(employeeId: String): Flow<List<AbsenceRequest>>
    
    // Consultas por estado
    @Query("SELECT * FROM absence_requests WHERE status = :status ORDER BY createdAt DESC")
    fun getRequestsByStatus(status: RequestStatus): Flow<List<AbsenceRequest>>
    
    @Query("SELECT * FROM absence_requests WHERE status IN (:statuses) ORDER BY createdAt DESC")
    fun getRequestsByStatuses(statuses: List<RequestStatus>): Flow<List<AbsenceRequest>>
    
    // Consultas por prioridad
    @Query("SELECT * FROM absence_requests WHERE priority = :priority ORDER BY createdAt DESC")
    fun getRequestsByPriority(priority: RequestPriority): Flow<List<AbsenceRequest>>
    
    @Query("SELECT * FROM absence_requests WHERE priority IN (:priorities) ORDER BY createdAt DESC")
    fun getRequestsByPriorities(priorities: List<RequestPriority>): Flow<List<AbsenceRequest>>
    
    // Consultas por fechas
    @Query("SELECT * FROM absence_requests WHERE startDate >= :startDate AND endDate <= :endDate ORDER BY createdAt DESC")
    fun getRequestsByDateRange(startDate: Date, endDate: Date): Flow<List<AbsenceRequest>>
    
    @Query("SELECT * FROM absence_requests WHERE startDate >= :date ORDER BY createdAt DESC")
    fun getRequestsFromDate(date: Date): Flow<List<AbsenceRequest>>
    
    // Consultas para administradores
    @Query("SELECT * FROM absence_requests WHERE status IN ('PENDIENTE', 'EN_REVISION') ORDER BY priority DESC, createdAt ASC")
    fun getPendingRequests(): Flow<List<AbsenceRequest>>
    
    @Query("SELECT * FROM absence_requests WHERE status = 'PENDIENTE' ORDER BY priority DESC, createdAt ASC")
    fun getApprovalPendingRequests(): Flow<List<AbsenceRequest>>
    
    // Consultas de sincronización
    @Query("SELECT * FROM absence_requests WHERE isSynced = 0")
    suspend fun getUnsyncedRequests(): List<AbsenceRequest>
    
    @Query("SELECT * FROM absence_requests WHERE googleSheetsId = :sheetsId")
    suspend fun getRequestBySheetsId(sheetsId: String): AbsenceRequest?
    
    // Estadísticas
    @Query("SELECT COUNT(*) FROM absence_requests WHERE status = :status")
    suspend fun getRequestCountByStatus(status: RequestStatus): Int
    
    @Query("SELECT COUNT(*) FROM absence_requests WHERE employeeId = :employeeId AND status = :status")
    suspend fun getEmployeeRequestCount(employeeId: String, status: RequestStatus): Int
    
    // Operaciones CRUD
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequest(request: AbsenceRequest): Long
    
    @Update
    suspend fun updateRequest(request: AbsenceRequest)
    
    @Delete
    suspend fun deleteRequest(request: AbsenceRequest)
    
    // Operaciones en lote
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequests(requests: List<AbsenceRequest>)
    
    @Query("DELETE FROM absence_requests WHERE id IN (:ids)")
    suspend fun deleteRequestsByIds(ids: List<Long>)
    
    // Limpieza
    @Query("DELETE FROM absence_requests")
    suspend fun deleteAllRequests()
}
