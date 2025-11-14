package com.Mari.mobileapp.data.di

import android.content.Context
import androidx.room.Room
import com.Mari.mobileapp.data.local.MariDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideMariDatabase(@ApplicationContext context: Context): MariDatabase =
        Room.databaseBuilder(
            context,
            MariDatabase::class.java,
            MariDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideUserDao(db: MariDatabase) = db.userDao()

    @Provides
    fun provideTransactionDao(db: MariDatabase) = db.transactionDao()
}
