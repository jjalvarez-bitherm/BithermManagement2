package com.bithermmanagement.core.data

import com.bithermmanagement.core.data.UserData
import com.bithermmanagement.core.data.GoogleSheetsManager
import com.bithermmanagement.core.utils.VariablesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoginRepository @Inject constructor(
    private val variablesManager: VariablesManager,
    private val googleSheetsManager: GoogleSheetsManager
) {
    private val _loggedInUser = MutableStateFlow<UserData?>(null)

    init {
        _loggedInUser.value = variablesManager.currentUser
    }

    fun getLoggedInUser(): Flow<UserData?> {
        return _loggedInUser
    }

    suspend fun validateCredentials(user: String, pass: String): UserData? {
        android.util.Log.d("LoginRepository", "=== validateCredentials INICIADO para usuario: $user ===")
        
        val userData = googleSheetsManager.getUserData(user, pass)
        android.util.Log.d("LoginRepository", "getUserData resultado: $userData")
        
        return if (userData != null && !userData.rol.isNullOrEmpty() && userData.rol != "0") {
            android.util.Log.d("LoginRepository", "Usuario válido encontrado: ${userData.nombre} ${userData.apellidos}")
            android.util.Log.d("LoginRepository", "Asignando a _loggedInUser...")
            _loggedInUser.value = userData
            android.util.Log.d("LoginRepository", "Asignando a variablesManager.currentUser...")
            variablesManager.currentUser = userData
            android.util.Log.d("LoginRepository", "VariablesManager.currentUser asignado: ${variablesManager.currentUser}")
            android.util.Log.d("LoginRepository", "=== validateCredentials EXITOSO ===")
            userData
        } else {
            android.util.Log.w("LoginRepository", "Usuario no válido o credenciales incorrectas")
            android.util.Log.d("LoginRepository", "=== validateCredentials FALLIDO ===")
            null
        }
    }
}