package com.Mari.mobileapp.service.core

import javax.inject.Inject

class CoreGatewayImpl @Inject constructor(
    private val api: CoreApi,
    private val auth: AuthStore
) : CoreGateway {
    override suspend fun login(email: String, password: String): Pair<String, String> {
        val res = api.login(LoginRequest(email, password))
        val token = res.data?.token ?: throw IllegalStateException("Login failed: no token")
        val userId = res.data.user.id
        auth.setAuth(token, userId)
        return token to userId
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
        val body = CreateTransactionBody(
            senderBioHash = senderBioHash,
            receiverBioHash = receiverBioHash,
            amount = amount,
            locationGrid = locationGrid,
            coupon = coupon,
            physicsData = PhysicsData(
                location = Location(grid = locationGrid),
                motion = Motion(x = motionX, y = motionY, z = motionZ),
                timestamp = timestampIso
            )
        )
        val res = api.postTransaction(body)
        if (res.success != true) {
            throw IllegalStateException("Core transaction rejected")
        }
    }

    override suspend fun processSettlement(
        batchId: String,
        merchantId: String,
        bankMerchantId: String,
        seal: String,
        transactions: List<SettlementTransaction>
    ): SettlementResponse {
        val token = auth.token ?: throw IllegalStateException("Not authenticated")
        val request = SettlementRequest(
            batchId = batchId,
            merchantId = merchantId,
            bankMerchantId = bankMerchantId,
            seal = seal,
            transactions = transactions
        )
        val res = api.processSettlement("Bearer $token", request)
        return res.data ?: throw IllegalStateException("Settlement failed")
    }

    override suspend fun getMerchantProfile(): MerchantProfile {
        val token = auth.token ?: throw IllegalStateException("Not authenticated")
        val res = api.getMerchantProfile("Bearer $token")
        return res.data ?: throw IllegalStateException("Failed to fetch profile")
    }
}
