package com.bithermmanagement.ausencias.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "absence_logs")
data class AbsenceLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Referencia a la ausencia
    val absenceId: Long,
    val absenceType: AbsenceType,
    
    // Acción realizada
    val action: LogAction,
    val actionDescription: String,
    
    // Usuario que realiza la acción
    val performedBy: String,
    val performedByRole: String,
    
    // Detalles de la acción
    val oldValue: String? = null,
    val newValue: String? = null,
    val additionalInfo: String? = null,
    
    // Metadatos
    val timestamp: Date = Date(),
    val googleSheetsId: String? = null,
    
    // Sincronización
    val isSynced: Boolean = false,
    val lastSyncAttempt: Date? = null
)

enum class LogAction(val displayName: String, val color: String) {
    CREAR("Crear", "#4CAF50"),
    MODIFICAR("Modificar", "#2196F3"),
    APROBAR("Aprobar", "#4CAF50"),
    RECHAZAR("Rechazar", "#F44336"),
    CANCELAR("Cancelar", "#9E9E9E"),
    SINCRONIZAR("Sincronizar", "#607D8B"),
    ELIMINAR("Eliminar", "#F44336")
}
