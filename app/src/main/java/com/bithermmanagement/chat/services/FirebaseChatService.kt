package com.bithermmanagement.chat.services

import android.content.Context
import android.util.Log
import com.bithermmanagement.chat.data.entities.ChatMessageEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import java.util.*

class FirebaseChatService(private val context: Context) {
    
    companion object {
        private const val TAG = "FirebaseChatService"
        private const val COLLECTION_MESSAGES = "chat_messages"
        private const val COLLECTION_USERS = "users"
    }
    
    private val db = FirebaseFirestore.getInstance()
    private val messaging = FirebaseMessaging.getInstance()
    private var messageListener: ListenerRegistration? = null
    
    /**
     * Envía mensaje a Firebase y sincroniza en tiempo real
     */
    suspend fun sendMessage(message: ChatMessageEntity): String {
        return try {
            // Guardar mensaje en Firestore
            val messageData = hashMapOf(
                "id" to message.messageId,
                "senderUsername" to message.senderUsername,
                "chatId" to message.chatId,
                "chatType" to message.chatType,
                "content" to message.content,
                "timestamp" to message.timestamp,
                "messageStatus" to "SENT"
            )
            
            db.collection(COLLECTION_MESSAGES)
                .document(message.messageId)
                .set(messageData)
                .await()
            
            // Enviar notificación push al destinatario
            sendPushNotification(message)
            
            Log.d(TAG, "Mensaje enviado a Firebase: ${message.messageId}")
            message.messageId
            
        } catch (e: Exception) {
            Log.e(TAG, "Error enviando mensaje a Firebase", e)
            throw e
        }
    }
    
    /**
     * Escucha mensajes en tiempo real para un chat
     */
    fun listenToChat(chatId: String, onMessageReceived: (ChatMessageEntity) -> Unit) {
        // Cancelar listener anterior si existe
        messageListener?.remove()
        
        messageListener = db.collection(COLLECTION_MESSAGES)
            .whereEqualTo("chatId", chatId)
            .orderBy("timestamp") // RESTAURADO - el índice ya está creado
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error escuchando mensajes", error)
                    return@addSnapshotListener
                }
                
                snapshot?.documentChanges?.forEach { change ->
                    when (change.type) {
                        com.google.firebase.firestore.DocumentChange.Type.ADDED -> {
                            val messageData = change.document.data
                            if (messageData != null) {
                                val message = ChatMessageEntity(
                                    messageId = messageData["id"] as String,
                                    senderUsername = messageData["senderUsername"] as String,
                                    chatId = messageData["chatId"] as String,
                                    chatType = messageData["chatType"] as String,
                                    messageType = com.bithermmanagement.chat.data.enums.MessageType.TEXT,
                                    content = messageData["content"] as String,
                                    timestamp = (messageData["timestamp"] as com.google.firebase.Timestamp).toDate(),
                                    messageStatus = messageData["messageStatus"] as String
                                )
                                onMessageReceived(message)
                            }
                        }
                        else -> { /* Ignorar modificaciones y eliminaciones por ahora */ }
                    }
                }
            }
    }
    
    /**
     * Registra usuario en Firebase para notificaciones
     */
    suspend fun registerUser(username: String) {
        try {
            // Obtener token FCM
            val token = messaging.token.await()
            
            // Guardar usuario en Firestore
            val userData = hashMapOf(
                "username" to username,
                "fcmToken" to token,
                "lastSeen" to Date(),
                "isOnline" to true
            )
            
            db.collection(COLLECTION_USERS)
                .document(username)
                .set(userData)
                .await()
            
            Log.d(TAG, "Usuario registrado en Firebase: $username")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error registrando usuario en Firebase", e)
        }
    }
    
    /**
     * Actualiza estado online del usuario
     */
    suspend fun updateUserStatus(username: String, isOnline: Boolean) {
        try {
            db.collection(COLLECTION_USERS)
                .document(username)
                .update(
                    mapOf(
                        "isOnline" to isOnline,
                        "lastSeen" to Date()
                    )
                )
                .await()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando estado del usuario", e)
        }
    }
    
    /**
     * Envía notificación push al destinatario
     */
    private suspend fun sendPushNotification(message: ChatMessageEntity) {
        try {
            // Obtener token FCM del destinatario
            val recipientDoc = db.collection(COLLECTION_USERS)
                .document(message.chatId)
                .get()
                .await()
            
            val fcmToken = recipientDoc.getString("fcmToken")
            if (fcmToken != null) {
                // Enviar notificación push
                sendPushNotificationToToken(fcmToken, message)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error enviando notificación push", e)
        }
    }
    
    /**
     * Envía notificación push a un token específico
     */
    private suspend fun sendPushNotificationToToken(token: String, message: ChatMessageEntity) {
        // TODO: Implementar envío real a Firebase Cloud Functions
        // Por ahora, solo log
        Log.d(TAG, "Enviando notificación push a token: $token")
    }
    
    /**
     * Detiene la escucha de mensajes
     */
    fun stopListening() {
        messageListener?.remove()
        messageListener = null
    }
    
    /**
     * Elimina todos los mensajes de un chat
     */
    suspend fun clearChat(chatId: String) {
        try {
            val query = db.collection(COLLECTION_MESSAGES)
                .whereEqualTo("chatId", chatId)
                .get()
                .await()
            
            val batch = db.batch()
            query.documents.forEach { document ->
                batch.delete(document.reference)
            }
            
            batch.commit().await()
            Log.d(TAG, "Chat $chatId vaciado en Firebase")
        } catch (e: Exception) {
            Log.e(TAG, "Error vaciando chat en Firebase", e)
            throw e
        }
    }
}
