package com.kichikan.negah.data.local.di

import com.kichikan.negah.data.local.settings.RoomClassDaysRepository
import com.kichikan.negah.domain.settings.ClassDaysRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ClassDaysRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindClassDaysRepository(implementation: RoomClassDaysRepository): ClassDaysRepository
}
