package com.Mari.mobileapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "blood_hash") val bloodHash: String,
    @ColumnInfo(name = "location_grid") val locationGrid: String,
    @ColumnInfo(name = "ready_cash") val readyCash: Double,
    @ColumnInfo(name = "total_money") val totalMoney: Double,
    @ColumnInfo(name = "function_id") val functionId: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)
