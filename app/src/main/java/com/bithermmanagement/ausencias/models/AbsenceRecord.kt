package com.bithermmanagement.ausencias.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "absence_records")
data class AbsenceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Datos del empleado
    val employeeId: String,
    val employeeName: String,
    val employeeEmail: String,
    
    // Datos de la ausencia
    val startDate: Date,
    val endDate: Date,
    val totalDays: Int,
    val absenceType: AbsenceType,
    val status: AbsenceStatus,
    
    // Detalles adicionales
    val description: String? = null,
    val approvedBy: String? = null,
    val approvedDate: Date? = null,
    val rejectionReason: String? = null,
    
    // Metadatos
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val googleSheetsId: String? = null, // ID en la hoja de Google Sheets
    
    // Campos para sincronización
    val isSynced: Boolean = false,
    val lastSyncAttempt: Date? = null
)

enum class AbsenceType(val displayName: String, val color: String) {
    VACACIONES("Vacaciones", "#4CAF50"),
    PERMISO("Permiso", "#2196F3"),
    ENFERMEDAD("Enfermedad", "#FF9800"),
    ASUNTOS_PERSONALES("Asuntos Personales", "#9C27B0"),
    FORMACION("Formación", "#607D8B"),
    OTROS("Otros", "#795548")
}

enum class AbsenceStatus(val displayName: String, val color: String) {
    PENDIENTE("Pendiente", "#FFC107"),
    APROBADA("Aprobada", "#4CAF50"),
    RECHAZADA("Rechazada", "#F44336"),
    CANCELADA("Cancelada", "#9E9E9E")
}
