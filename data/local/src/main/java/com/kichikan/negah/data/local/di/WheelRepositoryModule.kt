package com.kichikan.negah.data.local.di

import com.kichikan.negah.data.local.wheel.RoomWheelRepository
import com.kichikan.negah.domain.wheel.WheelRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WheelRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindWheelRepository(implementation: RoomWheelRepository): WheelRepository
}
