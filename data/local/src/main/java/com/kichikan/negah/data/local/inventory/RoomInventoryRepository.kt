package com.kichikan.negah.data.local.inventory
import com.kichikan.negah.domain.inventory.*
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
class RoomInventoryRepository @Inject constructor(private val dao: InventoryDao, private val shopDao: ShopDao) : InventoryRepository {
 override fun observeCatalog(category: InventoryCategory): Flow<List<CatalogItem>> = dao.observeCatalog(category.name).map { rows -> rows.map(CatalogItemEntity::toDomain) }
 override fun observeAllCatalog(category: InventoryCategory): Flow<List<CatalogItem>> = dao.observeAllCatalog(category.name).map { rows -> rows.map(CatalogItemEntity::toDomain) }
 override fun observeMemberInventory(memberId: String, category: InventoryCategory): Flow<List<MemberInventoryEntry>> = dao.observeMemberInventory(memberId, category.name).map { rows -> rows.map(InventoryCatalogRow::toDomain) }
 override fun observeActiveItems(memberId: String): Flow<List<ActiveInventoryItem>> = dao.observeActiveItems(memberId).map { rows -> rows.map { ActiveInventoryItem(it.memberId, InventoryCategory.valueOf(it.category), it.catalogItemId, it.activatedAtEpochMillis) } }
 override suspend fun saveCatalogItem(item: CatalogItem) = dao.upsertCatalogItem(CatalogItemEntity.fromDomain(item))
 override suspend fun grantItem(entry: MemberInventoryEntry) {
  val granted = dao.grantAvailableItem(MemberInventoryEntity(entry.memberId, entry.catalogItemId, entry.acquiredAtEpochMillis, entry.sourceType, entry.sourceId), entry.category)
  check(granted) { "Catalog item is unavailable or its category does not match" }
 }
 override suspend fun activateItem(memberId: String, category: InventoryCategory, catalogItemId: String, nowEpochMillis: Long): Boolean = dao.activateOwnedItem(memberId, category, catalogItemId, nowEpochMillis)
 override suspend fun purchaseItem(memberId: String, category: InventoryCategory, catalogItemId: String, transactionId: String, nowEpochMillis: Long): ShopPurchaseResult = shopDao.purchase(memberId, category, catalogItemId, transactionId, nowEpochMillis)
}
