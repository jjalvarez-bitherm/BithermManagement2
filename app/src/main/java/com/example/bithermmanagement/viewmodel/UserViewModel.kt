package com.example.bithermmanagement.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bithermmanagement.database.entities.UserEntity
import com.example.bithermmanagement.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import com.bithermmanagement.database.AppDatabase

class UserViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: UserRepository
    val allUsers: Flow<List<UserEntity>>

    init {
        val userDao = AppDatabase.getDatabase(application).userDao()
        repository = UserRepository(userDao)
        allUsers = repository.allUsers
    }

    fun getUserByCod(cod: String) = viewModelScope.launch {
        repository.getUserByCod(cod)
    }

    fun getUserByEmail(email: String) = viewModelScope.launch {
        repository.getUserByEmail(email)
    }

    fun insertUser(user: UserEntity) = viewModelScope.launch {
        repository.insertUser(user)
    }

    fun updateUser(user: UserEntity) = viewModelScope.launch {
        repository.updateUser(user)
    }

    fun deleteUser(user: UserEntity) = viewModelScope.launch {
        repository.deleteUser(user)
    }

    fun deleteAllUsers() = viewModelScope.launch {
        repository.deleteAllUsers()
    }
} 