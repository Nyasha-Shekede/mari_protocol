package com.Mari.mobileapp.domain.usecase

import com.Mari.mobileapp.core.transaction.TransactionManager
import javax.inject.Inject

class ValidateCouponUseCase @Inject constructor(
    private val transactionManager: TransactionManager
) {
    operator fun invoke(coupon: String): Boolean {
        return try {
            transactionManager.parseCoupon(coupon)
            true
        } catch (e: Exception) {
            false
        }
    }
}
