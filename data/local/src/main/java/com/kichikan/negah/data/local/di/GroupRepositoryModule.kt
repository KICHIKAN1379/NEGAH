package com.kichikan.negah.data.local.di

import com.kichikan.negah.data.local.group.RoomGroupRepository
import com.kichikan.negah.domain.group.GroupRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class GroupRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindGroupRepository(implementation: RoomGroupRepository): GroupRepository
}
