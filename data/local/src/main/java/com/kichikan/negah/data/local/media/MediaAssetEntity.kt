package com.kichikan.negah.data.local.media

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class MediaAssetOwnerType { MEMBER, RING, CATALOG_ITEM }

enum class MediaAssetRole { AVATAR, PROFILE_IMAGE, RING_IMAGE, RING_BANNER, CATALOG_IMAGE }

@Entity(
    tableName = "media_assets",
    indices = [Index(value = ["ownerType", "ownerId", "role"], unique = true)],
)
data class MediaAssetEntity(
    @PrimaryKey val id: String,
    val ownerType: String,
    val ownerId: String,
    val role: String,
    val relativePath: String,
    val mimeType: String,
    val byteSize: Long,
    val sha256: String,
    val createdAtEpochMillis: Long,
)
