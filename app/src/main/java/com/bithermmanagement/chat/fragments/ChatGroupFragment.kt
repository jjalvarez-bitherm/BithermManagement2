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
import android.view.ViewGroup.LayoutParams
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bithermmanagement.R
import kotlinx.coroutines.*

class ChatGroupFragment : Fragment() {
    
    companion object {
        private const val ARG_GROUP_NAME = "group_name"
        private const val ARG_GROUP_DISPLAY_NAME = "group_display_name"
        
        fun newInstance(groupName: String, groupDisplayName: String): ChatGroupFragment {
            val fragment = ChatGroupFragment()
            val args = Bundle()
            args.putString(ARG_GROUP_NAME, groupName)
            args.putString(ARG_GROUP_DISPLAY_NAME, groupDisplayName)
            fragment.arguments = args
            return fragment
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chat_group, container, false)
        
        val groupName = arguments?.getString(ARG_GROUP_NAME) ?: ""
        val groupDisplayName = arguments?.getString(ARG_GROUP_DISPLAY_NAME) ?: ""
        
        Log.d("ChatGroupFragment", "Fragment creado para grupo: $groupName ($groupDisplayName)")
        
        // Configurar click listener para abrir la conversación
        view.setOnClickListener {
            Log.d("ChatGroupFragment", "Click en grupo: $groupName")
            openGroupConversation(groupName, groupDisplayName)
        }
        
        return view
    }
    
        private fun openGroupConversation(groupName: String, groupDisplayName: String) {
        Log.d("ChatGroupFragment", "Abriendo conversación de grupo: $groupName - $groupDisplayName")

        // Crear y mostrar el diálogo de conversación
        val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.fragment_chat_conversation_whatsapp)

        // Configurar el diálogo
        dialog.window?.setLayout(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        )

        // Configurar elementos del diálogo
        val buttonBack = dialog.findViewById<ImageButton>(R.id.buttonBack)
        val textViewChatName = dialog.findViewById<TextView>(R.id.textViewChatName)
        val textViewChatStatus = dialog.findViewById<TextView>(R.id.textViewChatStatus)
        val editTextMessage = dialog.findViewById<EditText>(R.id.editTextMessage)
        val buttonSend = dialog.findViewById<ImageButton>(R.id.buttonSend)
        val recyclerView = dialog.findViewById<RecyclerView>(R.id.recyclerViewMessages)
        
        textViewChatName.text = groupDisplayName
        textViewChatStatus.text = "Grupo de trabajo"
        
        // Configurar RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        val adapter = com.bithermmanagement.chat.adapters.ChatMessagesAdapter()
        recyclerView.adapter = adapter
        
        // Cargar mensajes existentes
        loadMessagesForChat(groupName, adapter, true) // true = chat grupal
        
        // Guardar referencia al adaptador en el diálogo
        dialog.findViewById<View>(android.R.id.content).tag = adapter
        
        // REGISTRAR USUARIO EN FIREBASE Y ACTIVAR LISTENER
        val prefs = requireContext().getSharedPreferences("bitherm_prefs", android.content.Context.MODE_PRIVATE)
        val currentUsername = prefs.getString("username", "") ?: ""
        
        if (currentUsername.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val firebaseService = com.bithermmanagement.chat.services.FirebaseChatService(requireContext())
                    
                    // Registrar usuario en Firebase
                    firebaseService.registerUser(currentUsername)
                    
                    // Activar listener para mensajes dirigidos AL GRUPO
                    firebaseService.listenToChat(groupName) { message ->
                        Log.d("ChatGroupFragment", "Mensaje recibido en tiempo real para grupo $groupName: ${message.content}")
                        
                        // Actualizar UI en el hilo principal
                        requireActivity().runOnUiThread {
                            adapter.addMessage(message, currentUsername)
                            // Scroll al último mensaje
                            recyclerView.smoothScrollToPosition(adapter.itemCount - 1)
                        }
                    }
                    
                    Log.d("ChatGroupFragment", "Listener de Firebase activado para grupo: $groupName")
                    
                } catch (e: Exception) {
                    Log.e("ChatGroupFragment", "Error configurando Firebase para grupo", e)
                }
            }
        }
        
        // Configurar botón de volver
        buttonBack.setOnClickListener {
            // Detener listener de Firebase al cerrar
            val firebaseService = com.bithermmanagement.chat.services.FirebaseChatService(requireContext())
            firebaseService.stopListening()
            dialog.dismiss()
        }
        
        // Configurar envío de mensajes
        buttonSend.setOnClickListener {
            val messageText = editTextMessage.text.toString().trim()
            if (messageText.isNotEmpty()) {
                sendMessageDirectly(groupName, "group", messageText, dialog)
                editTextMessage.text.clear()
            }
        }

        // Mostrar el diálogo
        dialog.show()
    }
    
    /**
     * Envía mensaje directamente sin fragmentos
     */
    private fun sendMessageDirectly(chatId: String, chatType: String, messageText: String, dialog: Dialog) {
        val prefs = requireContext().getSharedPreferences("bitherm_prefs", android.content.Context.MODE_PRIVATE)
        val currentUsername = prefs.getString("username", "") ?: ""
        
        if (currentUsername.isEmpty()) {
            Log.e("ChatGroupFragment", "No se pudo obtener username actual")
            return
        }
        
        // Obtener el adaptador del tag del diálogo
        val adapter = dialog.findViewById<View>(android.R.id.content).tag as? com.bithermmanagement.chat.adapters.ChatMessagesAdapter
        if (adapter == null) {
            Log.e("ChatGroupFragment", "No se pudo obtener el adaptador del diálogo")
            return
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("ChatGroupFragment", "Enviando mensaje: $messageText a $chatId")
                
                val chatMessageService = com.bithermmanagement.chat.services.ChatMessageService(requireContext())
                val messageId = chatMessageService.sendMessage(
                    senderUsername = currentUsername,
                    chatId = chatId,
                    chatType = chatType,
                    content = messageText
                )
                
                Log.d("ChatGroupFragment", "Mensaje enviado con ID: $messageId")
                
                // Recargar mensajes para mostrar el nuevo
                loadMessagesForChat(chatId, adapter, true) // true = chat grupal
                
            } catch (e: Exception) {
                Log.e("ChatGroupFragment", "Error enviando mensaje", e)
            }
        }
    }
    
    /**
     * Carga mensajes para un chat específico
     */
    private fun loadMessagesForChat(chatId: String, adapter: com.bithermmanagement.chat.adapters.ChatMessagesAdapter, isGroup: Boolean) {
        val prefs = requireContext().getSharedPreferences("bitherm_prefs", android.content.Context.MODE_PRIVATE)
        val currentUsername = prefs.getString("username", "") ?: ""
        
        if (currentUsername.isEmpty()) {
            Log.e("ChatGroupFragment", "No se pudo obtener username actual")
            return
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val chatMessageService = com.bithermmanagement.chat.services.ChatMessageService(requireContext())
                val messages = chatMessageService.getMessagesForChat(chatId)
                
                withContext(Dispatchers.Main) {
                    adapter.updateMessages(messages, currentUsername, isGroup)
                    
                    // Scroll al último mensaje
                    if (messages.isNotEmpty()) {
                        val recyclerView = adapter.itemCount - 1
                        // TODO: Implementar scroll automático
                    }
                }
                
            } catch (e: Exception) {
                Log.e("ChatGroupFragment", "Error cargando mensajes", e)
            }
        }
    }
}
