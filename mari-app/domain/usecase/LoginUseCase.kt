package com.Mari.mobileapp.domain.usecase

import com.Mari.mobileapp.service.core.CoreGateway
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val coreGateway: CoreGateway
) {
    suspend operator fun invoke(email: String, password: String): Pair<String, String> {
        return coreGateway.login(email, password)
    }
}
