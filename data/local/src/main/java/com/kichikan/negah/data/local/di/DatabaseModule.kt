package com.kichikan.negah.data.local.di

import android.content.Context
import androidx.room.Room
import com.kichikan.negah.data.local.NegahDatabase
import com.kichikan.negah.data.local.attendance.AttendanceDao
import com.kichikan.negah.data.local.commitment.CommitmentDao
import com.kichikan.negah.data.local.economy.EconomyTransactionDao
import com.kichikan.negah.data.local.member.MemberDao
import com.kichikan.negah.data.local.session.SessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideNegahDatabase(
        @ApplicationContext context: Context,
    ): NegahDatabase = Room.databaseBuilder(
        context,
        NegahDatabase::class.java,
        "negah_coach.db",
    ).build()

    @Provides fun provideMemberDao(database: NegahDatabase): MemberDao = database.memberDao()
    @Provides fun provideSessionDao(database: NegahDatabase): SessionDao = database.sessionDao()
    @Provides fun provideAttendanceDao(database: NegahDatabase): AttendanceDao = database.attendanceDao()
    @Provides fun provideCommitmentDao(database: NegahDatabase): CommitmentDao = database.commitmentDao()
    @Provides fun provideEconomyTransactionDao(database: NegahDatabase): EconomyTransactionDao =
        database.economyTransactionDao()
}
