package com.Mari.mobileapp.data.model

data class User(
    val id: String,
    val bloodHash: String,
    val locationGrid: String,
    val readyCash: Double,
    val totalMoney: Double,
    val functionId: String,
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object {
        fun fromEntity(entity: com.Mari.mobileapp.data.local.entity.UserEntity): User =
            User(
                id = entity.id,
                bloodHash = entity.bloodHash,
                locationGrid = entity.locationGrid,
                readyCash = entity.readyCash,
                totalMoney = entity.totalMoney,
                functionId = entity.functionId,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )

        fun toEntity(user: User): com.Mari.mobileapp.data.local.entity.UserEntity =
            com.Mari.mobileapp.data.local.entity.UserEntity(
                id = user.id,
                bloodHash = user.bloodHash,
                locationGrid = user.locationGrid,
                readyCash = user.readyCash,
                totalMoney = user.totalMoney,
                functionId = user.functionId,
                createdAt = user.createdAt,
                updatedAt = user.updatedAt
            )
    }
}
