package com.bithermmanagement.chat.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bithermmanagement.chat.models.ChatUser
import com.bithermmanagement.chat.models.ChatGroup
import com.bithermmanagement.chat.models.ChatMessage
import com.bithermmanagement.chat.services.ChatDataService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

class ChatViewModel : ViewModel() {
    
    private var chatDataService: ChatDataService? = null
    
    private val _users = MutableStateFlow<List<ChatUser>>(emptyList())
    val users: StateFlow<List<ChatUser>> = _users.asStateFlow()
    
    private val _groups = MutableStateFlow<List<ChatGroup>>(emptyList())
    val groups: StateFlow<List<ChatGroup>> = _groups.asStateFlow()
    
    private val _currentGroup = MutableStateFlow<ChatGroup?>(null)
    val currentGroup: StateFlow<ChatGroup?> = _currentGroup.asStateFlow()
    
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    /**
     * Inicializa el servicio de datos del chat
     */
    fun initialize(context: Context) {
        Log.d("ChatViewModel", "Inicializando ChatViewModel con contexto")
        chatDataService = ChatDataService(context)
        Log.d("ChatViewModel", "ChatDataService creado: ${chatDataService != null}")
    }
    
    /**
     * Carga los usuarios y grupos desde Google Sheets (solo una vez por login)
     */
    fun loadChatData() {
        viewModelScope.launch {
            try {
                Log.d("ChatViewModel", "Iniciando carga de datos del chat...")
                _isLoading.value = true
                _error.value = null
                
                chatDataService?.let { service ->
                    Log.d("ChatViewModel", "Cargando usuarios desde Google Sheets...")
                    val users = service.loadUsersFromGoogleSheets()
                    Log.d("ChatViewModel", "Usuarios cargados: ${users.size}")
                    _users.value = users
                    
                    Log.d("ChatViewModel", "Cargando grupos desde caché...")
                    val groups = service.loadGroupsFromCache()
                    Log.d("ChatViewModel", "Grupos cargados: ${groups.size}")
                    _groups.value = groups
                    
                    // Marcar al usuario actual como online
                    markCurrentUserAsOnline()
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error cargando datos del chat", e)
                _error.value = "Error cargando datos del chat: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Obtiene el username del usuario actual
     */
    fun getCurrentUsername(): String? {
        // TODO: Obtener desde SharedPreferences
        return "admin1" // Temporal
    }
    
    /**
     * Marca al usuario actual como online
     */
    private fun markCurrentUserAsOnline() {
        // TODO: Obtener el username del usuario actual desde SharedPreferences o similar
        val currentUsername = getCurrentUsername() ?: return
        
        chatDataService?.let { service ->
            val updatedUsers = service.markUserAsOnline(currentUsername, _users.value)
            _users.value = updatedUsers
            
            // Actualizar grupos con usuarios actualizados
            val updatedGroups = _groups.value.map { group ->
                group.copy(members = group.members.map { user ->
                    updatedUsers.find { it.username == user.username } ?: user
                })
            }
            _groups.value = updatedGroups
        }
    }
    
    /**
     * Selecciona un grupo de chat
     */
    fun selectGroup(groupName: String) {
        val group = _groups.value.find { it.name == groupName }
        _currentGroup.value = group
        
        // TODO: Cargar mensajes del grupo seleccionado
        loadMessagesForGroup(groupName)
    }
    
    /**
     * Carga los mensajes para un grupo específico
     */
    private fun loadMessagesForGroup(groupName: String) {
        // TODO: Implementar carga de mensajes desde base de datos local o servidor
        // Por ahora usamos mensajes de ejemplo
        val exampleMessages = listOf(
            ChatMessage(
                id = "1",
                sender = _users.value.firstOrNull { it.username == "admin1" } ?: return,
                content = "¡Hola a todos!",
                timestamp = Date(),
                groupName = groupName
            ),
            ChatMessage(
                id = "2",
                sender = _users.value.firstOrNull { it.username == "inspector1" } ?: return,
                content = "Buenos días equipo",
                timestamp = Date(),
                groupName = groupName
            )
        )
        _messages.value = exampleMessages
    }
    
    /**
     * Envía un mensaje al grupo actual
     */
    fun sendMessage(content: String) {
        if (content.isBlank() || _currentGroup.value == null) return
        
        val currentUser = _users.value.firstOrNull { it.username == "admin1" } // Temporal
        if (currentUser == null) return
        
        val newMessage = ChatMessage(
            id = System.currentTimeMillis().toString(),
            sender = currentUser,
            content = content.trim(),
            timestamp = Date(),
            groupName = _currentGroup.value!!.name
        )
        
        _messages.value = _messages.value + newMessage
        
        // TODO: Guardar mensaje en base de datos local y enviar al servidor
    }
    
    /**
     * Obtiene el grupo por defecto para el usuario actual
     */
    fun getDefaultGroupForCurrentUser(): ChatGroup? {
        val currentUsername = "admin1" // Temporal
        return chatDataService?.getDefaultGroupForUser(currentUsername, _groups.value)
    }
    
    /**
     * Obtiene usuarios online
     */
    fun getOnlineUsers(): List<ChatUser> {
        return _users.value.filter { it.isOnline }
    }
    
    /**
     * Inicia una conversación privada con un usuario
     */
    fun startPrivateChat(user: ChatUser) {
        // TODO: Implementar conversación privada
    }
    
    /**
     * Limpia el estado del chat
     */
    fun clearChat() {
        _messages.value = emptyList()
        _currentGroup.value = null
    }
}
