package com.kichikan.negah.data.local.di

import com.kichikan.negah.data.local.media.MediaAssetManager
import com.kichikan.negah.domain.member.MemberAvatarRepository
import com.kichikan.negah.domain.inventory.CatalogItemMediaRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MediaAssetRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindMemberAvatarRepository(implementation: MediaAssetManager): MemberAvatarRepository

    @Binds
    @Singleton
    abstract fun bindCatalogItemMediaRepository(implementation: MediaAssetManager): CatalogItemMediaRepository
}
