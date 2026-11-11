package com.kichikan.negah.data.local.di

import com.kichikan.negah.data.local.media.MediaAssetManager
import com.kichikan.negah.domain.member.MemberAvatarRepository
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
    abstract fun bindMemberAvatarRepository(implementation: MediaAssetManager): MemberAvatarRepository
}
