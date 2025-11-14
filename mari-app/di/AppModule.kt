package com.Mari.mobile.di

import android.app.Application
import android.content.Context
import android.hardware.SensorManager
import com.Mari.mobile.BuildConfig
import com.Mari.mobileapp.core.crypto.MariCryptoManager
import com.Mari.mobileapp.core.network.NetworkManager
import com.Mari.mobileapp.core.physics.PhysicsSensorManager
import com.Mari.mobileapp.core.protocol.MariProtocol
import com.Mari.mobileapp.core.sms.SmsManager
import com.Mari.mobileapp.core.transaction.TransactionManager
import com.Mari.mobileapp.core.transaction.TransactionManagerImpl
import com.Mari.mobileapp.service.core.CoreGateway
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // Context
    @Provides
    @Singleton
    fun provideContext(application: Application): Context {
        return application.applicationContext
    }

    // PhysicsSensorManager is provided in PhysicsModule

    // Mari Crypto Manager
    @Provides
    @Singleton
    fun provideMariCryptoManager(
        physicsSensorManager: PhysicsSensorManager
    ): MariCryptoManager = MariCryptoManager(physicsSensorManager)

    // Mari Protocol (String Generator)
    @Provides
    @Singleton
    fun provideMariProtocol(
        cryptoManager: MariCryptoManager,
        physicsSensorManager: PhysicsSensorManager
    ): MariProtocol = MariProtocol(cryptoManager, physicsSensorManager)

    // SMS Manager
    @Provides
    @Singleton
    fun provideSmsManager(
        context: Context,
        cryptoManager: MariCryptoManager
    ): SmsManager = SmsManager(context, cryptoManager)

    // Network Manager
    @Provides
    @Singleton
    fun provideNetworkManager(context: Context): NetworkManager =
        NetworkManager(context, enableWifiP2p = !BuildConfig.DEV_OFFLINE)

    // Transaction Manager
    @Provides
    @Singleton
    fun provideTransactionManager(
        context: Context,
        MariProtocol: MariProtocol,
        smsManager: SmsManager,
        networkManager: NetworkManager,
        physicsSensorManager: PhysicsSensorManager,
        coreGateway: CoreGateway
    ): TransactionManager = TransactionManagerImpl(
        context,
        MariProtocol,
        smsManager,
        physicsSensorManager,
        networkManager,
        coreGateway
    )

    // Database and repositories are provided in data/di modules

    // Coroutine Dispatchers
    @Provides
    fun provideDispatcherProvider(): DispatcherProvider {
        return DefaultDispatcherProvider()
    }
}
