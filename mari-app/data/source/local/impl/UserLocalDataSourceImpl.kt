package com.Mari.mobileapp.data.source.local.impl

import com.Mari.mobileapp.data.local.dao.UserDao
import com.Mari.mobileapp.data.model.User
import com.Mari.mobileapp.data.source.local.UserLocalDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserLocalDataSourceImpl @Inject constructor(
    private val userDao: UserDao
) : UserLocalDataSource {

    override suspend fun saveUser(user: User) =
        userDao.insertUser(User.toEntity(user))

    override suspend fun getUser(userId: String): User? =
        userDao.getUser(userId)?.let { User.fromEntity(it) }

    override suspend fun getUserByBloodHash(bloodHash: String): User? =
        userDao.getUserByBloodHash(bloodHash)?.let { User.fromEntity(it) }

    override suspend fun updateUser(user: User) =
        userDao.updateUser(User.toEntity(user))

    override suspend fun deleteUser(userId: String) =
        userDao.deleteUser(userId)

    override fun observeUser(userId: String): Flow<User?> =
        userDao.observeUser(userId).map { it?.let { e -> User.fromEntity(e) } }
}
