package com.Mari.mobileapp.service.core

import javax.inject.Inject

class FakeCoreGateway @Inject constructor() : CoreGateway {
    override suspend fun login(email: String, password: String): Pair<String, String> {
        // Return a stable fake token and user id
        return ("fake-token" to "user-dev-001")
    }

    override suspend fun postTransaction(
        senderBioHash: String,
        receiverBioHash: String,
        amount: Double,
        locationGrid: String,
        coupon: String,
        motionX: Double,
        motionY: Double,
        motionZ: Double,
        timestampIso: String
    ) {
        // No-op in dev mode
    }

    override suspend fun processSettlement(
        batchId: String,
        merchantId: String,
        bankMerchantId: String,
        seal: String,
        transactions: List<SettlementTransaction>
    ): SettlementResponse {
        val processed = transactions.size
        val successful = processed
        val failed = 0
        val totalAmount = transactions.sumOf { it.amount }
        val totalCommission = 0.0
        val incrementKey = "inc-dev-001"
        // The response expects a list for transactions; we can echo empty details
        return SettlementResponse(
            batchId = batchId.ifBlank { "dev-batch-001" },
            processed = processed,
            successful = successful,
            failed = failed,
            totalAmount = totalAmount,
            totalCommission = totalCommission,
            incrementKey = incrementKey,
            transactions = emptyList()
        )
    }

    override suspend fun getMerchantProfile(): MerchantProfile {
        return MerchantProfile(
            merchantId = "merchant-dev-001",
            bankMerchantId = "bank-merchant-dev-001"
        )
    }
}
