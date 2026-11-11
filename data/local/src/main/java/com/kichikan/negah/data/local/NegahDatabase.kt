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
@Database(entities = [MemberEntity::class, SessionEntity::class, AttendanceEntity::class, AttendanceRevisionEntity::class, CommitmentEntity::class, CommitmentRevisionEntity::class, EconomyTransactionEntity::class, GrowthItemEntity::class, MemberGrowthProgressEntity::class, CatalogItemEntity::class, MemberInventoryEntity::class, MemberActiveItemEntity::class, ClassDayEntity::class, HomeShortcutEntity::class, HomeShortcutSettingsEntity::class], version = 8, exportSchema = true)
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
}
