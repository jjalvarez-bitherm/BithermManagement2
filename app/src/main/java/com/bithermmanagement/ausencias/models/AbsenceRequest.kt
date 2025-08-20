package com.bithermmanagement.ausencias.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "absence_requests")
data class AbsenceRequest(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Datos del empleado que solicita
    val employeeId: String,
    val employeeName: String,
    val employeeEmail: String,
    val employeeRole: String,
    
    // Datos de la solicitud
    val startDate: Date,
    val endDate: Date,
    val totalDays: Int,
    val absenceType: AbsenceType,
    val description: String,
    
    // Estado de la solicitud
    val status: RequestStatus = RequestStatus.PENDIENTE,
    val priority: RequestPriority = RequestPriority.NORMAL,
    
    // Aprobación
    val approvedBy: String? = null,
    val approvedDate: Date? = null,
    val rejectionReason: String? = null,
    
    // Metadatos
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val googleSheetsId: String? = null,
    
    // Sincronización
    val isSynced: Boolean = false,
    val lastSyncAttempt: Date? = null
)

enum class RequestStatus(val displayName: String, val color: String) {
    PENDIENTE("Pendiente", "#FFC107"),
    EN_REVISION("En Revisión", "#2196F3"),
    APROBADA("Aprobada", "#4CAF50"),
    RECHAZADA("Rechazada", "#F44336"),
    CANCELADA("Cancelada", "#9E9E9E"),
    PASADA("Pasada", "#9C27B0")
}

enum class RequestPriority(val displayName: String, val color: String) {
    BAJA("Baja", "#4CAF50"),
    NORMAL("Normal", "#2196F3"),
    ALTA("Alta", "#FF9800"),
    URGENTE("Urgente", "#F44336")
}
