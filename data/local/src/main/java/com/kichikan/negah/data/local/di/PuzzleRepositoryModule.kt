package com.kichikan.negah.data.local.di

import com.kichikan.negah.data.local.mission.RoomPuzzleRepository
import com.kichikan.negah.domain.mission.PuzzleRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PuzzleRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindPuzzleRepository(implementation: RoomPuzzleRepository): PuzzleRepository
}
