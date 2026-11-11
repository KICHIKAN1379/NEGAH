package com.kichikan.negah.data.local.media

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface MediaAssetDao {
    @Query("SELECT * FROM media_assets WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): MediaAssetEntity?

    @Query("SELECT * FROM media_assets WHERE ownerType = :ownerType AND ownerId = :ownerId AND role = :role LIMIT 1")
    suspend fun findByOwner(ownerType: String, ownerId: String, role: String): MediaAssetEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(asset: MediaAssetEntity)

    @Query("DELETE FROM media_assets WHERE id = :id")
    suspend fun deleteById(id: String)

    @Transaction
    suspend fun replaceForOwner(asset: MediaAssetEntity): MediaAssetEntity? {
        val previous = findByOwner(asset.ownerType, asset.ownerId, asset.role)
        if (previous != null) deleteById(previous.id)
        insert(asset)
        return previous
    }

    @Transaction
    suspend fun removeForOwner(ownerType: String, ownerId: String, role: String): MediaAssetEntity? {
        val previous = findByOwner(ownerType, ownerId, role) ?: return null
        deleteById(previous.id)
        return previous
    }
}
