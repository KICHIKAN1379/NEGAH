package com.kichikan.negah.data.local.di

import com.kichikan.negah.data.local.growth.RoomGrowthMapRepository
import com.kichikan.negah.domain.growth.GrowthMapRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class GrowthMapRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindGrowthMapRepository(implementation: RoomGrowthMapRepository): GrowthMapRepository
}
