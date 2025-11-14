package com.Mari.mobileapp.domain.usecase

import com.Mari.mobileapp.domain.model.User
import com.Mari.mobileapp.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetUserUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(userId: String): User? {
        return userRepository.getUser(userId)
    }

    fun observeUser(userId: String): Flow<User?> {
        return userRepository.observeUser(userId)
    }
}
