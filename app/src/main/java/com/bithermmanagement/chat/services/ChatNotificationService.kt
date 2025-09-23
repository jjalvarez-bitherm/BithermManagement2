package com.bithermmanagement.chat.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.bithermmanagement.R
import com.bithermmanagement.chat.data.entities.ChatMessageEntity
import com.bithermmanagement.ui.MainMenuActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class ChatNotificationService : FirebaseMessagingService() {
    
    companion object {
        private const val CHANNEL_ID = "chat_notifications"
        private const val CHANNEL_NAME = "Chat Notifications"
        private const val CHANNEL_DESCRIPTION = "Notificaciones de chat en tiempo real"
        
        // Función para crear el canal de notificaciones
        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CHANNEL_DESCRIPTION
                    enableVibration(true)
                    enableLights(true)
                }
                
                val notificationManager = context.getSystemService(NotificationManager::class.java)
                notificationManager.createNotificationChannel(channel)
            }
        }
        
        // Función para mostrar notificación local
        fun showChatNotification(
            context: Context,
            senderName: String,
            messageContent: String,
            chatId: String
        ) {
            val intent = Intent(context, MainMenuActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("open_chat", chatId)
            }
            
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_chat_notification)
                .setContentTitle("Nuevo mensaje de $senderName")
                .setContentText(messageContent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()
            
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        }
    }
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Guardar el token para enviarlo al servidor
        saveTokenToPreferences(token)
    }
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        // Procesar mensaje recibido
        val data = remoteMessage.data
        val senderUsername = data["sender_username"] ?: return
        val chatId = data["chat_id"] ?: return
        val messageContent = data["message_content"] ?: return
        val messageId = data["message_id"] ?: return
        
        // Guardar mensaje en base de datos local
        saveMessageLocally(senderUsername, chatId, messageContent, messageId)
        
        // Mostrar notificación
        showChatNotification(
            this,
            senderUsername,
            messageContent,
            chatId
        )
        
        // Enviar broadcast para actualizar la UI
        sendMessageReceivedBroadcast(chatId)
    }
    
    private fun saveTokenToPreferences(token: String) {
        val prefs = getSharedPreferences("bitherm_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("fcm_token", token).apply()
    }
    
    private fun saveMessageLocally(
        senderUsername: String,
        chatId: String,
        messageContent: String,
        messageId: String
    ) {
        try {
            val chatMessageService = ChatMessageService(this)
                    val message = ChatMessageEntity(
            messageId = messageId,
            senderUsername = senderUsername,
            chatId = chatId,
            chatType = "individual", // Por defecto, se puede mejorar
            messageType = com.bithermmanagement.chat.data.enums.MessageType.TEXT,
            content = messageContent,
            timestamp = Date(),
            messageStatus = "RECEIVED"
        )
            
            // Guardar en base de datos local
            CoroutineScope(Dispatchers.IO).launch {
                chatMessageService.saveMessageLocally(message)
            }
            
        } catch (e: Exception) {
            // Log del error
            android.util.Log.e("ChatNotificationService", "Error guardando mensaje localmente", e)
        }
    }
    
    private fun sendMessageReceivedBroadcast(chatId: String) {
        val intent = Intent("CHAT_MESSAGE_RECEIVED")
        intent.putExtra("chat_id", chatId)
        sendBroadcast(intent)
    }
}
