package com.Mari.mobileapp.service.bank

import javax.inject.Inject

class BankGatewayImpl @Inject constructor(
    private val api: BankApi
) : BankGateway {
    override suspend fun verifyIncrementKey(incrementKey: String): Boolean {
        val res = api.verifyIncrementKey(VerifyRequest(incrementKey))
        return res.success && (res.data?.isValid == true)
    }
}
