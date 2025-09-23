package com.bithermmanagement.chat.services

import android.content.Context
import android.util.Log
import com.bithermmanagement.chat.models.ChatUser
import com.bithermmanagement.chat.models.ChatGroup
import com.bithermmanagement.ausencias.services.GoogleSheetsTransferService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileReader
import java.io.FileWriter

class ChatDataService(private val context: Context) {
    
    companion object {
        private const val TAG = "ChatDataService"
        private const val USERS_CACHE_FILE = "chat_users_cache.json"
        private const val GROUPS_CACHE_FILE = "chat_groups_cache.json"
        private const val SPREADSHEET_ID = "1IyWGyxYDDTWY5SHh2xLBxtakSZX_xhZFo2jta4JeSW4"
        private const val SHEET_NAME = "EQUIPOS" // Nombre correcto de la hoja
    }
    
    private val gson = Gson()
    private val googleSheetsService = GoogleSheetsTransferService(context)
    
    /**
     * Carga los usuarios desde Google Sheets y los guarda en caché local
     * Solo se ejecuta una vez por login
     */
    suspend fun loadUsersFromGoogleSheets(): List<ChatUser> {
        return try {
            Log.d(TAG, "Cargando usuarios desde Google Sheets...")
            Log.d(TAG, "Spreadsheet ID: $SPREADSHEET_ID")
            Log.d(TAG, "Sheet Name: $SHEET_NAME")
            
            // Leer datos desde la hoja de usuarios
            val usersData = googleSheetsService.readUsersFromSheet(SPREADSHEET_ID, SHEET_NAME)
            Log.d(TAG, "Datos brutos recibidos: $usersData")
            
            if (usersData.isNotEmpty()) {
                Log.d(TAG, "Usuarios cargados desde Google Sheets: ${usersData.size}")
                
                // Buscar índices de columnas por nombre
                val headerRow = usersData.firstOrNull()
                if (headerRow == null) {
                    Log.e(TAG, "No se encontró fila de encabezados")
                    return emptyList()
                }
                
                val usernameIndex = headerRow.indexOfFirst { it.toString().equals("username", ignoreCase = true) }
                val workTeamIndex = headerRow.indexOfFirst { it.toString().equals("workTeam", ignoreCase = true) }
                val visibleNameIndex = headerRow.indexOfFirst { it.toString().equals("visibleName", ignoreCase = true) }
                val userImageIndex = headerRow.indexOfFirst { it.toString().equals("userImage", ignoreCase = true) }
                
                Log.d(TAG, "Índices encontrados: username=$usernameIndex, workTeam=$workTeamIndex, visibleName=$visibleNameIndex, userImage=$userImageIndex")
                
                if (usernameIndex == -1 || workTeamIndex == -1 || visibleNameIndex == -1) {
                    Log.e(TAG, "No se encontraron las columnas requeridas")
                    return emptyList()
                }
                
                // Convertir datos de Google Sheets a ChatUser (saltando la fila de encabezados)
                val users = usersData.drop(1).map { row ->
                    val username = row.getOrNull(usernameIndex)?.toString() ?: ""
                    val workTeam = row.getOrNull(workTeamIndex)?.toString() ?: ""
                    val visibleName = row.getOrNull(visibleNameIndex)?.toString() ?: username
                    val userImage = if (userImageIndex != -1) row.getOrNull(userImageIndex)?.toString() else null
                    
                    Log.d(TAG, "Procesando fila: username='$username', workTeam='$workTeam', visibleName='$visibleName'")
                    
                    // Determinar si es admin (leader) o jefe de equipo (*)
                    val isAdmin = workTeam.equals("leader", ignoreCase = true)
                    val isTeamLeader = workTeam.endsWith("*")
                    
                    ChatUser(
                        username = username,
                        workTeam = workTeam,
                        visibleName = visibleName,
                        userImage = userImage,
                        isOnline = false,
                        isTeamLeader = isTeamLeader,
                        isAdmin = isAdmin
                    )
                }.filter { it.username.isNotEmpty() }
                
                Log.d(TAG, "Usuarios procesados: ${users.size}")
                users.forEach { user ->
                    Log.d(TAG, "Usuario: ${user.username} - ${user.visibleName} - ${user.workTeam}")
                }
                
                // Guardar en caché local
                saveUsersToCache(users)
                
                // Crear y guardar grupos
                val groups = createGroupsFromUsers(users)
                saveGroupsToCache(groups)
                
                users
            } else {
                Log.w(TAG, "No se encontraron usuarios en Google Sheets, usando caché local")
                loadUsersFromCache()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cargando usuarios desde Google Sheets", e)
            Log.e(TAG, "Stack trace completo:", e)
            loadUsersFromCache()
        }
    }
    
    /**
     * Crea los grupos de chat basándose en los usuarios
     */
    private fun createGroupsFromUsers(users: List<ChatUser>): List<ChatGroup> {
        val groups = mutableListOf<ChatGroup>()
        
        // Grupo "TODOS"
        groups.add(ChatGroup(
            name = "todos",
            displayName = "TODOS",
            members = users,
            isDefault = false
        ))
        
        // Grupo "leader" (jefes)
        val leaders = users.filter { it.isAdmin }
        groups.add(ChatGroup(
            name = "leader",
            displayName = "TECN.",
            members = leaders,
            isDefault = false
        ))
        
        // Grupo "inspection"
        val inspectionUsers = users.filter { it.workTeam.startsWith("inspection") }
        groups.add(ChatGroup(
            name = "inspection",
            displayName = "INSPC.",
            members = inspectionUsers,
            isDefault = false
        ))
        
        // Grupo "repair"
        val repairUsers = users.filter { it.workTeam.startsWith("repair") }
        groups.add(ChatGroup(
            name = "repair",
            displayName = "REP.",
            members = repairUsers,
            isDefault = false
        ))
        
        // Grupo "iot"
        val iotUsers = users.filter { it.workTeam.startsWith("iot") }
        groups.add(ChatGroup(
            name = "iot",
            displayName = "IOT",
            members = iotUsers,
            isDefault = false
        ))
        
        return groups
    }
    
    /**
     * Obtiene el grupo por defecto para un usuario específico
     */
    fun getDefaultGroupForUser(username: String, groups: List<ChatGroup>): ChatGroup? {
        val user = groups.flatMap { it.members }.find { it.username == username }
        return user?.let { chatUser ->
            when {
                chatUser.isAdmin -> groups.find { it.name == "leader" }
                chatUser.workTeam.startsWith("inspection") -> groups.find { it.name == "inspection" }
                chatUser.workTeam.startsWith("repair") -> groups.find { it.name == "repair" }
                chatUser.workTeam.startsWith("iot") -> groups.find { it.name == "iot" }
                else -> groups.find { it.name == "todos" }
            }
        }
    }
    
    /**
     * Marca un usuario como online
     */
    fun markUserAsOnline(username: String, users: List<ChatUser>): List<ChatUser> {
        return users.map { user ->
            if (user.username == username) {
                user.copy(isOnline = true)
            } else {
                user
            }
        }
    }
    
    /**
     * Marca un usuario como offline
     */
    fun markUserAsOffline(username: String, users: List<ChatUser>): List<ChatUser> {
        return users.map { user ->
            if (user.username == username) {
                user.copy(isOnline = false)
            } else {
                user
            }
        }
    }
    
    private fun saveUsersToCache(users: List<ChatUser>) {
        try {
            val file = File(context.filesDir, USERS_CACHE_FILE)
            FileWriter(file).use { writer ->
                gson.toJson(users, writer)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando usuarios en caché", e)
        }
    }
    
    private fun saveGroupsToCache(groups: List<ChatGroup>) {
        try {
            val file = File(context.filesDir, GROUPS_CACHE_FILE)
            FileWriter(file).use { writer ->
                gson.toJson(groups, writer)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando grupos en caché", e)
        }
    }
    
    private fun loadUsersFromCache(): List<ChatUser> {
        return try {
            val file = File(context.filesDir, USERS_CACHE_FILE)
            if (file.exists()) {
                FileReader(file).use { reader ->
                    val type = object : TypeToken<List<ChatUser>>() {}.type
                    gson.fromJson(reader, type) ?: emptyList()
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cargando usuarios desde caché", e)
            emptyList()
        }
    }
    
    fun loadGroupsFromCache(): List<ChatGroup> {
        return try {
            val file = File(context.filesDir, GROUPS_CACHE_FILE)
            if (file.exists()) {
                FileReader(file).use { reader ->
                    val type = object : TypeToken<List<ChatGroup>>() {}.type
                    gson.fromJson(reader, type) ?: emptyList()
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cargando grupos desde caché", e)
            emptyList()
        }
    }
}
