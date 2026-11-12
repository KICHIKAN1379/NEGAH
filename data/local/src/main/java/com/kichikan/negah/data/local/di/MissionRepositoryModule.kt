package com.kichikan.negah.data.local.di

import com.kichikan.negah.data.local.mission.RoomMissionRepository
import com.kichikan.negah.domain.mission.MissionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MissionRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindMissionRepository(implementation: RoomMissionRepository): MissionRepository
}
