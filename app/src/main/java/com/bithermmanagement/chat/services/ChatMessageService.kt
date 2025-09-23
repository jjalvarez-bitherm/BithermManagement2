package com.bithermmanagement.chat.services

import android.content.Context
import android.util.Log
import com.bithermmanagement.chat.data.ChatDatabase
import com.bithermmanagement.chat.data.entities.ChatMessageEntity
import com.bithermmanagement.chat.data.enums.MessageStatus
import com.bithermmanagement.chat.data.enums.MessageType
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

class ChatMessageService(private val context: Context) {
    
    companion object {
        private const val TAG = "ChatMessageService"
        private const val SYNC_INTERVAL = 5 * 60 * 1000L // 5 minutos
        private const val DAILY_CLEANUP_HOUR = 2 // 2:00 AM
    }
    
    private val database = ChatDatabase.getDatabase(context)
    private val onlineManager = FichajeBasedOnlineManager(context)
    private val firebaseService = FirebaseChatService(context)
    private val messageQueue = ConcurrentLinkedQueue<ChatMessageEntity>()
    private var syncJob: Job? = null
    private var cleanupJob: Job? = null
    
    init {
        startMessageSync()
        startDailyCleanup()
    }
    
    /**
     * Envía un mensaje
     */
    suspend fun sendMessage(
        senderUsername: String,
        chatId: String,
        chatType: String,
        content: String,
        messageType: MessageType = MessageType.TEXT
    ): String {
        val messageId = generateMessageId()
        val currentTime = Date()
        
        val message = ChatMessageEntity(
            messageId = messageId,
            senderUsername = senderUsername,
            chatId = chatId,
            chatType = chatType,
            messageType = messageType,
            content = content,
            timestamp = currentTime,
            messageStatus = MessageStatus.SENDING.name
        )
        
        // Guardar en base de datos local
        database.chatMessageDao().insertMessage(message)
        
        // Enviar a Firebase para sincronización en tiempo real
        try {
            firebaseService.sendMessage(message)
            database.chatMessageDao().updateMessageStatus(messageId, MessageStatus.SENT.name)
        } catch (e: Exception) {
            Log.e(TAG, "Error enviando mensaje a Firebase", e)
            // Marcar como pendiente si falla Firebase
            database.chatMessageDao().updateMessageStatus(messageId, MessageStatus.PENDING.name)
            messageQueue.offer(message)
        }
        
        return messageId
    }
    
    /**
     * Envía mensaje a grupo
     */
    private suspend fun sendGroupMessage(message: ChatMessageEntity) {
        // Obtener miembros del grupo
        val groupMembers = getGroupMembers(message.chatId)
        
        for (member in groupMembers) {
            if (member.username != message.senderUsername) {
                val isOnline = onlineManager.isUserOnline(member.username)
                
                if (isOnline) {
                    // Crear copia del mensaje para cada miembro
                    val memberMessage = message.copy(
                        messageId = generateMessageId(),
                        chatId = member.username,
                        chatType = "individual"
                    )
                    
                    database.chatMessageDao().insertMessage(memberMessage)
                    
                    if (attemptDelivery(memberMessage.messageId, member.username)) {
                        database.chatMessageDao().updateMessageStatus(memberMessage.messageId, MessageStatus.DELIVERED.name)
                    }
                } else {
                    // Miembro offline, poner en cola
                    val pendingMessage = message.copy(
                        messageId = generateMessageId(),
                        chatId = member.username,
                        chatType = "individual",
                        messageStatus = MessageStatus.PENDING.name
                    )
                    messageQueue.offer(pendingMessage)
                }
            }
        }
        
        // Marcar mensaje original como enviado
        database.chatMessageDao().updateMessageStatus(message.messageId, MessageStatus.SENT.name)
    }
    
    /**
     * Intenta entregar un mensaje
     */
    private suspend fun attemptDelivery(messageId: String, recipientUsername: String): Boolean {
        return try {
            // Aquí implementarías la lógica real de entrega
            // Por ahora simulamos éxito
            delay(100) // Simular tiempo de entrega
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error entregando mensaje $messageId", e)
            false
        }
    }
    
