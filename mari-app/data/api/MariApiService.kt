package com.Mari.mobileapp.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit API service for Mari Server
 */
interface MariApiService {
    
    /**
     * Lookup user by phone number
     * GET /api/users/lookup?phone=XXX
     */
    @GET("api/users/lookup")
    suspend fun lookupUserByPhone(
        @Query("phone") phone: String
    ): Response<UserLookupResponse>
    
    /**
     * Lookup user by username
     * GET /api/users/lookup?username=XXX
     */
    @GET("api/users/lookup")
    suspend fun lookupUserByUsername(
        @Query("username") username: String
    ): Response<UserLookupResponse>
}

/**
 * Response from /api/users/lookup
 */
data class UserLookupResponse(
    val success: Boolean,
    val data: UserLookupData?,
    val error: String?
)

data class UserLookupData(
    val id: String,
    val username: String,
    val phoneNumber: String
)
