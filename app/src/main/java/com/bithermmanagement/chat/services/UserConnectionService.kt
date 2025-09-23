package com.bithermmanagement.chat.services

import android.content.Context
import android.util.Log
import com.bithermmanagement.chat.data.ChatDatabase
import com.bithermmanagement.chat.data.entities.UserConnectionEntity
import kotlinx.coroutines.*
import java.util.*

class UserConnectionService(private val context: Context) {
    
    private val database = ChatDatabase.getDatabase(context)
    private val userConnectionDao = database.userConnectionDao()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    companion object {
        private const val TAG = "UserConnectionService"
        private const val INACTIVITY_TIMEOUT = 20 * 60 * 1000L // 20 minutos
    }
    
    /**
     * Marcar usuario como online
     */
    suspend fun markUserAsOnline(username: String) {
        try {
            val now = Date()
            val connection = UserConnectionEntity(
                username = username,
                isOnline = true,
                lastConnectionTime = now,
                lastActivityTime = now,
                deviceInfo = getDeviceInfo(),
                appVersion = getAppVersion()
            )
            
            userConnectionDao.insertOrUpdateConnection(connection)
            // userConnectionDao.markUserAsOnline(username, now)
            
            Log.d(TAG, "Usuario marcado como online: $username")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error marcando usuario como online: $username", e)
        }
    }
    
    /**
     * Marcar usuario como offline
     */
    suspend fun markUserAsOffline(username: String) {
        try {
            val now = Date()
            val connection = userConnectionDao.getConnectionByUsername(username)
            
            if (connection != null && connection.isOnline) {
                val duration = now.time - (connection.lastConnectionTime?.time ?: now.time)
                // userConnectionDao.markUserAsOffline(username, now, duration)
                Log.d(TAG, "Usuario marcado como offline: $username (duración: ${duration}ms)")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error marcando usuario como offline: $username", e)
        }
    }
    
    /**
     * Actualizar última actividad del usuario
     */
    suspend fun updateUserActivity(username: String) {
        try {
            val now = Date()
            // userConnectionDao.updateLastActivity(username, now)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando actividad del usuario: $username", e)
        }
    }
    
    /**
     * Obtener usuarios online
     */
    suspend fun getOnlineUsers(): List<UserConnectionEntity> {
        return try {
            // userConnectionDao.getOnlineUsersSync()
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo usuarios online", e)
            emptyList()
        }
    }
    
    /**
     * Verificar si un usuario está online
     */
    suspend fun isUserOnline(username: String): Boolean {
        return try {
            val connection = userConnectionDao.getConnectionByUsername(username)
            connection?.isOnline == true && isUserActive(connection)
        } catch (e: Exception) {
            Log.e(TAG, "Error verificando si usuario está online: $username", e)
            false
        }
    }
    
    /**
     * Obtener última conexión de un usuario
     */
    suspend fun getLastConnectionTime(username: String): Date? {
        return try {
            val connection = userConnectionDao.getConnectionByUsername(username)
            connection?.lastConnectionTime
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo última conexión: $username", e)
            null
        }
    }
    
    /**
     * Obtener tiempo desde la última actividad
     */
    suspend fun getTimeSinceLastActivity(username: String): Long {
        return try {
            val connection = userConnectionDao.getConnectionByUsername(username)
            if (connection?.lastActivityTime != null) {
                System.currentTimeMillis() - connection.lastActivityTime.time
            } else {
                Long.MAX_VALUE
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo tiempo desde última actividad: $username", e)
            Long.MAX_VALUE
        }
    }
    
    /**
     * Limpiar usuarios inactivos
     */
    suspend fun cleanupInactiveUsers() {
        try {
            val cutoffTime = Date(System.currentTimeMillis() - INACTIVITY_TIMEOUT)
            // val inactiveUsers = userConnectionDao.getInactiveUsers(cutoffTime)
            
            // for (user in inactiveUsers) {
            //     markUserAsOffline(user.username)
            // }
            
            // if (inactiveUsers.isNotEmpty()) {
            //     Log.d(TAG, "Limpiados ${inactiveUsers.size} usuarios inactivos")
            // }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error limpiando usuarios inactivos", e)
        }
    }
    
    /**
     * Verificar si un usuario está activo (no ha estado inactivo por mucho tiempo)
     */
    private fun isUserActive(connection: UserConnectionEntity): Boolean {
        val lastActivity = connection.lastActivityTime ?: return false
        val now = System.currentTimeMillis()
        return (now - lastActivity.time) < INACTIVITY_TIMEOUT
    }
    
    /**
     * Obtener información del dispositivo
     */
    private fun getDeviceInfo(): String {
        return try {
            "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
        } catch (e: Exception) {
            "Unknown Device"
        }
    }
    
    /**
     * Obtener versión de la app
     */
    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    /**
     * Iniciar monitoreo de actividad
     */
    fun startActivityMonitoring(username: String) {
        scope.launch {
            while (isActive) {
                updateUserActivity(username)
                delay(30000) // Actualizar cada 30 segundos
            }
        }
    }
    
    /**
     * Detener monitoreo
     */
    fun stopActivityMonitoring() {
        scope.cancel()
    }
}
