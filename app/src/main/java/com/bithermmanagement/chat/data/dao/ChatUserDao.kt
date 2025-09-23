package com.bithermmanagement.chat.data.dao

import androidx.room.*
import com.bithermmanagement.chat.data.entities.ChatUserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatUserDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: ChatUserEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<ChatUserEntity>)
    
    @Query("SELECT * FROM chat_users ORDER BY visible_name ASC")
    fun getAllUsers(): Flow<List<ChatUserEntity>>
    
    @Query("SELECT * FROM chat_users ORDER BY visible_name ASC")
    suspend fun getAllUsersList(): List<ChatUserEntity>
    
    @Query("SELECT * FROM chat_users WHERE username = :username")
    suspend fun getUserByUsername(username: String): ChatUserEntity?
    
    @Query("SELECT * FROM chat_users WHERE work_team = :workTeam")
    suspend fun getUsersByWorkTeam(workTeam: String): List<ChatUserEntity>
    
    @Query("SELECT * FROM chat_users WHERE is_online = 1")
    suspend fun getOnlineUsers(): List<ChatUserEntity>
    
    @Query("UPDATE chat_users SET is_online = :isOnline, last_seen = :lastSeen WHERE username = :username")
    suspend fun updateUserOnlineStatus(username: String, isOnline: Boolean, lastSeen: Long = System.currentTimeMillis())
    
    @Update
    suspend fun updateUser(user: ChatUserEntity)
    
    @Delete
    suspend fun deleteUser(user: ChatUserEntity)
    
    @Query("DELETE FROM chat_users")
    suspend fun deleteAllUsers()
}
