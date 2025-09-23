package com.bithermmanagement.chat.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "user_connections")
data class UserConnectionEntity(
    @PrimaryKey
    val username: String,
    
    val isOnline: Boolean = false,
    
    val lastConnectionTime: Date? = null,
    
    val lastDisconnectionTime: Date? = null,
    
    val connectionDuration: Long = 0, // en milisegundos
    
    val totalConnectionTime: Long = 0, // tiempo total conectado en milisegundos
    
    val connectionCount: Int = 0, // número de veces que se ha conectado
    
    val lastActivityTime: Date? = null, // última actividad en la app
    
    val deviceInfo: String? = null, // información del dispositivo
    
    val appVersion: String? = null // versión de la app
)



