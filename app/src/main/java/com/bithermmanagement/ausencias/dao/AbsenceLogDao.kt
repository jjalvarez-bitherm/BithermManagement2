package com.bithermmanagement.ausencias.dao

import androidx.room.*
import com.bithermmanagement.ausencias.models.AbsenceLog
import com.bithermmanagement.ausencias.models.LogAction
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface AbsenceLogDao {
    
    // Consultas básicas
    @Query("SELECT * FROM absence_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<AbsenceLog>>
    
    @Query("SELECT * FROM absence_logs WHERE id = :id")
    suspend fun getLogById(id: Long): AbsenceLog?
    
    @Query("SELECT * FROM absence_logs WHERE absenceId = :absenceId ORDER BY timestamp DESC")
    fun getLogsByAbsenceId(absenceId: Long): Flow<List<AbsenceLog>>
    
    // Consultas por acción
    @Query("SELECT * FROM absence_logs WHERE action = :action ORDER BY timestamp DESC")
    fun getLogsByAction(action: LogAction): Flow<List<AbsenceLog>>
    
    @Query("SELECT * FROM absence_logs WHERE action IN (:actions) ORDER BY timestamp DESC")
    fun getLogsByActions(actions: List<LogAction>): Flow<List<AbsenceLog>>
    
    // Consultas por usuario
    @Query("SELECT * FROM absence_logs WHERE performedBy = :userId ORDER BY timestamp DESC")
    fun getLogsByUser(userId: String): Flow<List<AbsenceLog>>
    
    @Query("SELECT * FROM absence_logs WHERE performedByRole = :role ORDER BY timestamp DESC")
    fun getLogsByRole(role: String): Flow<List<AbsenceLog>
    
    // Consultas por fechas
    @Query("SELECT * FROM absence_logs WHERE timestamp >= :startDate AND timestamp <= :endDate ORDER BY timestamp DESC")
    fun getLogsByDateRange(startDate: Date, endDate: Date): Flow<List<AbsenceLog>>
    
    @Query("SELECT * FROM absence_logs WHERE timestamp >= :date ORDER BY timestamp DESC")
    fun getLogsFromDate(date: Date): Flow<List<AbsenceLog>>
    
    // Consultas para auditoría
    @Query("SELECT * FROM absence_logs WHERE absenceId = :absenceId AND action IN ('MODIFICAR', 'APROBAR', 'RECHAZAR') ORDER BY timestamp DESC")
    fun getAuditLogsForAbsence(absenceId: Long): Flow<List<AbsenceLog>>
    
    @Query("SELECT * FROM absence_logs WHERE performedBy = :userId AND action IN ('APROBAR', 'RECHAZAR') ORDER BY timestamp DESC")
    fun getApprovalLogsByUser(userId: String): Flow<List<AbsenceLog>>
    
    // Consultas de sincronización
    @Query("SELECT * FROM absence_logs WHERE isSynced = 0")
    suspend fun getUnsyncedLogs(): List<AbsenceLog>
    
    @Query("SELECT * FROM absence_logs WHERE googleSheetsId = :sheetsId")
    suspend fun getLogBySheetsId(sheetsId: String): AbsenceLog?
    
    // Estadísticas
    @Query("SELECT COUNT(*) FROM absence_logs WHERE action = :action")
    suspend fun getLogCountByAction(action: LogAction): Int
    
    @Query("SELECT COUNT(*) FROM absence_logs WHERE performedBy = :userId")
    suspend fun getUserLogCount(userId: String): Int
    
    // Operaciones CRUD
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AbsenceLog): Long
    
    @Update
    suspend fun updateLog(log: AbsenceLog)
    
    @Delete
    suspend fun deleteLog(log: AbsenceLog)
    
    // Operaciones en lote
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<AbsenceLog>)
    
    @Query("DELETE FROM absence_logs WHERE id IN (:ids)")
    suspend fun deleteLogsByIds(ids: List<Long>)
    
    // Limpieza
    @Query("DELETE FROM absence_logs")
    suspend fun deleteAllLogs()
    
    @Query("DELETE FROM absence_logs WHERE timestamp < :date")
    suspend fun deleteLogsOlderThan(date: Date)
}
