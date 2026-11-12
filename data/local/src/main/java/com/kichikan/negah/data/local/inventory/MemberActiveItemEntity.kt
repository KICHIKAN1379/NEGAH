package com.kichikan.negah.data.local.inventory

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "member_active_items",
    primaryKeys = ["memberId", "category"],
    foreignKeys = [
        ForeignKey(
            entity = MemberInventoryEntity::class,
            parentColumns = ["memberId", "catalogItemId"],
            childColumns = ["memberId", "catalogItemId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["memberId", "catalogItemId"])],
)
data class MemberActiveItemEntity(
    val memberId: String,
    val category: String,
    val catalogItemId: String,
    val activatedAtEpochMillis: Long,
)
