package com.bithermmanagement.chat.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import com.bithermmanagement.chat.data.enums.MessageType
import java.util.Date

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "message_id")
    val messageId: String, // ID único del mensaje
    
    @ColumnInfo(name = "sender_username")
    val senderUsername: String, // Username del remitente
    
    @ColumnInfo(name = "chat_id")
    val chatId: String, // ID del chat (grupo o usuario individual)
    
    @ColumnInfo(name = "chat_type")
    val chatType: String, // "group" o "individual"
    
    @ColumnInfo(name = "message_type")
    val messageType: MessageType, // TEXT, IMAGE, DRAWING, FILE, etc.
    
    @ColumnInfo(name = "content")
    val content: String, // Contenido del mensaje (texto o ruta del archivo)
    
    @ColumnInfo(name = "file_path")
    val filePath: String? = null, // Ruta local del archivo (imagen, dibujo, etc.)
    
    @ColumnInfo(name = "file_size")
    val fileSize: Long? = null, // Tamaño del archivo en bytes
    
    @ColumnInfo(name = "file_mime_type")
    val fileMimeType: String? = null, // Tipo MIME del archivo
    
    @ColumnInfo(name = "thumbnail_path")
    val thumbnailPath: String? = null, // Ruta de la miniatura para imágenes
    
    @ColumnInfo(name = "timestamp")
    val timestamp: Date, // Fecha y hora del mensaje
    
    @ColumnInfo(name = "message_status")
    val messageStatus: String = "SENDING", // Estado del mensaje (SENDING, SENT, DELIVERED, READ, FAILED, PENDING)
    
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false, // Si el mensaje ha sido eliminado por admin
    
    @ColumnInfo(name = "deleted_by")
    val deletedBy: String? = null, // Username del admin que lo eliminó
    
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Date? = null, // Fecha de eliminación
    
    @ColumnInfo(name = "reply_to_message_id")
    val replyToMessageId: String? = null, // ID del mensaje al que responde
    
    @ColumnInfo(name = "edited")
    val edited: Boolean = false, // Si el mensaje ha sido editado
    
    @ColumnInfo(name = "edited_at")
    val editedAt: Date? = null, // Fecha de edición
    
    @ColumnInfo(name = "created_at")
    val createdAt: Date = Date() // Fecha de creación en la BD
)
