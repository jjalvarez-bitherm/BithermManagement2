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
        val userData = googleSheetsManager.getUserData(user, pass)
        return if (userData != null && !userData.rol.isNullOrEmpty() && userData.rol != "0") {
            _loggedInUser.value = userData
            variablesManager.currentUser = userData
            userData
        } else {
            null
        }
    }
}