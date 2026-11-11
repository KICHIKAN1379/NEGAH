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
        RuntimeEnvironment.getApplication<android.app.Application>().filesDir.resolve("media-assets").deleteRecursively()
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
