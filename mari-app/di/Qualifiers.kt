package com.Mari.mobile.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MockPhysicsSensor

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RealPhysicsSensor

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DevelopmentEnvironment

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ProductionEnvironment
