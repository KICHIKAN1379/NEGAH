package com.kichikan.negah.data.local
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kichikan.negah.data.local.attendance.*
import com.kichikan.negah.data.local.commitment.*
import com.kichikan.negah.data.local.economy.*
import com.kichikan.negah.data.local.growth.*
import com.kichikan.negah.data.local.member.*
import com.kichikan.negah.data.local.inventory.*
import com.kichikan.negah.data.local.session.*
import com.kichikan.negah.data.local.settings.ClassDayEntity
import com.kichikan.negah.data.local.settings.ClassDaysDao
import com.kichikan.negah.data.local.settings.HomeShortcutEntity
import com.kichikan.negah.data.local.settings.HomeShortcutSettingsEntity
import com.kichikan.negah.data.local.settings.HomeShortcutsDao
import com.kichikan.negah.data.local.media.MediaAssetEntity
import com.kichikan.negah.data.local.media.MediaAssetDao
import com.kichikan.negah.data.local.settings.RingProfileEntity
import com.kichikan.negah.data.local.settings.RingProfileDao
import com.kichikan.negah.data.local.settings.DailyQuoteEntity
import com.kichikan.negah.data.local.settings.DailyQuoteDao
import com.kichikan.negah.data.local.mission.MissionEntity
import com.kichikan.negah.data.local.mission.MissionAssignmentEntity
import com.kichikan.negah.data.local.mission.MissionDao
import com.kichikan.negah.data.local.reading.BookReadingDao
import com.kichikan.negah.data.local.reading.BookReadingEntity

@Database(entities = [MemberEntity::class, SessionEntity::class, AttendanceEntity::class, AttendanceRevisionEntity::class, CommitmentEntity::class, CommitmentRevisionEntity::class, EconomyTransactionEntity::class, GrowthItemEntity::class, MemberGrowthProgressEntity::class, CatalogItemEntity::class, MemberInventoryEntity::class, MemberActiveItemEntity::class, ClassDayEntity::class, HomeShortcutEntity::class, HomeShortcutSettingsEntity::class, MediaAssetEntity::class, RingProfileEntity::class, MissionEntity::class, MissionAssignmentEntity::class, DailyQuoteEntity::class, BookReadingEntity::class, MemberGrowthHistoryEntity::class], version = 14, exportSchema = true)
@TypeConverters(AttendanceStatusConverter::class)
abstract class NegahDatabase : RoomDatabase() {
 abstract fun memberDao(): MemberDao
 abstract fun sessionDao(): SessionDao
 abstract fun attendanceDao(): AttendanceDao
 abstract fun commitmentDao(): CommitmentDao
 abstract fun economyTransactionDao(): EconomyTransactionDao
 abstract fun growthMapDao(): GrowthMapDao
 abstract fun inventoryDao(): InventoryDao
 abstract fun shopDao(): ShopDao
 abstract fun classDaysDao(): ClassDaysDao
 abstract fun homeShortcutsDao(): HomeShortcutsDao
 abstract fun mediaAssetDao(): MediaAssetDao
 abstract fun ringProfileDao(): RingProfileDao
 abstract fun missionDao(): MissionDao
 abstract fun dailyQuoteDao(): DailyQuoteDao
 abstract fun bookReadingDao(): BookReadingDao
}
