package com.bithermmanagement.chat.fragments

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bithermmanagement.R
import com.bithermmanagement.chat.services.ChatMessageService
import com.bithermmanagement.chat.services.FichajeBasedOnlineManager
import kotlinx.coroutines.*

class ChatConversationFragment : Fragment() {
    
    private lateinit var chatId: String
    private lateinit var chatType: String
    private lateinit var chatName: String
    private lateinit var currentUsername: String
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var editTextMessage: EditText
    private lateinit var buttonSend: ImageButton
    private lateinit var textViewChatName: TextView
    private lateinit var textViewChatStatus: TextView
    
    private lateinit var chatMessageService: ChatMessageService
    private lateinit var onlineManager: FichajeBasedOnlineManager
    
    companion object {
        fun newInstance(chatId: String, chatType: String, chatName: String): ChatConversationFragment {
            val fragment = ChatConversationFragment()
            fragment.chatId = chatId
            fragment.chatType = chatType
            fragment.chatName = chatName
            return fragment
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chat_conversation_whatsapp, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Inicializar servicios
        chatMessageService = ChatMessageService(requireContext())
        onlineManager = FichajeBasedOnlineManager(requireContext())
        
        // Obtener username actual
        val prefs = requireContext().getSharedPreferences("bitherm_prefs", android.content.Context.MODE_PRIVATE)
        currentUsername = prefs.getString("username", "") ?: ""
        
        // Inicializar vistas
        initializeViews(view)
        setupClickListeners()
        loadMessages()
    }
    
    private fun initializeViews(view: View) {
        recyclerView = view.findViewById(R.id.recyclerViewMessages)
        editTextMessage = view.findViewById(R.id.editTextMessage)
        buttonSend = view.findViewById(R.id.buttonSend)
        textViewChatName = view.findViewById(R.id.textViewChatName)
        textViewChatStatus = view.findViewById(R.id.textViewChatStatus)
        
        // Configurar RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)
        // TODO: Implementar adaptador de mensajes
        
        // Configurar header
        textViewChatName.text = chatName
        textViewChatStatus.text = if (chatType == "individual") "Chat privado" else "Grupo de trabajo"
    }
    
    private fun setupClickListeners() {
        buttonSend.setOnClickListener {
            sendMessage()
        }
        
        // Configurar botón de volver
        view?.findViewById<ImageButton>(R.id.buttonBack)?.setOnClickListener {
            // Volver al fragmento anterior
            parentFragmentManager.popBackStack()
        }
    }
    
    private fun sendMessage() {
        val messageText = editTextMessage.text.toString().trim()
        if (messageText.isEmpty()) return
        
        // Limpiar input
        editTextMessage.text.clear()
        
        // Enviar mensaje
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("ChatConversation", "Enviando mensaje: $messageText a $chatId")
                
                val messageId = chatMessageService.sendMessage(
                    senderUsername = currentUsername,
                    chatId = chatId,
                    chatType = chatType,
                    content = messageText
                )
                
                Log.d("ChatConversation", "Mensaje enviado con ID: $messageId")
                
                // Recargar mensajes
                withContext(Dispatchers.Main) {
                    loadMessages()
                }
                
            } catch (e: Exception) {
                Log.e("ChatConversation", "Error enviando mensaje", e)
                withContext(Dispatchers.Main) {
                    // Mostrar error al usuario
                    // TODO: Implementar Toast o Snackbar
                }
            }
        }
    }
    
    private fun loadMessages() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val messages = chatMessageService.getMessagesForChat(chatId)
                
                withContext(Dispatchers.Main) {
                    // TODO: Actualizar RecyclerView con mensajes
                    Log.d("ChatConversation", "Cargados ${messages.size} mensajes")
                }
                
            } catch (e: Exception) {
                Log.e("ChatConversation", "Error cargando mensajes", e)
            }
        }
    }
}
