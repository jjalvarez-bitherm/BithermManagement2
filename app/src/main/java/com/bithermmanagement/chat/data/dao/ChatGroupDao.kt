package com.bithermmanagement.chat.data.dao

import androidx.room.*
import com.bithermmanagement.chat.data.entities.ChatGroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatGroupDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: ChatGroupEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroups(groups: List<ChatGroupEntity>)
    
    @Query("SELECT * FROM chat_groups ORDER BY display_name ASC")
    fun getAllGroups(): Flow<List<ChatGroupEntity>>
    
    @Query("SELECT * FROM chat_groups WHERE group_id = :groupId")
    suspend fun getGroupById(groupId: String): ChatGroupEntity?
    
    @Query("SELECT * FROM chat_groups WHERE name = :name")
    suspend fun getGroupByName(name: String): ChatGroupEntity?
    
    @Update
    suspend fun updateGroup(group: ChatGroupEntity)
    
    @Delete
    suspend fun deleteGroup(group: ChatGroupEntity)
    
    @Query("DELETE FROM chat_groups")
    suspend fun deleteAllGroups()
}
