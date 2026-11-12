package com.kichikan.negah.data.local.di

import com.kichikan.negah.data.local.settings.RoomHomeShortcutsRepository
import com.kichikan.negah.domain.settings.HomeShortcutsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class HomeShortcutsRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindHomeShortcutsRepository(
        implementation: RoomHomeShortcutsRepository,
    ): HomeShortcutsRepository
}
