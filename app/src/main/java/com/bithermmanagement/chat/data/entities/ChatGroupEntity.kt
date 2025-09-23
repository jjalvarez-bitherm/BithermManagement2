package com.bithermmanagement.chat.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "chat_groups")
data class ChatGroupEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "group_id")
    val groupId: String, // ID único del grupo
    
    @ColumnInfo(name = "name")
    val name: String, // Nombre interno del grupo (leader, inspection, etc.)
    
    @ColumnInfo(name = "display_name")
    val displayName: String, // Nombre para mostrar (TECN., INSPC., etc.)
    
    @ColumnInfo(name = "is_default")
    val isDefault: Boolean = false, // Si es el grupo por defecto
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis() // Timestamp de creación
)
