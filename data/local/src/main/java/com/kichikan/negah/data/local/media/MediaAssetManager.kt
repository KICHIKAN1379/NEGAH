package com.kichikan.negah.data.local.media

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.io.InputStream
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

class MediaAssetManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaAssetDao: MediaAssetDao,
) {
    private val fileStore by lazy {
        MediaAssetFileStore(context.filesDir.resolve("media-assets"))
    }

    suspend fun importImage(
        uri: Uri,
        ownerType: MediaAssetOwnerType,
        ownerId: String,
        role: MediaAssetRole,
    ): MediaAssetEntity {
        require(ownerId.isNotBlank()) { "An image owner ID is required" }
        val mimeType = context.contentResolver.getType(uri)
            ?.substringBefore(';')
            ?.lowercase(Locale.ROOT)
            ?: throw IllegalArgumentException("The selected file has no supported image type")

        val input = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Unable to read the selected image")
        val stored = fileStore.write(input, mimeType)
        val asset = MediaAssetEntity(
            id = UUID.randomUUID().toString(),
            ownerType = ownerType.name,
            ownerId = ownerId,
            role = role.name,
            relativePath = stored.relativePath,
            mimeType = stored.mimeType,
            byteSize = stored.byteSize,
            sha256 = stored.sha256,
            createdAtEpochMillis = System.currentTimeMillis(),
        )

        val previous = try {
            mediaAssetDao.replaceForOwner(asset)
        } catch (failure: Exception) {
            fileStore.delete(stored.relativePath)
            throw failure
        }
        previous?.let { fileStore.delete(it.relativePath) }
        return asset
    }

    suspend fun openImage(assetId: String): InputStream? {
        val asset = mediaAssetDao.findById(assetId) ?: return null
        return fileStore.open(asset.relativePath)
    }

    suspend fun removeImage(
        ownerType: MediaAssetOwnerType,
        ownerId: String,
        role: MediaAssetRole,
    ): Boolean {
        val removed = mediaAssetDao.removeForOwner(ownerType.name, ownerId, role.name)
            ?: return false
        fileStore.delete(removed.relativePath)
        return true
    }
}
