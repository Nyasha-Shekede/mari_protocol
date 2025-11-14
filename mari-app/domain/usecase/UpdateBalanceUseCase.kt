package com.Mari.mobileapp.domain.usecase

import com.Mari.mobileapp.domain.repository.UserRepository
import javax.inject.Inject

class UpdateBalanceUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(userId: String, readyCash: Double, totalMoney: Double) {
        userRepository.updateBalance(userId, readyCash, totalMoney)
    }
}
