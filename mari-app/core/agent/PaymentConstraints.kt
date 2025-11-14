package com.Mari.mobileapp.core.agent
data class PaymentConstraints(
    val maxAmount: Long, // in minor units (e.g., cents)
    val merchantWhitelist: List<String> = emptyList(),
    val category: String? = null,
    val description: String? = null
)
