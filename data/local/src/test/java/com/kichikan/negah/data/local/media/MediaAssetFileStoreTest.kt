package com.kichikan.negah.data.local.media

import java.io.ByteArrayInputStream
import java.io.File
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class MediaAssetFileStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun writesAndReadsImageBytesWithStableMetadata() {
        val store = MediaAssetFileStore(File(temporaryFolder.root, "media"))
        val bytes = byteArrayOf(1, 2, 3, 4, 5)

        val stored = store.write(ByteArrayInputStream(bytes), "image/png")

        assertEquals("image/png", stored.mimeType)
        assertEquals(bytes.size.toLong(), stored.byteSize)
        assertEquals(64, stored.sha256.length)
        assertArrayEquals(bytes, store.open(stored.relativePath)!!.use { it.readBytes() })
    }

    @Test
    fun deletesStoredImage() {
        val store = MediaAssetFileStore(File(temporaryFolder.root, "media"))
        val stored = store.write(ByteArrayInputStream(byteArrayOf(9, 8, 7)), "image/jpeg")

        assertTrue(store.delete(stored.relativePath))
        assertNull(store.open(stored.relativePath))
    }

    @Test
    fun rejectsUnsupportedMimeTypesAndEmptyFiles() {
        val store = MediaAssetFileStore(File(temporaryFolder.root, "media"))

        try {
            store.write(ByteArrayInputStream(byteArrayOf(1)), "application/pdf")
            throw AssertionError("Expected unsupported MIME type to fail")
        } catch (_: IllegalArgumentException) {
            assertTrue(true)
        }

        try {
            store.write(ByteArrayInputStream(byteArrayOf()), "image/png")
            throw AssertionError("Expected empty image to fail")
        } catch (_: java.io.IOException) {
            assertTrue(true)
        }
    }

    @Test
    fun rejectsPathsOutsideTheManagedAssetDirectory() {
        val store = MediaAssetFileStore(File(temporaryFolder.root, "media"))

        assertNull(store.open("../private.png"))
        assertFalse(store.delete("../private.png"))
    }
}
