package com.kichikan.negah.data.local.inventory

import androidx.room.ColumnInfo
import com.kichikan.negah.domain.inventory.InventoryCategory
import com.kichikan.negah.domain.inventory.MemberInventoryEntry

data class InventoryCatalogRow(
    @ColumnInfo(name = "memberId") val memberId: String,
    @ColumnInfo(name = "catalogItemId") val catalogItemId: String,
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "description") val description: String?,
    @ColumnInfo(name = "acquiredAtEpochMillis") val acquiredAtEpochMillis: Long,
    @ColumnInfo(name = "sourceType") val sourceType: String,
    @ColumnInfo(name = "sourceId") val sourceId: String?,
) {
    fun toDomain() = MemberInventoryEntry(
        memberId, catalogItemId, InventoryCategory.valueOf(category), title, description,
        acquiredAtEpochMillis, sourceType, sourceId,
    )
}
