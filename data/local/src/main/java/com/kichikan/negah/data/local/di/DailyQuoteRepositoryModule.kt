package com.kichikan.negah.data.local.di

import com.kichikan.negah.data.local.settings.RoomDailyQuoteRepository
import com.kichikan.negah.domain.settings.DailyQuoteRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DailyQuoteRepositoryModule {
    @Binds @Singleton
    abstract fun bindDailyQuoteRepository(implementation: RoomDailyQuoteRepository): DailyQuoteRepository
}