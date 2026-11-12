package com.kichikan.negah.domain.inventory

import java.io.InputStream

interface CatalogItemMediaRepository {
    suspend fun saveCatalogImage(itemId: String, input: InputStream, mimeType: String)
    suspend fun loadCatalogImage(itemId: String): ByteArray?
    suspend fun removeCatalogImage(itemId: String): Boolean
}
