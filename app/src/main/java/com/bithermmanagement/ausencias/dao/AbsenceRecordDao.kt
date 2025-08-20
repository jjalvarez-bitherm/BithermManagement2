package com.bithermmanagement.ausencias.dao

import androidx.room.*
import com.bithermmanagement.ausencias.models.AbsenceRecord
import com.bithermmanagement.ausencias.models.AbsenceStatus
import com.bithermmanagement.ausencias.models.AbsenceType
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface AbsenceRecordDao {
    
    // Consultas básicas
    @Query("SELECT * FROM absence_records ORDER BY startDate DESC")
    fun getAllAbsences(): Flow<List<AbsenceRecord>>
    
    @Query("SELECT * FROM absence_records WHERE id = :id")
    suspend fun getAbsenceById(id: Long): AbsenceRecord?
    
    @Query("SELECT * FROM absence_records WHERE employeeId = :employeeId ORDER BY startDate DESC")
    fun getAbsencesByEmployee(employeeId: String): Flow<List<AbsenceRecord>>
    
    // Consultas por estado
    @Query("SELECT * FROM absence_records WHERE status = :status ORDER BY startDate DESC")
    fun getAbsencesByStatus(status: AbsenceStatus): Flow<List<AbsenceRecord>>
    
    @Query("SELECT * FROM absence_records WHERE status IN (:statuses) ORDER BY startDate DESC")
    fun getAbsencesByStatuses(statuses: List<AbsenceStatus>): Flow<List<AbsenceRecord>>
    
    // Consultas por tipo
    @Query("SELECT * FROM absence_records WHERE absenceType = :type ORDER BY startDate DESC")
    fun getAbsencesByType(type: AbsenceType): Flow<List<AbsenceRecord>>
    
    // Consultas por fechas
    @Query("SELECT * FROM absence_records WHERE startDate >= :startDate AND endDate <= :endDate ORDER BY startDate DESC")
    fun getAbsencesByDateRange(startDate: Date, endDate: Date): Flow<List<AbsenceRecord>>
    
    @Query("SELECT * FROM absence_records WHERE startDate >= :date ORDER BY startDate DESC")
    fun getAbsencesFromDate(date: Date): Flow<List<AbsenceRecord>>
    
    @Query("SELECT * FROM absence_records WHERE endDate <= :date ORDER BY startDate DESC")
    fun getAbsencesUntilDate(date: Date): Flow<List<AbsenceRecord>>
    
    // Consultas para cuadrante
    @Query("SELECT * FROM absence_records WHERE startDate <= :date AND endDate >= :date ORDER BY employeeName ASC")
    fun getAbsencesForDate(date: Date): Flow<List<AbsenceRecord>>
    
    @Query("SELECT * FROM absence_records WHERE startDate <= :endDate AND endDate >= :startDate ORDER BY startDate ASC")
    fun getAbsencesForDateRange(startDate: Date, endDate: Date): Flow<List<AbsenceRecord>>
    
    // Consulta por mes específico
    @Query("SELECT * FROM absence_records WHERE (startDate >= :startOfMonth AND startDate < :endOfMonth) OR (endDate >= :startOfMonth AND endDate < :endOfMonth) OR (startDate <= :startOfMonth AND endDate >= :endOfMonth) ORDER BY startDate ASC")
    suspend fun getAbsencesByMonth(startOfMonth: Date, endOfMonth: Date): List<AbsenceRecord>
    
    // Consultas de sincronización
    @Query("SELECT * FROM absence_records WHERE isSynced = 0")
    suspend fun getUnsyncedAbsences(): List<AbsenceRecord>
    
    @Query("SELECT * FROM absence_records WHERE googleSheetsId = :sheetsId")
    suspend fun getAbsenceBySheetsId(sheetsId: String): AbsenceRecord?
    
    // Estadísticas
    @Query("SELECT COUNT(*) FROM absence_records WHERE status = :status")
    suspend fun getAbsenceCountByStatus(status: AbsenceStatus): Int
    
    @Query("SELECT COUNT(*) FROM absence_records WHERE employeeId = :employeeId AND status = :status")
    suspend fun getEmployeeAbsenceCount(employeeId: String, status: AbsenceStatus): Int
    
    // Operaciones CRUD
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAbsence(absence: AbsenceRecord): Long
    
    @Update
    suspend fun updateAbsence(absence: AbsenceRecord)
    
    @Delete
    suspend fun deleteAbsence(absence: AbsenceRecord)
    
    // Operaciones en lote
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAbsences(absences: List<AbsenceRecord>)
    
    @Query("DELETE FROM absence_records WHERE id IN (:ids)")
    suspend fun deleteAbsencesByIds(ids: List<Long>)
    
    // Limpieza
    @Query("DELETE FROM absence_records")
    suspend fun deleteAllAbsences()
}
