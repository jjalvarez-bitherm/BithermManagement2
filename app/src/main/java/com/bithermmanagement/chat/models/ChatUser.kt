package com.bithermmanagement.chat.models

data class ChatUser(
    val username: String,
    val workTeam: String,
    val visibleName: String,
    val userImage: String? = null,
    var isOnline: Boolean = false,
    val isTeamLeader: Boolean = false, // Tiene * al final del workTeam
    val isAdmin: Boolean = false // Pertenece al grupo "leader"
)
