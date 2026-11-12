package com.kichikan.negah.data.local.di

import com.kichikan.negah.data.local.settings.RoomRingProfileRepository
import com.kichikan.negah.domain.settings.RingProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RingProfileRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindRingProfileRepository(implementation: RoomRingProfileRepository): RingProfileRepository
}
