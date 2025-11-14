package com.Mari.mobileapp.service.bank

interface BankGateway {
    suspend fun verifyIncrementKey(incrementKey: String): Boolean
}
