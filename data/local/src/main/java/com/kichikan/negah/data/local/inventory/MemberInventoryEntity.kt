package com.kichikan.negah.data.local.inventory

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.kichikan.negah.data.local.member.MemberEntity

@Entity(
    tableName = "member_inventory",
    primaryKeys = ["memberId", "catalogItemId"],
    foreignKeys = [
        ForeignKey(entity = MemberEntity::class, parentColumns = ["id"], childColumns = ["memberId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = CatalogItemEntity::class, parentColumns = ["id"], childColumns = ["catalogItemId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("catalogItemId")],
)
data class MemberInventoryEntity(
    val memberId: String,
    val catalogItemId: String,
    val acquiredAtEpochMillis: Long,
    val sourceType: String,
    val sourceId: String?,
)
