package com.bithermmanagement.chat.models

import java.util.Date

data class ChatMessage(
    val id: String,
    val sender: ChatUser,
    val content: String,
    val timestamp: Date,
    val groupName: String,
    val isRead: Boolean = false
)
