package com.bithermmanagement.chat.models

data class ChatGroup(
    val name: String,
    val displayName: String,
    val members: List<ChatUser>,
    val isDefault: Boolean = false
)
