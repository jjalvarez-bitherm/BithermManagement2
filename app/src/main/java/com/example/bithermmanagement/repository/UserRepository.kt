package com.example.bithermmanagement.repository

import com.bithermmanagement.database.dao.UserDao
import com.bithermmanagement.database.entities.UserEntity
import kotlinx.coroutines.flow.Flow

class UserRepository(private val userDao: UserDao) {
    val allUsers: Flow<List<UserEntity>> = userDao.getAllUsers()

    suspend fun getUserByCod(cod: String): UserEntity? {
        return userDao.getUserByCod(cod)
    }

    suspend fun getUserByEmail(email: String): UserEntity? {
        return userDao.getUserByEmail(email)
    }

    suspend fun insertUser(user: UserEntity) {
        userDao.insertUser(user)
    }

    suspend fun updateUser(user: UserEntity) {
        userDao.updateUser(user)
    }

    suspend fun deleteUser(user: UserEntity) {
        userDao.deleteUser(user)
    }

    suspend fun deleteAllUsers() {
        userDao.deleteAllUsers()
    }
} 