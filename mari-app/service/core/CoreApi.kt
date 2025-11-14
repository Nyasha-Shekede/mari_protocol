package com.Mari.mobileapp.service.core

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Header
import retrofit2.http.GET

interface CoreApi {
    @POST("/api/transactions")
    suspend fun postTransaction(@Body body: CreateTransactionBody): ApiResponse<TransactionResponse>

    @POST("/api/auth/login")
    suspend fun login(@Body body: LoginRequest): ApiResponse<LoginResponse>

    @POST("/api/settlement/process")
    suspend fun processSettlement(
        @Header("Authorization") bearer: String,
        @Body body: SettlementRequest
    ): ApiResponse<SettlementResponse>

    @GET("/api/merchant/profile")
    suspend fun getMerchantProfile(
        @Header("Authorization") bearer: String
    ): ApiResponse<MerchantProfile>
}

// --- Request/Response models ---
data class CreateTransactionBody(
    val senderBioHash: String,
    val receiverBioHash: String,
    val amount: Double,
    val locationGrid: String,
    val coupon: String,
    val physicsData: PhysicsData
)

data class PhysicsData(
    val location: Location,
    val motion: Motion,
    val timestamp: String
)

data class Location(val grid: String)

data class Motion(val x: Double, val y: Double, val z: Double)

data class ApiResponse<T>(val success: Boolean, val data: T?)

data class TransactionResponse(
    val transactionId: String?,
    val status: String?
)

// Auth
data class LoginRequest(val email: String, val password: String)
data class LoginResponse(val token: String, val user: LoginUser)
data class LoginUser(val id: String, val username: String)

// Settlement
data class SettlementRequest(
    val batchId: String,
    val merchantId: String,
    val bankMerchantId: String,
    val seal: String,
    val transactions: List<SettlementTransaction>
)

data class SettlementTransaction(
    val id: String,
    val amount: Double,
    val coupon: String,
    val physicsData: PhysicsData
)

data class SettlementResponse(
    val batchId: String,
    val processed: Int,
    val successful: Int,
    val failed: Int,
    val totalAmount: Double,
    val totalCommission: Double,
    val incrementKey: String,
    val transactions: List<Any>
)

data class MerchantProfile(
    val merchantId: String,
    val bankMerchantId: String
)
