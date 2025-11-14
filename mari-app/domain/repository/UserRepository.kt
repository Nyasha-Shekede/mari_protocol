package com.Mari.mobileapp.domain.repository

import com.Mari.mobileapp.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun createUser(user: User)
    suspend fun getUser(userId: String): User?
    suspend fun getUserByBloodHash(bloodHash: String): User?
    suspend fun updateUser(user: User)
    suspend fun deleteUser(userId: String)
    fun observeUser(userId: String): Flow<User?>
    suspend fun updateBalance(userId: String, readyCash: Double, totalMoney: Double)
}
