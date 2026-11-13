package com.kichikan.negah.data.local.di

import com.kichikan.negah.data.local.reading.RoomReadingRepository
import com.kichikan.negah.domain.reading.ReadingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ReadingRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindReadingRepository(implementation: RoomReadingRepository): ReadingRepository
}
