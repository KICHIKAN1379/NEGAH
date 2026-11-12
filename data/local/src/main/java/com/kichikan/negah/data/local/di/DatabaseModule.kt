package com.kichikan.negah.data.local.di
import android.content.Context
import androidx.room.Room
import com.kichikan.negah.data.local.*
import com.kichikan.negah.data.local.attendance.AttendanceDao
import com.kichikan.negah.data.local.commitment.CommitmentDao
import com.kichikan.negah.data.local.economy.EconomyTransactionDao
import com.kichikan.negah.data.local.growth.GrowthMapDao
import com.kichikan.negah.data.local.member.MemberDao
import com.kichikan.negah.data.local.inventory.InventoryDao
import com.kichikan.negah.data.local.inventory.ShopDao
import com.kichikan.negah.data.local.session.SessionDao
import com.kichikan.negah.data.local.settings.ClassDaysDao
import com.kichikan.negah.data.local.settings.HomeShortcutsDao
import com.kichikan.negah.data.local.settings.DailyQuoteDao
import com.kichikan.negah.data.local.media.MediaAssetDao
import com.kichikan.negah.data.local.settings.RingProfileDao
import com.kichikan.negah.data.local.mission.MissionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
 @Provides @Singleton fun provideNegahDatabase(@ApplicationContext context: Context): NegahDatabase =
  Room.databaseBuilder(context, NegahDatabase::class.java, "negah_coach.db")
   .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13).build()
 @Provides fun provideMemberDao(database: NegahDatabase): MemberDao = database.memberDao()
 @Provides fun provideSessionDao(database: NegahDatabase): SessionDao = database.sessionDao()
 @Provides fun provideAttendanceDao(database: NegahDatabase): AttendanceDao = database.attendanceDao()
 @Provides fun provideCommitmentDao(database: NegahDatabase): CommitmentDao = database.commitmentDao()
 @Provides fun provideEconomyTransactionDao(database: NegahDatabase): EconomyTransactionDao = database.economyTransactionDao()
 @Provides fun provideGrowthMapDao(database: NegahDatabase): GrowthMapDao = database.growthMapDao()
 @Provides fun provideInventoryDao(database: NegahDatabase): InventoryDao = database.inventoryDao()
 @Provides fun provideShopDao(database: NegahDatabase): ShopDao = database.shopDao()
 @Provides fun provideClassDaysDao(database: NegahDatabase): ClassDaysDao = database.classDaysDao()
 @Provides fun provideHomeShortcutsDao(database: NegahDatabase): HomeShortcutsDao = database.homeShortcutsDao()
 @Provides fun provideMediaAssetDao(database: NegahDatabase): MediaAssetDao = database.mediaAssetDao()
 @Provides fun provideRingProfileDao(database: NegahDatabase): RingProfileDao = database.ringProfileDao()
 @Provides fun provideMissionDao(database: NegahDatabase): MissionDao = database.missionDao()
 @Provides fun provideDailyQuoteDao(database: NegahDatabase): DailyQuoteDao = database.dailyQuoteDao()
}
