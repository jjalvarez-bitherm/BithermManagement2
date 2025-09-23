package com.bithermmanagement.chat.services

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.bithermmanagement.chat.data.ChatDatabase
import com.bithermmanagement.chat.data.entities.ChatUserEntity
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class OnlineStatusManager(private val context: Context) {
    
    companion object {
        private const val TAG = "OnlineStatusManager"
        private const val PREFS_NAME = "online_status_prefs"
        private const val KEY_LAST_ACTIVITY = "last_activity_"
        private const val KEY_IS_ONLINE = "is_online_"
        
        // Tiempo en milisegundos para considerar a un usuario offline
        private const val OFFLINE_THRESHOLD = 5 * 60 * 1000L // 5 minutos
        private const val HEARTBEAT_INTERVAL = 30 * 1000L // 30 segundos
    }
    
    private val database = ChatDatabase.getDatabase(context)
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val onlineUsers = ConcurrentHashMap<String, Long>() // username -> lastActivity
    private var heartbeatJob: Job? = null
    private var currentUsername: String? = null
    
    /**
     * Marca al usuario actual como online
     * Se llama cuando:
     * 1. El usuario hace login exitoso
     * 2. El usuario abre la app
     * 3. El usuario navega a BithermChat
     */
    suspend fun markUserAsOnline(username: String) {
        currentUsername = username
        val currentTime = System.currentTimeMillis()
        
        // Actualizar en memoria
        onlineUsers[username] = currentTime
        
        // Actualizar en SharedPreferences
        prefs.edit()
            .putLong(KEY_LAST_ACTIVITY + username, currentTime)
            .putBoolean(KEY_IS_ONLINE + username, true)
            .apply()
        
        // Actualizar en base de datos
        database.chatUserDao().updateUserOnlineStatus(username, true, currentTime)
        
        Log.d(TAG, "Usuario $username marcado como online")
        
        // Iniciar heartbeat si no está activo
        startHeartbeat()
    }
    
    /**
     * Marca al usuario actual como offline
     * Se llama cuando:
     * 1. El usuario hace logout
     * 2. El usuario cierra la app
     * 3. La app pasa a background por mucho tiempo
     */
    suspend fun markUserAsOffline(username: String) {
        currentUsername = null
        
        // Remover de memoria
        onlineUsers.remove(username)
        
        // Actualizar en SharedPreferences
        prefs.edit()
            .putBoolean(KEY_IS_ONLINE + username, false)
            .apply()
        
        // Actualizar en base de datos
        database.chatUserDao().updateUserOnlineStatus(username, false, System.currentTimeMillis())
        
        Log.d(TAG, "Usuario $username marcado como offline")
        
        // Detener heartbeat si no hay usuarios online
        if (onlineUsers.isEmpty()) {
            stopHeartbeat()
        }
    }
    
    /**
     * Actualiza la actividad del usuario actual
     * Se llama periódicamente para mantener el estado online
     */
    suspend fun updateUserActivity(username: String) {
        val currentTime = System.currentTimeMillis()
        onlineUsers[username] = currentTime
        
        // Actualizar en SharedPreferences
        prefs.edit()
            .putLong(KEY_LAST_ACTIVITY + username, currentTime)
            .apply()
        
        // Actualizar en base de datos cada cierto tiempo para no saturar
        if (currentTime % (HEARTBEAT_INTERVAL * 2) == 0L) {
            database.chatUserDao().updateUserOnlineStatus(username, true, currentTime)
        }
    }
    
    /**
     * Obtiene la lista de usuarios online
     */
    suspend fun getOnlineUsers(): List<ChatUserEntity> {
        return database.chatUserDao().getOnlineUsers()
    }
    
    /**
     * Verifica si un usuario específico está online
     */
    suspend fun isUserOnline(username: String): Boolean {
        // Primero verificar en memoria (más rápido)
        val lastActivity = onlineUsers[username]
        if (lastActivity != null) {
            val timeSinceActivity = System.currentTimeMillis() - lastActivity
            return timeSinceActivity < OFFLINE_THRESHOLD
        }
        
        // Si no está en memoria, verificar en base de datos
        val user = database.chatUserDao().getUserByUsername(username)
        return user?.isOnline == true
    }
    
    /**
     * Inicia el heartbeat para mantener el estado online
     */
    private fun startHeartbeat() {
        if (heartbeatJob?.isActive == true) return
        
        heartbeatJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive && currentUsername != null) {
                try {
                    currentUsername?.let { username ->
                        updateUserActivity(username)
                    }
                    delay(HEARTBEAT_INTERVAL)
                } catch (e: Exception) {
                    Log.e(TAG, "Error en heartbeat", e)
                }
            }
        }
    }
    
    /**
     * Detiene el heartbeat
     */
    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }
    
    /**
     * Limpia usuarios offline antiguos
     * Se ejecuta periódicamente para mantener la base de datos limpia
     */
    suspend fun cleanupOfflineUsers() {
        val currentTime = System.currentTimeMillis()
        val offlineThreshold = currentTime - OFFLINE_THRESHOLD
        
        // Limpiar usuarios en memoria
        onlineUsers.entries.removeIf { (_, lastActivity) ->
            lastActivity < offlineThreshold
        }
        
        // Marcar como offline en base de datos usuarios inactivos
        val allUsers = database.chatUserDao().getAllUsersList()
        for (user in allUsers) {
            if (user.isOnline && user.lastSeen < offlineThreshold) {
                database.chatUserDao().updateUserOnlineStatus(user.username, false, user.lastSeen)
            }
        }
    }
    
    /**
     * Se llama cuando la app pasa a background
     */
    fun onAppBackgrounded() {
        // No marcar como offline inmediatamente, solo reducir la frecuencia del heartbeat
        Log.d(TAG, "App en background, reduciendo frecuencia de heartbeat")
    }
    
    /**
     * Se llama cuando la app vuelve a foreground
     */
    suspend fun onAppForegrounded() {
        currentUsername?.let { username ->
            updateUserActivity(username)
            startHeartbeat()
        }
        Log.d(TAG, "App en foreground, reanudando heartbeat")
    }
}
