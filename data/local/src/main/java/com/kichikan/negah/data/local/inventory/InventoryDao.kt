package com.kichikan.negah.data.local.inventory

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.kichikan.negah.domain.inventory.InventoryCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {
    @Query("SELECT * FROM catalog_items WHERE category = :category AND isAvailable = 1 ORDER BY sortOrder, title COLLATE NOCASE")
    fun observeCatalog(category: String): Flow<List<CatalogItemEntity>>

    @Query("SELECT * FROM catalog_items WHERE category = :category ORDER BY sortOrder, title COLLATE NOCASE")
    fun observeAllCatalog(category: String): Flow<List<CatalogItemEntity>>

    @Query("""
        SELECT mi.memberId AS memberId, mi.catalogItemId AS catalogItemId,
               ci.category AS category, ci.title AS title, ci.description AS description,
               mi.acquiredAtEpochMillis AS acquiredAtEpochMillis,
               mi.sourceType AS sourceType, mi.sourceId AS sourceId
        FROM member_inventory mi
        INNER JOIN catalog_items ci ON ci.id = mi.catalogItemId
        WHERE mi.memberId = :memberId AND ci.category = :category
        ORDER BY mi.acquiredAtEpochMillis DESC, ci.title COLLATE NOCASE
    """)
    fun observeMemberInventory(memberId: String, category: String): Flow<List<InventoryCatalogRow>>

    @Query("SELECT * FROM member_active_items WHERE memberId = :memberId ORDER BY category")
    fun observeActiveItems(memberId: String): Flow<List<MemberActiveItemEntity>>

    @Upsert
    suspend fun upsertCatalogItem(item: CatalogItemEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertInventoryEntry(entry: MemberInventoryEntity)

    @Query("""
        SELECT COUNT(*) FROM member_inventory mi
        INNER JOIN catalog_items ci ON ci.id = mi.catalogItemId
        WHERE mi.memberId = :memberId AND mi.catalogItemId = :catalogItemId
          AND ci.category = :category
    """)
    suspend fun ownedItemCount(memberId: String, catalogItemId: String, category: String): Int

    @Upsert
    suspend fun upsertActiveItem(item: MemberActiveItemEntity)

    @Transaction
    suspend fun activateOwnedItem(
        memberId: String,
        category: InventoryCategory,
        catalogItemId: String,
        nowEpochMillis: Long,
    ): Boolean {
        if (ownedItemCount(memberId, catalogItemId, category.name) == 0) return false
        upsertActiveItem(MemberActiveItemEntity(memberId, category.name, catalogItemId, nowEpochMillis))
        return true
    }
}
