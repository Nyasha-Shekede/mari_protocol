package com.Mari.mobileapp.domain.usecase

import com.Mari.mobileapp.domain.model.User
import com.Mari.mobileapp.domain.repository.UserRepository
import javax.inject.Inject

class CreateUserUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(user: User) {
        userRepository.createUser(user)
    }
}
