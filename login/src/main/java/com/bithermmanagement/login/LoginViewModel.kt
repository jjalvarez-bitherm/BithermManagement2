package com.bithermmanagement.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bithermmanagement.core.data.LoginRepository
import com.bithermmanagement.login.data.LoginResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginRepository: LoginRepository
) : ViewModel() {

    private val _loginResult = MutableStateFlow<LoginResult>(LoginResult.Error(""))
    val loginResult: StateFlow<LoginResult> = _loginResult

    fun validateLogin(user: String, pass: String) {
        _loginResult.value = LoginResult.Loading
        viewModelScope.launch {
            val userData = loginRepository.validateCredentials(user, pass)
            if (userData != null) {
                _loginResult.value = LoginResult.Success(userData)
            } else {
                _loginResult.value = LoginResult.Error("Usuario o contraseña incorrectos")
            }
        }
    }
}

