package com.Mari.mobileapp.service.core

interface CoreGateway {
    suspend fun login(email: String, password: String): Pair<String, String> // returns token, userId
    suspend fun postTransaction(
        senderBioHash: String,
        receiverBioHash: String,
        amount: Double,
        locationGrid: String,
        coupon: String,
        motionX: Double,
        motionY: Double,
        motionZ: Double,
        timestampIso: String
    )

    suspend fun processSettlement(
        batchId: String,
        merchantId: String,
        bankMerchantId: String,
        seal: String,
        transactions: List<SettlementTransaction>
    ): SettlementResponse

    suspend fun getMerchantProfile(): MerchantProfile
}
