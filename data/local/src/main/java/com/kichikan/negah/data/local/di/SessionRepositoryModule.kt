package com.kichikan.negah.data.local.di

import com.kichikan.negah.data.local.session.RoomSessionRepository
import com.kichikan.negah.domain.session.SessionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SessionRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindSessionRepository(implementation: RoomSessionRepository): SessionRepository
}
