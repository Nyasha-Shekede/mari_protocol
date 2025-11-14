package com.Mari.mobile.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
object ViewModelModule {
    // Intentionally empty: rely on @HiltViewModel constructors with @Inject for ViewModels.
}
