package com.kichikan.negah.data.local

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import com.kichikan.negah.data.local.media.MediaAssetEntity
import java.io.File
import java.util.UUID
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DatabaseMaintenanceTest {
    @Test
    fun snapshotCopiesDatabaseAndEveryReferencedMediaAsset() {
        val context = RuntimeEnvironment.getApplication()
        context.deleteDatabase("negah_coach.db")
        val database = Room.databaseBuilder(context, NegahDatabase::class.java, "negah_coach.db")
            .setJournalMode(androidx.room.RoomDatabase.JournalMode.TRUNCATE)
            .allowMainThreadQueries()
            .build()
        val assetId = UUID.randomUUID().toString()
        val bytes = byteArrayOf(7, 3, 1, 9, 4)
        val mediaRoot = File(context.filesDir, "media-assets").apply { mkdirs() }
        val relativePath = "$assetId.png"
        File(mediaRoot, relativePath).writeBytes(bytes)
        val snapshot = File(context.cacheDir, "backup-test-$assetId")

        try {
            database.mediaAssetDao().insert(
                MediaAssetEntity(
                    id = assetId,
                    ownerType = "MEMBER",
                    ownerId = "member-1",
                    role = "AVATAR",
                    relativePath = relativePath,
                    mimeType = "image/png",
                    byteSize = bytes.size.toLong(),
                    sha256 = "0".repeat(64),
                    createdAtEpochMillis = 1L,
                ),
            )

            val version = DatabaseMaintenance(context, database).snapshotTo(snapshot)

            assertEquals(18, version)
            val copiedDatabase = File(snapshot, "negah_coach.db")
            assertTrue(copiedDatabase.isFile && copiedDatabase.length() > 0L)
            assertArrayEquals(bytes, File(snapshot, "media-assets/$relativePath").readBytes())

            val sqlite = SQLiteDatabase.openDatabase(
                copiedDatabase.absolutePath, null, SQLiteDatabase.OPEN_READONLY,
            )
            try {
                assertEquals(18, sqlite.version)
                sqlite.rawQuery("PRAGMA integrity_check", null).use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals("ok", cursor.getString(0))
                }
            } finally {
                sqlite.close()
            }
        } finally {
            snapshot.deleteRecursively()
            database.close()
            context.deleteDatabase("negah_coach.db")
            File(mediaRoot, relativePath).delete()
            mediaRoot.delete()
        }
    }
}
