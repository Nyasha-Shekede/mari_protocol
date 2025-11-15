package com.Mari.mobileapp.core.protocol

import com.Mari.mobileapp.core.crypto.MariCryptoManager
import com.Mari.mobileapp.core.physics.PhysicsSensorManager

/**
 * Handles generation and parsing of Mari protocol strings
 * 
 * IMPORTANT NOTE ON VARIABLE NAMES:
 * - "bloodHash", "senderBio", "receiverBio", "bioHash" are TERRIBLE names!
 * - They are NOT biometric data - they're just pseudonymous user account identifiers
 * - Think of them as: userId, senderId, receiverId, accountId
 * - NO special biometric hardware is needed - these are just random unique IDs
 * 
 * The real security comes from:
 * 1. Device key (kid) - hardware-backed ECDSA P-256 key in Android Keystore
 * 2. Motion seal - accelerometer data for anti-automation
 * 3. Location grid - coarse GPS location
 * 4. User unlocks device with ANY method (fingerprint/PIN/face/pattern)
 */
class MariProtocol(
    private val cryptoManager: MariCryptoManager,
    private val physicsSensorManager: PhysicsSensorManager
) {
    companion object {
        private const val PROTOCOL_VERSION = 1
        private const val COUPON_EXPIRY_MS = 5 * 60 * 1000 // 5 minutes
        private const val MAX_GRID_DISTANCE = 2 // Maximum grid distance for validation
    }

    /**
     * Generate a Function ID string
     * 
     * @param bloodHash WARNING: Misleading name! This is NOT biometric data.
     *                  It's just a user account identifier (should be renamed to userId)
     */
    fun generateFunctionId(
        bloodHash: String,  // TODO: Rename to userId - NOT biometric data!
        readyCash: Double,
        totalMoney: Double
    ): String {
        val grid = physicsSensorManager.locationGrid.value
        val seal = cryptoManager.generateSeal()
        val timestamp = System.currentTimeMillis()
        val movementIntensity = physicsSensorManager.calculateMovementIntensity()

        return "Mari://v${PROTOCOL_VERSION}?g=${grid}&b=${bloodHash}&rc=${readyCash}&tm=${totalMoney}&seal=${seal}&ts=${timestamp}&mi=${movementIntensity}"
    }

    /**
     * Generate a Transfer Coupon string
     * 
     * @param senderBio WARNING: Misleading name! This is NOT biometric data.
     *                  It's just the sender's account identifier (should be renamed to senderId)
     * @param receiverBio WARNING: Misleading name! This is NOT biometric data.
     *                    It's just the receiver's account identifier (should be renamed to receiverId)
     */
    fun generateTransferCoupon(
        senderBio: String,      // TODO: Rename to senderId - NOT biometric data!
        receiverBio: String,    // TODO: Rename to receiverId - NOT biometric data!
        amount: Double,
        includeTimestamp: Boolean = true
    ): String {
        val grid = physicsSensorManager.locationGrid.value
        val expiry = if (includeTimestamp) System.currentTimeMillis() + COUPON_EXPIRY_MS else 0L
        val signedSeal = cryptoManager.generateSignedSeal()
        val movementIntensity = physicsSensorManager.calculateMovementIntensity()
        val lightLevel = physicsSensorManager.lightLevel.value

        return "Mari://xfer?from=${senderBio}&to=${receiverBio}&val=${amount}&g=${grid}&exp=${expiry}&s=${signedSeal.seal}&sig=${signedSeal.signature}&kid=${signedSeal.kid}&mi=${movementIntensity}&ll=${lightLevel}"
    }

    /**
     * Generate a Location Verification string
     */
    fun generateLocationVerification(): String {
        val grid = physicsSensorManager.locationGrid.value
        val seal = cryptoManager.generateSeal()
        val timestamp = System.currentTimeMillis()
        val movementIntensity = physicsSensorManager.calculateMovementIntensity()

        return "Mari://loc?g=${grid}&s=${seal}&ts=${timestamp}&mi=${movementIntensity}"
    }

    /**
     * Generate a Physics Challenge string
     */
    fun generatePhysicsChallenge(): String {
        val motion = physicsSensorManager.motionData.value
        val gyro = physicsSensorManager.gyroscopeData.value
        val magnetic = physicsSensorManager.magneticData.value
        val seal = cryptoManager.generateSeal()

        return "Mari://challenge?mx=${motion.x}&my=${motion.y}&mz=${motion.z}&gx=${gyro.x}&gy=${gyro.y}&gz=${gyro.z}&mx=${magnetic.x}&my=${magnetic.y}&mz=${magnetic.z}&s=${seal}"
    }

    /**
     * Parse an Mari string into its components
     */
    fun parseMariString(MariString: String): MariEntity {
        return when {
            MariString.startsWith("Mari://v") -> parseFunctionId(MariString)
            MariString.startsWith("Mari://xfer") -> parseTransferCoupon(MariString)
            MariString.startsWith("Mari://loc") -> parseLocationVerification(MariString)
            MariString.startsWith("Mari://challenge") -> parsePhysicsChallenge(MariString)
            else -> throw IllegalArgumentException("Invalid Mari string format")
        }
    }

    /**
     * Parse a Function ID string
     */
    private fun parseFunctionId(functionId: String): FunctionIdEntity {
        val regex = """Mari://v(\d+)\?g=([^&]+)&b=([^&]+)&rc=([^&]+)&tm=([^&]+)&seal=([^&]+)&ts=([^&]+)&mi=([^&]+)""".toRegex()
        val matchResult = regex.find(functionId)

        if (matchResult != null) {
            val (version, grid, bloodHash, readyCash, totalMoney, seal, timestamp, movementIntensity) = matchResult.destructured
            return FunctionIdEntity(
                version = version.toInt(),
                grid = grid,
                bloodHash = bloodHash,
                readyCash = readyCash.toDouble(),
                totalMoney = totalMoney.toDouble(),
                seal = seal,
                timestamp = timestamp.toLong(),
                movementIntensity = movementIntensity.toFloat()
            )
        }

        throw IllegalArgumentException("Invalid Function ID format")
    }

    /**
     * Parse a Transfer Coupon string
     */
    private fun parseTransferCoupon(coupon: String): TransferCouponEntity {
        val regex = """Mari://xfer\?from=([^&]+)&to=([^&]+)&val=([^&]+)&g=([^&]+)&exp=([^&]+)&s=([^&]+)&mi=([^&]+)&ll=([^&]+)""".toRegex()
        val matchResult = regex.find(coupon)

        if (matchResult != null) {
            val (sender, receiver, amount, grid, expiry, seal, movementIntensity, lightLevel) = matchResult.destructured
            return TransferCouponEntity(
                senderBio = sender,
                receiverBio = receiver,
                amount = amount.toDouble(),
                grid = grid,
                expiry = expiry.toLong(),
                seal = seal,
                movementIntensity = movementIntensity.toFloat(),
                lightLevel = lightLevel.toFloat()
            )
        }

        throw IllegalArgumentException("Invalid Transfer Coupon format")
    }

    /**
     * Parse a Location Verification string
     */
    private fun parseLocationVerification(locationString: String): LocationVerificationEntity {
        val regex = """Mari://loc\?g=([^&]+)&s=([^&]+)&ts=([^&]+)&mi=([^&]+)""".toRegex()
        val matchResult = regex.find(locationString)

        if (matchResult != null) {
            val (grid, seal, timestamp, movementIntensity) = matchResult.destructured
            return LocationVerificationEntity(
                grid = grid,
                seal = seal,
                timestamp = timestamp.toLong(),
                movementIntensity = movementIntensity.toFloat()
            )
        }

        throw IllegalArgumentException("Invalid Location Verification format")
    }

    /**
     * Parse a Physics Challenge string
     */
    private fun parsePhysicsChallenge(challengeString: String): PhysicsChallengeEntity {
        val regex = """Mari://challenge\?mx=([^&]+)&my=([^&]+)&mz=([^&]+)&gx=([^&]+)&gy=([^&]+)&gz=([^&]+)&mx=([^&]+)&my=([^&]+)&mz=([^&]+)&s=([^&]+)""".toRegex()
        val matchResult = regex.find(challengeString)

        if (matchResult != null) {
            val (mx, my, mz, gx, gy, gz, magx, magy, magz, seal) = matchResult.destructured
            return PhysicsChallengeEntity(
                motionVector = PhysicsSensorManager.MotionVector(mx.toFloat(), my.toFloat(), mz.toFloat()),
                gyroVector = PhysicsSensorManager.GyroscopeVector(gx.toFloat(), gy.toFloat(), gz.toFloat()),
                magneticVector = PhysicsSensorManager.MagneticVector(magx.toFloat(), magy.toFloat(), magz.toFloat()),
                seal = seal
            )
        }

        throw IllegalArgumentException("Invalid Physics Challenge format")
    }

    /**
     * Validate a Transfer Coupon against current physics conditions
     */
    fun validateCoupon(coupon: TransferCouponEntity): ValidationResult {
        val errors = mutableListOf<ValidationError>()

        // Check expiry
        if (coupon.expiry > 0 && coupon.expiry < System.currentTimeMillis()) {
            errors.add(ValidationError.TIME_EXPIRED)
        }

        // Check location grid (with some tolerance for demo)
        val currentGrid = physicsSensorManager.locationGrid.value
        if (!isGridNearby(currentGrid, coupon.grid)) {
            errors.add(ValidationError.LOCATION_MISMATCH)
        }

        // Check movement intensity (should be similar)
        val currentMovement = physicsSensorManager.calculateMovementIntensity()
        if (Math.abs(currentMovement - coupon.movementIntensity) > 5.0f) {
            errors.add(ValidationError.MOVEMENT_MISMATCH)
        }

        // Check light level (should be similar)
        val currentLight = physicsSensorManager.lightLevel.value
        if (Math.abs(currentLight - coupon.lightLevel) > 100.0f) {
            errors.add(ValidationError.LIGHT_MISMATCH)
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            confidenceScore = calculateConfidenceScore(errors)
        )
    }

    /**
     * Validate location verification
     */
    fun validateLocationVerification(locationEntity: LocationVerificationEntity): ValidationResult {
        val errors = mutableListOf<ValidationError>()

        // Check location grid
        val currentGrid = physicsSensorManager.locationGrid.value
        if (!isGridNearby(currentGrid, locationEntity.grid)) {
            errors.add(ValidationError.LOCATION_MISMATCH)
        }

        // Check timestamp (should be recent)
        val timeDiff = Math.abs(System.currentTimeMillis() - locationEntity.timestamp)
        if (timeDiff > 60000) { // 1 minute
            errors.add(ValidationError.TIME_EXPIRED)
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            confidenceScore = calculateConfidenceScore(errors)
        )
    }

    /**
     * Validate physics challenge
     */
    fun validatePhysicsChallenge(challengeEntity: PhysicsChallengeEntity): ValidationResult {
        val errors = mutableListOf<ValidationError>()

        // Check if motion vectors are similar
        val currentMotion = physicsSensorManager.motionData.value
        val motionDiff = calculateVectorDifference(currentMotion, challengeEntity.motionVector)
        if (motionDiff > 2.0f) {
            errors.add(ValidationError.MOVEMENT_MISMATCH)
        }

        // Check if gyroscope vectors are similar
        val currentGyro = physicsSensorManager.gyroscopeData.value
        val gyroDiff = calculateVectorDifference(currentGyro, challengeEntity.gyroVector)
        if (gyroDiff > 1.0f) {
            errors.add(ValidationError.MOVEMENT_MISMATCH)
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            confidenceScore = calculateConfidenceScore(errors)
        )
    }

    /**
     * Calculate if two grids are nearby
     */
    private fun isGridNearby(grid1: String, grid2: String): Boolean {
        if (grid1 == grid2) return true

        // Lightweight hamming distance calculation
        var distance = 0
        val minLength = minOf(grid1.length, grid2.length)

        for (i in 0 until minLength) {
            if (grid1[i] != grid2[i]) distance++
        }

        distance += Math.abs(grid1.length - grid2.length)

        return distance <= MAX_GRID_DISTANCE
    }

    /**
     * Calculate vector difference
     */
    private fun calculateVectorDifference(vec1: Any, vec2: Any): Float {
        return when {
            vec1 is PhysicsSensorManager.MotionVector && vec2 is PhysicsSensorManager.MotionVector -> {
                Math.sqrt(
                    Math.pow((vec1.x - vec2.x).toDouble(), 2.0) +
                    Math.pow((vec1.y - vec2.y).toDouble(), 2.0) +
                    Math.pow((vec1.z - vec2.z).toDouble(), 2.0)
                ).toFloat()
            }
            vec1 is PhysicsSensorManager.GyroscopeVector && vec2 is PhysicsSensorManager.GyroscopeVector -> {
                Math.sqrt(
                    Math.pow((vec1.x - vec2.x).toDouble(), 2.0) +
                    Math.pow((vec1.y - vec2.y).toDouble(), 2.0) +
                    Math.pow((vec1.z - vec2.z).toDouble(), 2.0)
                ).toFloat()
            }
            else -> Float.MAX_VALUE
        }
    }

    /**
     * Calculate confidence score based on errors
     */
    private fun calculateConfidenceScore(errors: List<ValidationError>): Float {
        if (errors.isEmpty()) return 1.0f

        val errorImpact = errors.size * 0.2f
        return Math.max(0.0f, 1.0f - errorImpact)
    }

    /**
     * Data classes for Mari entities
     * 
     * WARNING: Variable names like "bloodHash", "senderBio", "receiverBio" are misleading!
     * They are NOT biometric data - just pseudonymous user account identifiers.
     */
    sealed class MariEntity

    data class FunctionIdEntity(
        val version: Int,
        val grid: String,
        val bloodHash: String,  // TODO: Rename to userId - NOT biometric data!
        val readyCash: Double,
        val totalMoney: Double,
        val seal: String,
        val timestamp: Long,
        val movementIntensity: Float
    ) : MariEntity()

    data class TransferCouponEntity(
        val senderBio: String,      // TODO: Rename to senderId - NOT biometric data!
        val receiverBio: String,    // TODO: Rename to receiverId - NOT biometric data!
        val amount: Double,
        val grid: String,
        val expiry: Long,
        val seal: String,
        val movementIntensity: Float,
        val lightLevel: Float
    ) : MariEntity()

    data class LocationVerificationEntity(
        val grid: String,
        val seal: String,
        val timestamp: Long,
        val movementIntensity: Float
    ) : MariEntity()

    data class PhysicsChallengeEntity(
        val motionVector: PhysicsSensorManager.MotionVector,
        val gyroVector: PhysicsSensorManager.GyroscopeVector,
        val magneticVector: PhysicsSensorManager.MagneticVector,
        val seal: String
    ) : MariEntity()

    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<ValidationError>,
        val confidenceScore: Float = 1.0f
    )

    enum class ValidationError {
        TIME_EXPIRED,
        LOCATION_MISMATCH,
        SEAL_MISMATCH,
        BLOOD_MISMATCH,
        MOVEMENT_MISMATCH,
        LIGHT_MISMATCH
    }
}
