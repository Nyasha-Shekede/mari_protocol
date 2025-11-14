package com.Mari.mobile.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val bloodHash: String,
    val locationGrid: String,
    val readyCash: Double,
    val totalMoney: Double,
    val createdAt: Long
)
