package com.kichikan.negah.data.local.media

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.io.InputStream
import com.kichikan.negah.domain.member.MemberAvatarRepository
import com.kichikan.negah.domain.settings.RingProfileMediaRepository
import com.kichikan.negah.domain.inventory.CatalogItemMediaRepository
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

class MediaAssetManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaAssetDao: MediaAssetDao,
) : MemberAvatarRepository, RingProfileMediaRepository, CatalogItemMediaRepository {
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


    override suspend fun saveAvatar(memberId: String, input: InputStream, mimeType: String) {
        require(memberId.isNotBlank()) { "A member ID is required" }
        val stored = fileStore.write(input, mimeType)
        val asset = MediaAssetEntity(
            id = UUID.randomUUID().toString(),
            ownerType = MediaAssetOwnerType.MEMBER.name,
            ownerId = memberId,
            role = MediaAssetRole.AVATAR.name,
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
    }



    override suspend fun saveCatalogImage(itemId: String, input: InputStream, mimeType: String) {
        require(itemId.isNotBlank()) { "A catalog item ID is required" }
        val stored = fileStore.write(input, mimeType)
        val asset = MediaAssetEntity(
            id = UUID.randomUUID().toString(),
            ownerType = MediaAssetOwnerType.CATALOG_ITEM.name,
            ownerId = itemId,
            role = MediaAssetRole.CATALOG_IMAGE.name,
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
    }

    override suspend fun loadCatalogImage(itemId: String): ByteArray? {
        val asset = mediaAssetDao.findByOwner(
            MediaAssetOwnerType.CATALOG_ITEM.name, itemId, MediaAssetRole.CATALOG_IMAGE.name,
        ) ?: return null
        return fileStore.open(asset.relativePath)?.use { it.readBytes() }
    }

    override suspend fun removeCatalogImage(itemId: String): Boolean =
        removeImage(MediaAssetOwnerType.CATALOG_ITEM, itemId, MediaAssetRole.CATALOG_IMAGE)

    override suspend fun saveRingImage(input: InputStream, mimeType: String) =
        saveRingAsset(input, mimeType, MediaAssetRole.RING_IMAGE)
    override suspend fun loadRingImage(): ByteArray? = loadRingAsset(MediaAssetRole.RING_IMAGE)
    override suspend fun removeRingImage(): Boolean =
        removeImage(MediaAssetOwnerType.RING, RING_OWNER_ID, MediaAssetRole.RING_IMAGE)
    override suspend fun saveRingBanner(input: InputStream, mimeType: String) =
        saveRingAsset(input, mimeType, MediaAssetRole.RING_BANNER)
    override suspend fun loadRingBanner(): ByteArray? = loadRingAsset(MediaAssetRole.RING_BANNER)
    override suspend fun removeRingBanner(): Boolean =
        removeImage(MediaAssetOwnerType.RING, RING_OWNER_ID, MediaAssetRole.RING_BANNER)

    private suspend fun saveRingAsset(input: InputStream, mimeType: String, role: MediaAssetRole) {
        val stored = fileStore.write(input, mimeType)
        val asset = MediaAssetEntity(
            id = UUID.randomUUID().toString(), ownerType = MediaAssetOwnerType.RING.name,
            ownerId = RING_OWNER_ID, role = role.name, relativePath = stored.relativePath,
            mimeType = stored.mimeType, byteSize = stored.byteSize, sha256 = stored.sha256,
            createdAtEpochMillis = System.currentTimeMillis(),
        )
        val previous = try { mediaAssetDao.replaceForOwner(asset) }
        catch (failure: Exception) { fileStore.delete(stored.relativePath); throw failure }
        previous?.let { fileStore.delete(it.relativePath) }
    }

    private suspend fun loadRingAsset(role: MediaAssetRole): ByteArray? {
        val asset = mediaAssetDao.findByOwner(MediaAssetOwnerType.RING.name, RING_OWNER_ID, role.name)
            ?: return null
        return fileStore.open(asset.relativePath)?.use { it.readBytes() }
    }

    override suspend fun removeAvatar(memberId: String): Boolean =
        removeImage(MediaAssetOwnerType.MEMBER, memberId, MediaAssetRole.AVATAR)

    override suspend fun loadAvatar(memberId: String): ByteArray? {
        val asset = mediaAssetDao.findByOwner(
            MediaAssetOwnerType.MEMBER.name,
            memberId,
            MediaAssetRole.AVATAR.name,
        ) ?: return null
        return fileStore.open(asset.relativePath)?.use { it.readBytes() }
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
    private companion object { const val RING_OWNER_ID = "primary" }
}
