package com.kichikan.negah.data.local.di

import com.kichikan.negah.data.local.economy.RoomEconomyRepository
import com.kichikan.negah.domain.economy.EconomyRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class EconomyRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindEconomyRepository(implementation: RoomEconomyRepository): EconomyRepository
}
