package com.bithermmanagement.chat.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bithermmanagement.R
import com.bithermmanagement.chat.data.entities.ChatMessageEntity
import java.text.SimpleDateFormat
import java.util.*

class ChatMessagesAdapter : RecyclerView.Adapter<ChatMessagesAdapter.MessageViewHolder>() {
    
    private var messages: List<ChatMessageEntity> = emptyList()
    private var currentUsername: String = ""
    private var isGroupChat: Boolean = false
    
    fun updateMessages(newMessages: List<ChatMessageEntity>, username: String, isGroup: Boolean) {
        messages = newMessages
        currentUsername = username
        isGroupChat = isGroup
        notifyDataSetChanged()
    }
    
    fun addMessage(message: ChatMessageEntity, username: String) {
        if (!messages.any { it.messageId == message.messageId }) {
            messages = messages + message
            currentUsername = username
            reagruparMensajes()
            notifyDataSetChanged()
        }
    }
    
    private fun reagruparMensajes() {
        messages = messages.sortedBy { it.timestamp }
    }
    
    private fun shouldShowTime(currentIndex: Int): Boolean {
        if (currentIndex >= messages.size - 1) return true
        
        val currentMessage = messages[currentIndex]
        val nextMessage = messages[currentIndex + 1]
        
        if (currentMessage.senderUsername == nextMessage.senderUsername) {
            val timeDiff = nextMessage.timestamp.time - currentMessage.timestamp.time
            val twoMinutesInMillis = 2 * 60 * 1000L
            if (timeDiff < twoMinutesInMillis) {
                return false
            }
        }
        
        return true
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_chat_message, parent, false)
        return MessageViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messages[position], position)
    }
    
    override fun getItemCount(): Int = messages.size
    
    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageContainer: LinearLayout = itemView.findViewById(R.id.messageContainer)
        private val messageText: TextView = itemView.findViewById(R.id.messageText)
        private val senderName: TextView = itemView.findViewById(R.id.senderName)
        private val messageTime: TextView = itemView.findViewById(R.id.messageTime)
        private val messageStatus: ImageView = itemView.findViewById(R.id.messageStatus)
        private val avatarImage: ImageView = itemView.findViewById(R.id.avatarImage)
        
        fun bind(message: ChatMessageEntity, position: Int) {
            messageText.text = message.content
            
            val isOwnMessage = message.senderUsername == currentUsername
            
            if (isOwnMessage) {
                // Mensaje enviado (derecha)
                messageContainer.gravity = android.view.Gravity.END
                messageText.setBackgroundResource(R.drawable.bg_message_sent)
                messageText.setTextColor(itemView.context.getColor(android.R.color.white))
                senderName.visibility = View.GONE
                avatarImage.visibility = View.GONE
                
                // Mostrar hora y estado solo cuando sea necesario
                if (shouldShowTime(position)) {
                    messageTime.text = formatTime(message.timestamp)
                    messageTime.visibility = View.VISIBLE
                    messageStatus.visibility = View.VISIBLE
                    
                    // Configurar estado del mensaje
                    val statusRes = when (message.messageStatus) {
                        "SENDING" -> R.drawable.ic_clock
                        "SENT" -> R.drawable.ic_check
                        "DELIVERED" -> R.drawable.ic_check_double
                        "READ" -> R.drawable.ic_check_double_blue
                        "FAILED" -> R.drawable.ic_error
                        "PENDING" -> R.drawable.ic_clock
                        else -> R.drawable.ic_check
                    }
                    
                    val statusColor = when (message.messageStatus) {
                        "READ" -> itemView.context.getColor(android.R.color.holo_blue_bright)
                        else -> itemView.context.getColor(android.R.color.darker_gray)
                    }
                    
                    messageStatus.setImageResource(statusRes)
                    messageStatus.setColorFilter(statusColor)
                } else {
                    messageTime.visibility = View.GONE
                    messageStatus.visibility = View.GONE
                }
                
            } else {
                // Mensaje recibido (izquierda)
                messageContainer.gravity = android.view.Gravity.START
                messageText.setBackgroundResource(R.drawable.bg_message_received)
                messageText.setTextColor(itemView.context.getColor(android.R.color.black))
                avatarImage.visibility = View.VISIBLE
                
                // Mostrar nombre del remitente solo en chats grupales
                if (isGroupChat) {
                    senderName.visibility = View.VISIBLE
                    senderName.text = message.senderUsername
                } else {
                    senderName.visibility = View.GONE
                }
                
                // Mostrar hora solo cuando sea necesario
                if (shouldShowTime(position)) {
                    messageTime.text = formatTime(message.timestamp)
                    messageTime.visibility = View.VISIBLE
                } else {
                    messageTime.visibility = View.GONE
                }
                
                // No mostrar estado para mensajes recibidos
                messageStatus.visibility = View.GONE
            }
        }
        
        private fun formatTime(timestamp: Date): String {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            return sdf.format(timestamp)
        }
    }
}
