package com.Mari.mobileapp.service.bank

import retrofit2.http.Body
import retrofit2.http.POST

interface BankApi {
    @POST("/hsm/verify")
    suspend fun verifyIncrementKey(@Body body: VerifyRequest): VerifyEnvelope<VerifyData>
}

data class VerifyRequest(val incrementKey: String)

data class VerifyEnvelope<T>(val success: Boolean, val data: T?)

data class VerifyData(
    val isValid: Boolean,
    val error: String?
)
