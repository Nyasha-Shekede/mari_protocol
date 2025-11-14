package com.Mari.mobileapp.data.source.local

import com.Mari.mobileapp.data.model.User
import kotlinx.coroutines.flow.Flow

interface UserLocalDataSource {
    suspend fun saveUser(user: User)
    suspend fun getUser(userId: String): User?
    suspend fun getUserByBloodHash(bloodHash: String): User?
    suspend fun updateUser(user: User)
    suspend fun deleteUser(userId: String)
    fun observeUser(userId: String): Flow<User?>
}
