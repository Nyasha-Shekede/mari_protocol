package com.Mari.mobile.di

import com.Mari.mobile.BuildConfig
import com.Mari.mobileapp.service.core.CoreApi
import com.Mari.mobileapp.service.core.CoreGateway
import com.Mari.mobileapp.service.core.CoreGatewayImpl
import com.Mari.mobileapp.service.core.AuthStore
import com.Mari.mobileapp.service.bank.BankApi
import com.Mari.mobileapp.service.bank.BankGateway
import com.Mari.mobileapp.service.bank.BankGatewayImpl
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.Protocol
import java.util.concurrent.TimeUnit
import retrofit2.Retrofit
import javax.inject.Named
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .protocols(listOf(Protocol.HTTP_1_1))
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder().build()

    @Provides
    @Singleton
    @Named("core")
    fun provideCoreRetrofit(client: OkHttpClient, moshi: Moshi): Retrofit {
        // On Android emulator, localhost of the host is 10.0.2.2. BuildConfig.CORE_BASE_URL is set in build.gradle.kts
        return Retrofit.Builder()
            .baseUrl(normalizeBaseUrl(BuildConfig.CORE_BASE_URL))
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun provideCoreApi(@Named("core") retrofit: Retrofit): CoreApi = retrofit.create(CoreApi::class.java)

    @Provides
    @Singleton
    fun provideCoreGateway(api: CoreApi, auth: AuthStore): CoreGateway =
        CoreGatewayImpl(api, auth)

    // --- Bank (Mock HSM) ---
    @Provides
    @Singleton
    @Named("bank")
    fun provideBankRetrofit(client: OkHttpClient, moshi: Moshi): Retrofit {
        return Retrofit.Builder()
            .baseUrl(normalizeBaseUrl(BuildConfig.BANK_BASE_URL))
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun provideBankApi(@Named("bank") retrofit: Retrofit): BankApi =
        retrofit.create(BankApi::class.java)

    @Provides
    @Singleton
    fun provideBankGateway(api: BankApi): BankGateway = BankGatewayImpl(api)

    private fun normalizeBaseUrl(url: String): String =
        if (url.endsWith("/")) url else "$url/"
}
