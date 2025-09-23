package com.bithermmanagement.chat.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.bithermmanagement.R
import com.bithermmanagement.chat.models.ChatGroup
import com.bithermmanagement.chat.models.ChatUser
import com.bithermmanagement.chat.services.UserConnectionService
import com.bithermmanagement.chat.viewmodels.ChatViewModel
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class BithermChatFragment : Fragment() {
    
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var tabLayout: com.google.android.material.tabs.TabLayout
    private lateinit var viewPager: androidx.viewpager2.widget.ViewPager2
    private lateinit var userConnectionService: UserConnectionService
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_bitherm_chat, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        Log.d("BithermChatFragment", "=== FRAGMENT INICIALIZADO ===")
        
        // Inicializar vistas
        tabLayout = view.findViewById(R.id.tabLayout)
        viewPager = view.findViewById(R.id.viewPager)
        
        // Inicializar servicio de conexión
        userConnectionService = UserConnectionService(requireContext())
        
        Log.d("BithermChatFragment", "Vistas inicializadas: tabLayout=${tabLayout != null}, viewPager=${viewPager != null}")
        
        // Inicializar ViewModel
        viewModel.initialize(requireContext())
        
        // Configurar ViewPager
        setupViewPager()
        
        // Observar cambios en los datos
        observeData()
        
        // Cargar datos del chat
        Log.d("BithermChatFragment", "Iniciando carga de datos del chat...")
        viewModel.loadChatData()
        
        // Marcar usuario actual como online
        markCurrentUserAsOnline()
    }
    
    private fun markCurrentUserAsOnline() {
        lifecycleScope.launch {
            try {
                val currentUsername = viewModel.getCurrentUsername()
                if (currentUsername != null) {
                    userConnectionService.markUserAsOnline(currentUsername)
                    userConnectionService.startActivityMonitoring(currentUsername)
                }
            } catch (e: Exception) {
                Log.e("BithermChatFragment", "Error marcando usuario como online", e)
            }
        }
    }
    
    private fun setupViewPager() {
        viewPager.adapter = ChatPagerAdapter(this)
        
        // Configurar TabLayout con ViewPager
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            // Los tabs se configurarán dinámicamente cuando se carguen los grupos
        }.attach()
    }
    
    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Observar grupos
            viewModel.groups.collect { groups ->
                Log.d("BithermChatFragment", "Grupos recibidos: ${groups.size}")
                if (groups.isNotEmpty()) {
                    Log.d("BithermChatFragment", "Configurando tabs para grupos: ${groups.map { it.displayName }}")
                    setupTabs(groups)
                    
                    // Seleccionar grupo por defecto
                    val defaultGroup = viewModel.getDefaultGroupForCurrentUser()
                    defaultGroup?.let { group ->
                        val defaultIndex = groups.indexOfFirst { it.name == group.name }
                        if (defaultIndex >= 0) {
                            viewPager.setCurrentItem(defaultIndex, false)
                        }
                    }
                } else {
                    Log.w("BithermChatFragment", "No hay grupos disponibles")
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            // Observar usuarios online
            viewModel.users.collect { users ->
                Log.d("BithermChatFragment", "Usuarios recibidos: ${users.size}")
                updateOnlineUsersCarousel(users)
            }
        }
    }
    
    private fun setupTabs(groups: List<ChatGroup>) {
        // Limpiar tabs existentes
        tabLayout.removeAllTabs()
        
        // Agregar tabs para cada grupo
        groups.forEach { group ->
            val tab = tabLayout.newTab().setText(group.displayName)
            tabLayout.addTab(tab)
        }
        
        // Actualizar el adaptador del ViewPager
        (viewPager.adapter as? ChatPagerAdapter)?.updateGroups(groups)
    }
    
    private fun updateOnlineUsersCarousel(users: List<ChatUser>) {
        // Obtener la referencia al carrusel horizontal
        val usersCarousel = view?.findViewById<LinearLayout>(R.id.usersCarousel)
        usersCarousel?.removeAllViews()
        
        lifecycleScope.launch {
            users.forEach { user ->
                val userAvatar = createUserAvatar(user)
                usersCarousel?.addView(userAvatar)
            }
        }
    }
    
    private fun createUserAvatar(user: ChatUser): View {
        val avatarView = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_user_avatar, null, false)
        
        val avatarImage = avatarView.findViewById<ImageView>(R.id.avatarImage)
        val onlineIndicator = avatarView.findViewById<View>(R.id.onlineIndicator)
        val userNameText = avatarView.findViewById<TextView>(R.id.userNameText)
        
        // Configurar avatar (por ahora usamos icono genérico)
        avatarImage.setImageResource(R.drawable.ic_person)
        
        // Configurar nombre del usuario
        userNameText.text = user.visibleName ?: user.username
        
        // Configurar indicador de estado online/offline
        lifecycleScope.launch {
            try {
                val timeSinceLastActivity = userConnectionService.getTimeSinceLastActivity(user.username)
                val isOnline = userConnectionService.isUserOnline(user.username)
                
                when {
                    isOnline && timeSinceLastActivity < 20 * 60 * 1000L -> {
                        // Usuario online (menos de 20 min) - LED verde
                        onlineIndicator.setBackgroundResource(R.drawable.bg_circle_green)
                        onlineIndicator.visibility = View.VISIBLE
                    }
                    timeSinceLastActivity < 20 * 60 * 1000L -> {
                        // Usuario activo recientemente (menos de 20 min) - LED amarillo
                        onlineIndicator.setBackgroundResource(R.drawable.bg_circle_yellow)
                        onlineIndicator.visibility = View.VISIBLE
                    }
                    else -> {
                        // Usuario inactivo (más de 20 min) - Sin LED
                        onlineIndicator.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                Log.e("BithermChatFragment", "Error configurando indicador para usuario: ${user.username}", e)
                onlineIndicator.visibility = View.GONE
            }
        }
        
        // Configurar click para iniciar chat privado
        avatarView.setOnClickListener {
            viewModel.startPrivateChat(user)
        }
        
        return avatarView
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        // Detener monitoreo de actividad
        try {
            val currentUsername = viewModel.getCurrentUsername()
            if (currentUsername != null) {
                userConnectionService.stopActivityMonitoring()
                lifecycleScope.launch {
                    userConnectionService.markUserAsOffline(currentUsername)
                }
            }
        } catch (e: Exception) {
            Log.e("BithermChatFragment", "Error deteniendo monitoreo", e)
        }
    }
    
    private inner class ChatPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        private var groups: List<ChatGroup> = emptyList()
        
        fun updateGroups(newGroups: List<ChatGroup>) {
            groups = newGroups
            notifyDataSetChanged()
        }
        
        override fun getItemCount(): Int = groups.size
        
        override fun createFragment(position: Int): Fragment {
            val group = groups[position]
            return ChatConversationFragment.newInstance(group.name, "group", group.displayName)
        }
    }
}
