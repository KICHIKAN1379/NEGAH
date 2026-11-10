package com.kichikan.negah.data.local.growth

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kichikan.negah.domain.growth.GrowthItem
import com.kichikan.negah.domain.growth.GrowthTrack

@Entity(tableName = "growth_items")
data class GrowthItemEntity(
    @PrimaryKey val id: String,
    val track: String,
    val title: String,
    val description: String?,
    val sortOrder: Int,
    val isActive: Boolean,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
) {
    fun toDomain() = GrowthItem(
        id, GrowthTrack.valueOf(track), title, description, sortOrder,
        isActive, createdAtEpochMillis, updatedAtEpochMillis,
    )

    companion object {
        fun fromDomain(item: GrowthItem) = GrowthItemEntity(
            item.id, item.track.name, item.title, item.description, item.sortOrder,
            item.isActive, item.createdAtEpochMillis, item.updatedAtEpochMillis,
        )
    }
}
