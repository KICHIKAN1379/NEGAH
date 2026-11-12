package com.kichikan.negah.data.local.media

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest
import java.util.UUID

internal data class StoredMediaAsset(
    val relativePath: String,
    val mimeType: String,
    val byteSize: Long,
    val sha256: String,
)

internal class MediaAssetFileStore(private val rootDirectory: File) {
    private val extensionsByMimeType = mapOf(
        "image/jpeg" to "jpg",
        "image/jpg" to "jpg",
        "image/png" to "png",
        "image/webp" to "webp",
    )

    fun write(input: InputStream, mimeType: String): StoredMediaAsset {
        val normalizedMimeType = mimeType.substringBefore(';').trim().lowercase()
        val extension = extensionsByMimeType[normalizedMimeType]
            ?: throw IllegalArgumentException("Unsupported image type")

        if (!rootDirectory.exists() && !rootDirectory.mkdirs()) {
            throw IOException("Unable to create media storage directory")
        }

        val id = UUID.randomUUID().toString()
        val relativePath = "$id.$extension"
        val temporaryFile = File(rootDirectory, ".$id.tmp")
        val destinationFile = File(rootDirectory, relativePath)
        val digest = MessageDigest.getInstance("SHA-256")
        var byteSize = 0L

        try {
            input.use { source ->
                temporaryFile.outputStream().buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = source.read(buffer)
                        if (count < 0) break
                        if (count == 0) continue
                        output.write(buffer, 0, count)
                        digest.update(buffer, 0, count)
                        byteSize += count
                    }
                }
            }

            if (byteSize == 0L) throw IOException("Image file is empty")
            if (!temporaryFile.renameTo(destinationFile)) {
                throw IOException("Unable to finalize stored image")
            }

            val checksum = digest.digest().joinToString("") { "%02x".format(it) }
            return StoredMediaAsset(relativePath, normalizedMimeType, byteSize, checksum)
        } catch (failure: Exception) {
            temporaryFile.delete()
            destinationFile.delete()
            throw failure
        }
    }

    fun open(relativePath: String): InputStream? {
        if (!isValidRelativePath(relativePath)) return null
        val file = File(rootDirectory, relativePath)
        if (file.canonicalFile.parentFile != rootDirectory.canonicalFile) return null
        return file.takeIf { it.isFile }?.inputStream()
    }

    fun delete(relativePath: String): Boolean {
        if (!isValidRelativePath(relativePath)) return false
        val file = File(rootDirectory, relativePath)
        if (file.canonicalFile.parentFile != rootDirectory.canonicalFile) return false
        return !file.exists() || file.delete()
    }

    private fun isValidRelativePath(value: String): Boolean = ASSET_FILE_PATTERN.matches(value)

    private companion object {
        val ASSET_FILE_PATTERN = Regex("^[0-9a-fA-F-]{36}\\.(jpg|png|webp)$")
    }
}
