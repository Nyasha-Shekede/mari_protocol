package com.Mari.mobileapp.service.core

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthStore @Inject constructor() {
    @Volatile var token: String? = null
        private set
    @Volatile var userId: String? = null
        private set

    fun setAuth(token: String, userId: String) {
        this.token = token
        this.userId = userId
    }

    fun clear() {
        token = null
        userId = null
    }
}