    /**
     * Marca mensaje como leído
     */
    suspend fun markMessageAsRead(messageId: String) {
        database.chatMessageDao().updateMessageStatus(messageId, MessageStatus.READ.name)
    }
    
    /**
     * Obtiene mensajes de un chat
     */
    suspend fun getMessagesForChat(chatId: String): List<ChatMessageEntity> {
        return database.chatMessageDao().getMessagesForChatSync(chatId)
    }
    
    /**
     * Guarda un mensaje recibido localmente
     */
    suspend fun saveMessageLocally(message: ChatMessageEntity) {
        database.chatMessageDao().insertMessage(message)
    }
    
    /**
     * Elimina todos los mensajes de un chat de la base de datos local
     */
    suspend fun clearChat(chatId: String) {
        database.chatMessageDao().deleteMessagesByChatId(chatId)
    }
    
    /**
     * Inicia sincronización de mensajes
     */
    private fun startMessageSync() {
        syncJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    processPendingMessages()
                    delay(SYNC_INTERVAL)
                } catch (e: Exception) {
                    Log.e(TAG, "Error en sincronización de mensajes", e)
                }
            }
        }
    }
    
    /**
     * Procesa mensajes pendientes
     */
    private suspend fun processPendingMessages() {
        val pendingMessages = mutableListOf<ChatMessageEntity>()
        
        // Obtener mensajes de la cola
        while (messageQueue.isNotEmpty()) {
            pendingMessages.add(messageQueue.poll())
        }
        
        // Obtener mensajes pendientes de la base de datos
        val dbPendingMessages = database.chatMessageDao().getPendingMessages()
        pendingMessages.addAll(dbPendingMessages)
        
        // Intentar enviar mensajes pendientes
        for (message in pendingMessages) {
            val recipientUsername = message.chatId
            val isOnline = onlineManager.isUserOnline(recipientUsername)
            
            if (isOnline) {
                // Usuario ahora está online, intentar entrega
                if (attemptDelivery(message.messageId, recipientUsername)) {
                    database.chatMessageDao().updateMessageStatus(message.messageId, MessageStatus.DELIVERED.name)
                } else {
                    // Volver a poner en cola
                    messageQueue.offer(message)
                }
            } else {
                // Usuario sigue offline, volver a poner en cola
                messageQueue.offer(message)
            }
        }
    }
    
    /**
     * Inicia limpieza diaria
     */
    private fun startDailyCleanup() {
        cleanupJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    val now = Calendar.getInstance()
                    val targetHour = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, DAILY_CLEANUP_HOUR)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                    }
                    
                    if (now.after(targetHour)) {
                        targetHour.add(Calendar.DAY_OF_YEAR, 1)
                    }
                    
                    val delay = targetHour.timeInMillis - now.timeInMillis
                    delay(delay)
                    
                    performDailyCleanup()
                } catch (e: Exception) {
                    Log.e(TAG, "Error en limpieza diaria", e)
                }
            }
        }
    }
    
    /**
     * Realiza limpieza diaria
     */
    private suspend fun performDailyCleanup() {
        try {
            // Limpiar mensajes eliminados antiguos (más de 30 días)
            val thirtyDaysAgo = Date(System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L))
            database.chatMessageDao().cleanupOldDeletedMessages(thirtyDaysAgo)
            
            // Limpiar archivos antiguos
            val fileManager = ChatFileManager(context)
            fileManager.cleanupOldFiles(30)
            
            Log.d(TAG, "Limpieza diaria completada")
        } catch (e: Exception) {
            Log.e(TAG, "Error en limpieza diaria", e)
        }
    }
    
    /**
     * Genera ID único para mensaje
     */
    private fun generateMessageId(): String {
        return "msg_${System.currentTimeMillis()}_${UUID.randomUUID().toString().substring(0, 8)}"
    }
    
    /**
     * Obtiene miembros de un grupo
     */
    private suspend fun getGroupMembers(groupName: String): List<com.bithermmanagement.chat.models.ChatUser> {
        // Implementar lógica para obtener miembros del grupo
        return emptyList() // Placeholder
    }
    
    /**
     * Detiene el servicio
     */
    fun stop() {
        syncJob?.cancel()
        cleanupJob?.cancel()
    }
}
