package com.Mari.mobileapp.data.mapper

import com.Mari.mobileapp.core.physics.PhysicsSensorManager
import com.Mari.mobileapp.core.protocol.MariProtocol
import com.Mari.mobileapp.data.model.User
import java.util.UUID
import javax.inject.Inject

class UserMapper @Inject constructor(
    private val physicsSensorManager: PhysicsSensorManager,
    private val MariProtocol: MariProtocol
) {
    fun createNewUser(): User {
        val bloodHash = physicsSensorManager.simulateVeinScan()
        val locationGrid = physicsSensorManager.locationGrid.value
        return User(
            id = UUID.randomUUID().toString(),
            bloodHash = bloodHash,
            locationGrid = locationGrid,
            readyCash = 100.0,
            totalMoney = 100.0,
            functionId = MariProtocol.generateFunctionId(bloodHash, 100.0, 100.0),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }
}
