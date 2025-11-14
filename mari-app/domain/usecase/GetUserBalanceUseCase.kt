package com.Mari.mobileapp.domain.usecase

import com.Mari.mobileapp.domain.repository.UserRepository
import javax.inject.Inject

class GetUserBalanceUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(userId: String): Pair<Double, Double> {
        val user = userRepository.getUser(userId)
        return user?.let {
            Pair(it.readyCash, it.totalMoney)
        } ?: Pair(0.0, 0.0)
    }
}
