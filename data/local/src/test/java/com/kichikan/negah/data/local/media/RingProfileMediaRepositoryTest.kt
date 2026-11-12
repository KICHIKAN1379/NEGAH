package com.kichikan.negah.data.local.media

import androidx.room.Room
import com.kichikan.negah.data.local.NegahDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
class RingProfileMediaRepositoryTest {
    private lateinit var db: NegahDatabase
    private lateinit var manager: MediaAssetManager
    @Before fun setUp() {
        db=Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(),NegahDatabase::class.java)
            .allowMainThreadQueries().build()
        manager=MediaAssetManager(RuntimeEnvironment.getApplication(),db.mediaAssetDao())
    }
    @After fun tearDown() {
        db.close()
        RuntimeEnvironment.getApplication().filesDir.resolve("media-assets").deleteRecursively()
    }
    @Test fun catalogImagesAreStoredIndependentlyByCatalogItem()=runBlocking {
        val first = byteArrayOf(3, 4, 5)
        val second = byteArrayOf(6, 7, 8)
        manager.saveCatalogImage("catalog-1", ByteArrayInputStream(first), "image/png")
        manager.saveCatalogImage("catalog-2", ByteArrayInputStream(second), "image/webp")
        assertArrayEquals(first, manager.loadCatalogImage("catalog-1"))
        assertArrayEquals(second, manager.loadCatalogImage("catalog-2"))
        assertTrue(manager.removeCatalogImage("catalog-1"))
        assertNull(manager.loadCatalogImage("catalog-1"))
        assertArrayEquals(second, manager.loadCatalogImage("catalog-2"))
    }

    @Test fun ringImageAndBannerPersistIndependentlyAndCanBeRemoved()=runBlocking {
        val image=byteArrayOf(1,2,3,4); val banner=byteArrayOf(9,8,7)
        manager.saveRingImage(ByteArrayInputStream(image),"image/png")
        manager.saveRingBanner(ByteArrayInputStream(banner),"image/jpeg")
        assertArrayEquals(image,manager.loadRingImage()); assertArrayEquals(banner,manager.loadRingBanner())
        assertTrue(manager.removeRingImage()); assertFalse(manager.removeRingImage())
        assertArrayEquals(banner,manager.loadRingBanner())
        assertTrue(manager.removeRingBanner()); assertFalse(manager.removeRingBanner())
    }
}
