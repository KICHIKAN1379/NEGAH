package com.kichikan.negah.data.local.inventory
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.kichikan.negah.data.local.economy.EconomyTransactionEntity
import com.kichikan.negah.domain.inventory.InventoryCategory
import com.kichikan.negah.domain.inventory.ShopPurchaseResult
@Dao
abstract class ShopDao {
 @Query("SELECT priceAmount FROM catalog_items WHERE id = :itemId AND category = :category AND isAvailable = 1")
 protected abstract suspend fun availableItemPrice(itemId: String, category: String): Long?
 @Query("SELECT COUNT(*) FROM catalog_items WHERE id = :itemId AND category = :category AND isAvailable = 1")
 protected abstract suspend fun availableItemCount(itemId: String, category: String): Int
 @Query("SELECT COUNT(*) FROM member_inventory WHERE memberId = :memberId AND catalogItemId = :itemId")
 protected abstract suspend fun ownedCount(memberId: String, itemId: String): Int
 @Query("SELECT COALESCE(SUM(amount), 0) FROM economy_transactions WHERE memberId = :memberId AND currency = :currency")
 protected abstract suspend fun balance(memberId: String, currency: String): Long
 @Insert protected abstract suspend fun insertTransaction(transaction: EconomyTransactionEntity)
 @Insert protected abstract suspend fun insertInventory(entry: MemberInventoryEntity)
 @Query("""UPDATE members SET totalXp = (SELECT COALESCE(SUM(amount), 0) FROM economy_transactions WHERE memberId = :memberId AND currency = 'XP'), spendablePoints = (SELECT COALESCE(SUM(amount), 0) FROM economy_transactions WHERE memberId = :memberId AND currency = 'POINTS'), diamonds = (SELECT COALESCE(SUM(amount), 0) FROM economy_transactions WHERE memberId = :memberId AND currency = 'DIAMONDS'), updatedAtEpochMillis = :now WHERE id = :memberId""")
 protected abstract suspend fun refreshBalances(memberId: String, now: Long)
 @Transaction
 open suspend fun purchase(memberId: String, category: InventoryCategory, itemId: String, transactionId: String, now: Long): ShopPurchaseResult {
  if (availableItemCount(itemId, category.name) == 0) return ShopPurchaseResult.ITEM_UNAVAILABLE
  val price = availableItemPrice(itemId, category.name) ?: return ShopPurchaseResult.PRICE_NOT_SET
  if (price <= 0L) return ShopPurchaseResult.PRICE_NOT_SET
  if (ownedCount(memberId, itemId) > 0) return ShopPurchaseResult.ALREADY_OWNED
  val currency = if (category == InventoryCategory.REWARD) "POINTS" else "DIAMONDS"
  if (balance(memberId, currency) < price) return ShopPurchaseResult.INSUFFICIENT_BALANCE
  insertTransaction(EconomyTransactionEntity(transactionId, memberId, currency, -price, "SPEND", "STORE_PURCHASE", itemId, now))
  insertInventory(MemberInventoryEntity(memberId, itemId, now, "PURCHASE", transactionId))
  refreshBalances(memberId, now)
  return ShopPurchaseResult.PURCHASED
 }
}
