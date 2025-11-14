package com.Mari.mobileapp.data.di

import com.Mari.mobileapp.data.source.local.UserLocalDataSource
import com.Mari.mobileapp.data.source.local.TransactionLocalDataSource
import com.Mari.mobileapp.data.source.local.impl.UserLocalDataSourceImpl
import com.Mari.mobileapp.data.source.local.impl.TransactionLocalDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DataSourceModule {

    @Binds
    abstract fun bindUserLocalDataSource(impl: UserLocalDataSourceImpl): UserLocalDataSource

    @Binds
    abstract fun bindTransactionLocalDataSource(impl: TransactionLocalDataSourceImpl): TransactionLocalDataSource
}
