package com.kichikan.negah.data.local.di

import com.kichikan.negah.data.local.media.MediaAssetManager
import com.kichikan.negah.domain.settings.RingProfileMediaRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MemberAvatarRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindRingProfileMediaRepository(implementation: MediaAssetManager): RingProfileMediaRepository

    @Binds
    @Singleton
    abstract fun bindCatalogItemMediaRepository(implementation: MediaAssetManager): CatalogItemMediaRepository
}
