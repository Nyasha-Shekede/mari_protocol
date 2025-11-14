package com.Mari.mobileapp.domain.model

data class User(
    val id: String,
    val bloodHash: String,
    val locationGrid: String,
    val readyCash: Double,
    val totalMoney: Double,
    val functionId: String,
    val createdAt: Long,
    val updatedAt: Long
)
