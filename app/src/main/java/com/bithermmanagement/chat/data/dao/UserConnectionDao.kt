package com.bithermmanagement.chat.data.dao

import androidx.room.*
import com.bithermmanagement.chat.data.entities.UserConnectionEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface UserConnectionDao {
    
    // Insertar o actualizar conexión de usuario
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateConnection(connection: UserConnectionEntity)
    
    // Obtener conexión de un usuario
    @Query("SELECT * FROM user_connections WHERE username = :username")
    suspend fun getConnectionByUsername(username: String): UserConnectionEntity?
    
    /*
    // Obtener todos los usuarios online
    @Query("SELECT * FROM user_connections WHERE is_online = 1")
    fun getOnlineUsers(): Flow<List<UserConnectionEntity>>
    
    // Obtener todos los usuarios online (versión síncrona)
    @Query("SELECT * FROM user_connections WHERE is_online = 1")
    suspend fun getOnlineUsersSync(): List<UserConnectionEntity>
    
    // Marcar usuario como online
    @Query("UPDATE user_connections SET is_online = 1, last_connection_time = :connectionTime, connection_count = connection_count + 1 WHERE username = :username")
    suspend fun markUserAsOnline(username: String, connectionTime: Date)
    
    // Marcar usuario como offline
    @Query("UPDATE user_connections SET is_online = 0, last_disconnection_time = :disconnectionTime, connection_duration = :duration, total_connection_time = total_connection_time + :duration WHERE username = :username")
    suspend fun markUserAsOffline(username: String, disconnectionTime: Date, duration: Long)
    
    // Actualizar última actividad
    @Query("UPDATE user_connections SET last_activity_time = :activityTime WHERE username = :username")
    suspend fun updateLastActivity(username: String, activityTime: Date)
    
    // Obtener usuarios que no han tenido actividad reciente (más de X minutos)
    @Query("SELECT * FROM user_connections WHERE last_activity_time < :cutoffTime AND is_online = 1")
    suspend fun getInactiveUsers(cutoffTime: Date): List<UserConnectionEntity>
    
    // Limpiar conexiones antiguas (más de X días)
    @Query("DELETE FROM user_connections WHERE last_activity_time < :cutoffDate")
    suspend fun cleanupOldConnections(cutoffDate: Date)
    
    // Obtener estadísticas de conexión
    @Query("SELECT COUNT(*) FROM user_connections WHERE is_online = 1")
    suspend fun getOnlineUserCount(): Int
    
    // Obtener tiempo total de conexión de un usuario
    @Query("SELECT total_connection_time FROM user_connections WHERE username = :username")
    suspend fun getTotalConnectionTime(username: String): Long?
    
    // Obtener usuarios con más tiempo de conexión
    @Query("SELECT * FROM user_connections ORDER BY total_connection_time DESC LIMIT :limit")
    suspend fun getTopConnectedUsers(limit: Int): List<UserConnectionEntity>
    */
}
