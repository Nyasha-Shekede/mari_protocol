package com.Mari.mobileapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "sender_bio_hash") val senderBioHash: String,
    @ColumnInfo(name = "receiver_bio_hash") val receiverBioHash: String,
    @ColumnInfo(name = "amount") val amount: Double,
    @ColumnInfo(name = "location_grid") val locationGrid: String,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "coupon") val coupon: String,
    @ColumnInfo(name = "transport_method") val transportMethod: String,
    @ColumnInfo(name = "coupon_hash") val couponHash: String? = null
)
