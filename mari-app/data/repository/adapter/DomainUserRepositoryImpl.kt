package com.Mari.mobileapp.data.repository.adapter

import com.Mari.mobileapp.domain.repository.UserRepository as DomainUserRepo
import com.Mari.mobileapp.domain.model.User as DomainUser
import com.Mari.mobileapp.data.repository.UserRepository as DataUserRepo
import com.Mari.mobileapp.data.model.User as DataUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DomainUserRepositoryImpl @Inject constructor(
    private val dataRepo: DataUserRepo
) : DomainUserRepo {
    override suspend fun createUser(user: DomainUser) {
        dataRepo.createUser(user.toData())
    }

    override suspend fun getUser(userId: String): DomainUser? =
        dataRepo.getUser(userId)?.toDomain()

    override suspend fun getUserByBloodHash(bloodHash: String): DomainUser? =
        dataRepo.getUserByBloodHash(bloodHash)?.toDomain()

    override suspend fun updateUser(user: DomainUser) {
        dataRepo.updateUser(user.toData())
    }

    override suspend fun deleteUser(userId: String) {
        dataRepo.deleteUser(userId)
    }

    override fun observeUser(userId: String): Flow<DomainUser?> =
        dataRepo.observeUser(userId).map { it?.toDomain() }

    override suspend fun updateBalance(userId: String, readyCash: Double, totalMoney: Double) {
        dataRepo.updateBalance(userId, readyCash, totalMoney)
    }

    private fun DomainUser.toData(): DataUser = DataUser(
        id = id,
        bloodHash = bloodHash,
        locationGrid = locationGrid,
        readyCash = readyCash,
        totalMoney = totalMoney,
        functionId = functionId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun DataUser.toDomain(): DomainUser = DomainUser(
        id = id,
        bloodHash = bloodHash,
        locationGrid = locationGrid,
        readyCash = readyCash,
        totalMoney = totalMoney,
        functionId = functionId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
