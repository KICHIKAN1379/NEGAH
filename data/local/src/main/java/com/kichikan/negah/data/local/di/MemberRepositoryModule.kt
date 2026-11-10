package com.kichikan.negah.data.local.di

import com.kichikan.negah.data.local.member.RoomMemberRepository
import com.kichikan.negah.domain.member.MemberRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MemberRepositoryModule {
    @Binds @Singleton
    abstract fun bindMemberRepository(implementation: RoomMemberRepository): MemberRepository
}
