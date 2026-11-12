package com.kichikan.negah.data.local.di

import com.kichikan.negah.data.local.attendance.RoomAttendanceRepository
import com.kichikan.negah.domain.attendance.AttendanceRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AttendanceRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindAttendanceRepository(implementation: RoomAttendanceRepository): AttendanceRepository
}
