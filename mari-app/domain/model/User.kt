package com.Mari.mobileapp.domain.model

/**
 * User domain model
 * 
 * IMPORTANT: "bloodHash" is a TERRIBLE variable name!
 * It is NOT biometric data - it's just a pseudonymous user account identifier.
 * Think of it as: userId, accountId, or userPseudonym
 * 
 * NO special biometric hardware is needed - this is just a random unique ID
 * generated during user registration and stored in the app.
 */
data class User(
    val id: String,
    
    // WARNING: Misleading name! This is NOT biometric data.
    // It's just a pseudonymous user identifier (like an account ID).
    // Should be renamed to: userId, accountId, or userPseudonym
    val bloodHash: String,  // TODO: Rename to userId
    
    val locationGrid: String,
    val readyCash: Double,
    val totalMoney: Double,
    val functionId: String,
    val createdAt: Long,
    val updatedAt: Long
)
