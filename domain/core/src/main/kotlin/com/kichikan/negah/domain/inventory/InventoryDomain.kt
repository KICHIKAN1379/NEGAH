package com.kichikan.negah.domain.inventory

import kotlinx.coroutines.flow.Flow

enum class InventoryCategory { AVATAR, FRAME, REWARD }

data class CatalogItem(
    val id: String,
    val category: InventoryCategory,
    val title: String,
    val description: String?,
    val isAvailable: Boolean,
    val sortOrder: Int,
)

data class MemberInventoryEntry(
    val memberId: String,
    val catalogItemId: String,
    val category: InventoryCategory,
    val title: String,
    val description: String?,
    val acquiredAtEpochMillis: Long,
    val sourceType: String,
    val sourceId: String?,
)

data class ActiveInventoryItem(
    val memberId: String,
    val category: InventoryCategory,
    val catalogItemId: String,
    val activatedAtEpochMillis: Long,
)

interface InventoryRepository {
    fun observeCatalog(category: InventoryCategory): Flow<List<CatalogItem>>
    fun observeMemberInventory(memberId: String, category: InventoryCategory): Flow<List<MemberInventoryEntry>>
    fun observeActiveItems(memberId: String): Flow<List<ActiveInventoryItem>>
    suspend fun saveCatalogItem(item: CatalogItem)
    suspend fun grantItem(entry: MemberInventoryEntry)
    suspend fun activateItem(memberId: String, category: InventoryCategory, catalogItemId: String, nowEpochMillis: Long): Boolean
}
