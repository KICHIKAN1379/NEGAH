package com.kichikan.negah.domain.settings

import java.io.InputStream

interface RingProfileMediaRepository {
    suspend fun saveRingImage(input: InputStream, mimeType: String)
    suspend fun loadRingImage(): ByteArray?
    suspend fun removeRingImage(): Boolean
    suspend fun saveRingBanner(input: InputStream, mimeType: String)
    suspend fun loadRingBanner(): ByteArray?
    suspend fun removeRingBanner(): Boolean
}
