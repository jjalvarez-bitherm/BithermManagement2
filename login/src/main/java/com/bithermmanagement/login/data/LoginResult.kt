package com.bithermmanagement.login.data

import com.bithermmanagement.core.data.UserData

sealed class LoginResult {
    data class Success(val data: UserData) : LoginResult()
    data class Error(val message: String) : LoginResult()
    object Loading : LoginResult()
}

