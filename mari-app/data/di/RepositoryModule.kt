package com.Mari.mobileapp.data.di

import com.Mari.mobileapp.data.repository.UserRepository
import com.Mari.mobileapp.data.repository.TransactionRepository
import com.Mari.mobileapp.data.repository.impl.UserRepositoryImpl
import com.Mari.mobileapp.data.repository.impl.TransactionRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository
}
