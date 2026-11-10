package com.kichikan.negah.data.local.inventory
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kichikan.negah.domain.inventory.CatalogItem
import com.kichikan.negah.domain.inventory.InventoryCategory
@Entity(tableName = "catalog_items")
data class CatalogItemEntity(@PrimaryKey val id: String, val category: String, val title: String, val description: String?, val isAvailable: Boolean, val sortOrder: Int, val priceAmount: Long?) {
 fun toDomain() = CatalogItem(id, InventoryCategory.valueOf(category), title, description, isAvailable, sortOrder, priceAmount)
 companion object {
  fun fromDomain(item: CatalogItem) = CatalogItemEntity(item.id, item.category.name, item.title, item.description, item.isAvailable, item.sortOrder, item.priceAmount)
 }
}
