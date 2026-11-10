package com.kichikan.negah.data.local.di

import android.content.Context
import androidx.room.Room
import com.kichikan.negah.data.local.NegahDatabase
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
}
