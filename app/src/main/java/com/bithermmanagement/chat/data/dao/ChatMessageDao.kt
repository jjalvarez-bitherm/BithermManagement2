package com.bithermmanagement.chat.data.dao

import androidx.room.*
import com.bithermmanagement.chat.data.entities.ChatMessageEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

data class UnreadCountResult(
    @ColumnInfo(name = "chat_id")
    val chatId: String,
    @ColumnInfo(name = "count")
    val count: Int
)

@Dao
interface ChatMessageDao {
    
    // Insertar mensaje
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)
    
    // Insertar múltiples mensajes
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessageEntity>)
    
    // Obtener mensajes de un chat
    @Query("SELECT * FROM chat_messages WHERE chat_id = :chatId AND is_deleted = 0 ORDER BY timestamp ASC")
    fun getMessagesForChat(chatId: String): Flow<List<ChatMessageEntity>>
    
    // Obtener mensajes de un chat con límite
    @Query("SELECT * FROM chat_messages WHERE chat_id = :chatId AND is_deleted = 0 ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessagesForChat(chatId: String, limit: Int): List<ChatMessageEntity>
    
    // Obtener mensaje por ID
    @Query("SELECT * FROM chat_messages WHERE message_id = :messageId")
    suspend fun getMessageById(messageId: String): ChatMessageEntity?
    
    // Actualizar estado del mensaje
    @Query("UPDATE chat_messages SET message_status = :status WHERE message_id = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: String)
    
    // Obtener mensajes de un chat (versión síncrona)
    @Query("SELECT * FROM chat_messages WHERE chat_id = :chatId AND is_deleted = 0 ORDER BY timestamp ASC")
    suspend fun getMessagesForChatSync(chatId: String): List<ChatMessageEntity>
    
    // Obtener mensajes pendientes
    @Query("SELECT * FROM chat_messages WHERE message_status = 'PENDING' AND is_deleted = 0 ORDER BY timestamp ASC")
    suspend fun getPendingMessages(): List<ChatMessageEntity>
    
    // Marcar mensajes como leídos para un chat
    @Query("UPDATE chat_messages SET message_status = 'READ' WHERE chat_id = :chatId AND sender_username != :currentUser")
    suspend fun markChatAsRead(chatId: String, currentUser: String)
    
    // Eliminar mensaje (soft delete)
    @Query("UPDATE chat_messages SET is_deleted = 1, deleted_by = :deletedBy, deleted_at = :deletedAt WHERE message_id = :messageId")
    suspend fun deleteMessage(messageId: String, deletedBy: String, deletedAt: Date)
    
    // Eliminar mensajes de un chat (soft delete)
    @Query("UPDATE chat_messages SET is_deleted = 1, deleted_by = :deletedBy, deleted_at = :deletedAt WHERE chat_id = :chatId")
    suspend fun deleteChatMessages(chatId: String, deletedBy: String, deletedAt: Date)
    
    // Editar mensaje
    @Query("UPDATE chat_messages SET content = :newContent, edited = 1, edited_at = :editedAt WHERE message_id = :messageId")
    suspend fun editMessage(messageId: String, newContent: String, editedAt: Date)
    
    // Obtener mensajes no leídos
    @Query("SELECT COUNT(*) FROM chat_messages WHERE chat_id = :chatId AND message_status != 'READ' AND sender_username != :currentUser AND is_deleted = 0")
    suspend fun getUnreadMessageCount(chatId: String, currentUser: String): Int
    
    // Obtener mensajes no leídos para todos los chats
    @Query("SELECT chat_id, COUNT(*) as count FROM chat_messages WHERE message_status != 'READ' AND sender_username != :currentUser AND is_deleted = 0 GROUP BY chat_id")
    suspend fun getUnreadMessageCountsForAllChats(currentUser: String): List<UnreadCountResult>
    
    // Buscar mensajes
    @Query("SELECT * FROM chat_messages WHERE content LIKE '%' || :query || '%' AND is_deleted = 0 ORDER BY timestamp DESC")
    suspend fun searchMessages(query: String): List<ChatMessageEntity>
    
    // Obtener estadísticas de mensajes
    @Query("SELECT COUNT(*) FROM chat_messages WHERE chat_id = :chatId AND is_deleted = 0")
    suspend fun getMessageCountForChat(chatId: String): Int
    
    // Obtener último mensaje de un chat
    @Query("SELECT * FROM chat_messages WHERE chat_id = :chatId AND is_deleted = 0 ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastMessageForChat(chatId: String): ChatMessageEntity?
    
    // Limpiar mensajes antiguos (más de X días)
    @Query("DELETE FROM chat_messages WHERE timestamp < :cutoffDate AND is_deleted = 1")
    suspend fun cleanupOldDeletedMessages(cutoffDate: Date)
    
    // Eliminar todos los mensajes de un chat (hard delete)
    @Query("DELETE FROM chat_messages WHERE chat_id = :chatId")
    suspend fun deleteMessagesByChatId(chatId: String)
    
    // Obtener mensajes por tipo
    @Query("SELECT * FROM chat_messages WHERE chat_id = :chatId AND message_type = :messageType AND is_deleted = 0 ORDER BY timestamp DESC")
    suspend fun getMessagesByType(chatId: String, messageType: String): List<ChatMessageEntity>
}
