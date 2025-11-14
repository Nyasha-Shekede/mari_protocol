package com.Mari.mobile.ui.models

data class Transaction(
    val id: String,
    val type: String, // "sent" or "received"
    val amount: Double,
    val peerId: String,
    val peerName: String,
    val timestamp: String,
    val hsmId: String
)

data class MotionData(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f
)
