package com.Mari.mobile.di

import android.content.Context
import android.hardware.SensorManager
import com.Mari.mobileapp.core.physics.PhysicsSensorManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PhysicsModule {
    @Provides
    @Singleton
    fun providePhysicsSensorManager(
        context: Context
    ): PhysicsSensorManager {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        return PhysicsSensorManager(context, sm)
    }
}
