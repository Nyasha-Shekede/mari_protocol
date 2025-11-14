package com.Mari.mobile.di

import com.Mari.mobileapp.domain.repository.TransactionRepository
import com.Mari.mobileapp.domain.repository.UserRepository
import com.Mari.mobileapp.data.repository.adapter.DomainTransactionRepositoryImpl
import com.Mari.mobileapp.data.repository.adapter.DomainUserRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(impl: DomainTransactionRepositoryImpl): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: DomainUserRepositoryImpl): UserRepository
}
