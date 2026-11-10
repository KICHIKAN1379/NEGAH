package com.kichikan.negah.data.local.di

import com.kichikan.negah.data.local.inventory.RoomInventoryRepository
import com.kichikan.negah.domain.inventory.InventoryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class InventoryRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindInventoryRepository(implementation: RoomInventoryRepository): InventoryRepository
}
