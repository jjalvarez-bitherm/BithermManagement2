package com.bithermmanagement.chat.services

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.bithermmanagement.chat.data.ChatDatabase
import com.bithermmanagement.chat.data.entities.ChatUserEntity
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class FichajeBasedOnlineManager(private val context: Context) {
    
    companion object {
        private const val TAG = "FichajeOnlineManager"
        private const val PREFS_NAME = "fichaje_online_prefs"
        private const val KEY_LAST_FICHAJE = "last_fichaje_"
        private const val KEY_FICHAJE_TYPE = "fichaje_type_"
        
        // Estados de fichaje
        const val FICHAJE_ENTRADA = "entrada"
        const val FICHAJE_SALIDA = "salida"
        
        // Tiempo máximo para considerar online después de fichaje de entrada
        private const val MAX_ONLINE_TIME = 12 * 60 * 60 * 1000L // 12 horas
    }
    
    private val database = ChatDatabase.getDatabase(context)
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val onlineUsers = ConcurrentHashMap<String, Long>() // username -> fichaje timestamp
    
    /**
     * Registra un fichaje de entrada
     * Se llama cuando el usuario hace fichaje de entrada exitoso
     */
    suspend fun registerFichajeEntrada(username: String) {
        val currentTime = System.currentTimeMillis()
        
        // Actualizar en memoria
        onlineUsers[username] = currentTime
        
        // Actualizar en SharedPreferences
        prefs.edit()
            .putLong(KEY_LAST_FICHAJE + username, currentTime)
            .putString(KEY_FICHAJE_TYPE + username, FICHAJE_ENTRADA)
            .apply()
        
        // Actualizar en base de datos
        database.chatUserDao().updateUserOnlineStatus(username, true, currentTime)
        
        Log.d(TAG, "Usuario $username fichó entrada - marcado como online")
    }
    
    /**
     * Registra un fichaje de salida
     * Se llama cuando el usuario hace fichaje de salida
     */
    suspend fun registerFichajeSalida(username: String) {
        // Remover de memoria
        onlineUsers.remove(username)
        
        // Actualizar en SharedPreferences
        prefs.edit()
            .putString(KEY_FICHAJE_TYPE + username, FICHAJE_SALIDA)
            .apply()
        
        // Actualizar en base de datos
        database.chatUserDao().updateUserOnlineStatus(username, false, System.currentTimeMillis())
        
        Log.d(TAG, "Usuario $username fichó salida - marcado como offline")
    }
    
    /**
     * Verifica si un usuario está online basado en su fichaje
     */
    suspend fun isUserOnline(username: String): Boolean {
        // Verificar en memoria primero
        val lastFichaje = onlineUsers[username]
        if (lastFichaje != null) {
            val timeSinceFichaje = System.currentTimeMillis() - lastFichaje
            return timeSinceFichaje < MAX_ONLINE_TIME
        }
        
        // Verificar en SharedPreferences
        val fichajeType = prefs.getString(KEY_FICHAJE_TYPE + username, FICHAJE_SALIDA)
        if (fichajeType == FICHAJE_ENTRADA) {
            val lastFichajeTime = prefs.getLong(KEY_LAST_FICHAJE + username, 0L)
            val timeSinceFichaje = System.currentTimeMillis() - lastFichajeTime
            
            if (timeSinceFichaje < MAX_ONLINE_TIME) {
                // Usuario sigue online, actualizar memoria
                onlineUsers[username] = lastFichajeTime
                return true
            } else {
                // Tiempo expirado, marcar como offline
                database.chatUserDao().updateUserOnlineStatus(username, false, lastFichajeTime)
                prefs.edit().putString(KEY_FICHAJE_TYPE + username, FICHAJE_SALIDA).apply()
                return false
            }
        }
        
        return false
    }
    
    /**
     * Obtiene la lista de usuarios online
     */
    suspend fun getOnlineUsers(): List<ChatUserEntity> {
        val allUsers = database.chatUserDao().getAllUsersList()
        val onlineUsersList = mutableListOf<ChatUserEntity>()
        
        for (user in allUsers) {
            if (isUserOnline(user.username)) {
                onlineUsersList.add(user)
            }
        }
        
        return onlineUsersList
    }
    
    /**
     * Limpia usuarios offline antiguos
     */
    suspend fun cleanupOfflineUsers() {
        val allUsers = database.chatUserDao().getAllUsersList()
        
        for (user in allUsers) {
            val fichajeType = prefs.getString(KEY_FICHAJE_TYPE + user.username, FICHAJE_SALIDA)
            if (fichajeType == FICHAJE_ENTRADA) {
                val lastFichajeTime = prefs.getLong(KEY_LAST_FICHAJE + user.username, 0L)
                val timeSinceFichaje = System.currentTimeMillis() - lastFichajeTime
                
                if (timeSinceFichaje >= MAX_ONLINE_TIME) {
                    // Marcar como offline
                    database.chatUserDao().updateUserOnlineStatus(user.username, false, lastFichajeTime)
                    prefs.edit().putString(KEY_FICHAJE_TYPE + user.username, FICHAJE_SALIDA).apply()
                    onlineUsers.remove(user.username)
                }
            }
        }
    }
    
    /**
     * Obtiene información del fichaje de un usuario
     */
    fun getFichajeInfo(username: String): FichajeInfo {
        val fichajeType = prefs.getString(KEY_FICHAJE_TYPE + username, FICHAJE_SALIDA)
        val lastFichajeTime = prefs.getLong(KEY_LAST_FICHAJE + username, 0L)
        
        return FichajeInfo(
            username = username,
            fichajeType = fichajeType ?: FICHAJE_SALIDA,
            lastFichajeTime = lastFichajeTime,
            isOnline = fichajeType == FICHAJE_ENTRADA && 
                      (System.currentTimeMillis() - lastFichajeTime) < MAX_ONLINE_TIME
        )
    }
    
    data class FichajeInfo(
        val username: String,
        val fichajeType: String,
        val lastFichajeTime: Long,
        val isOnline: Boolean
    )
}
