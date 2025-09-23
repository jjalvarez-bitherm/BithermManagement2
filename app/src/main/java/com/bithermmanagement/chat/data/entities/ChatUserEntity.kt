package com.bithermmanagement.chat.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "chat_users")
data class ChatUserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "username")
    val username: String, // Username único del usuario
    
    @ColumnInfo(name = "work_team")
    val workTeam: String, // Equipo de trabajo (leader, inspection, repair, iot)
    
    @ColumnInfo(name = "visible_name")
    val visibleName: String, // Nombre visible en la app
    
    @ColumnInfo(name = "user_image")
    val userImage: String? = null, // URL o ruta de la imagen del usuario
    
    @ColumnInfo(name = "is_online")
    val isOnline: Boolean = false, // Si el usuario está online
    
    @ColumnInfo(name = "is_team_leader")
    val isTeamLeader: Boolean = false, // Si es líder de equipo (*)
    
    @ColumnInfo(name = "is_admin")
    val isAdmin: Boolean = false, // Si es administrador (leader)
    
    @ColumnInfo(name = "last_seen")
    val lastSeen: Long = System.currentTimeMillis(), // Última vez que se vio online
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis() // Timestamp de creación
)
