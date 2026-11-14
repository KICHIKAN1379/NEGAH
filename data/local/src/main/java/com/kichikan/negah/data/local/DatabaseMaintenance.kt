package com.kichikan.negah.data.local

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.concurrent.Callable
import javax.inject.Inject

/**
 * Creates a consistent on-disk snapshot while Room holds its write transaction.
 *
 * The database is configured for TRUNCATE journaling so the main database file
 * is self-contained and no WAL sidecar needs to be copied.
 */
class DatabaseMaintenance @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: NegahDatabase,
) {
    fun schemaVersion(): Int = database.openHelper.readableDatabase.version

    fun snapshotTo(destination: File): Int {
        require(destination.exists() || destination.mkdirs()) {
            "Unable to create backup snapshot directory"
        }

        return database.runInTransaction(Callable {
            val sqlite = database.openHelper.writableDatabase
            check(journalMode(sqlite) == "truncate") {
                "Database must use TRUNCATE journal mode before backup"
            }

            val sourceDatabase = context.getDatabasePath(DATABASE_NAME)
            check(sourceDatabase.isFile) { "Database file is missing" }
            sourceDatabase.copyTo(File(destination, DATABASE_NAME), overwrite = true)

            val mediaRoot = File(context.filesDir, MEDIA_DIRECTORY)
            val mediaDestination = File(destination, MEDIA_DIRECTORY)
            check(mediaDestination.exists() || mediaDestination.mkdirs()) {
                "Unable to create media snapshot directory"
            }

            val relativePaths = mutableListOf<String>()
            sqlite.query("SELECT relativePath FROM media_assets").use { cursor ->
                val pathColumn = cursor.getColumnIndexOrThrow("relativePath")
                while (cursor.moveToNext()) relativePaths += cursor.getString(pathColumn)
            }

            relativePaths.distinct().forEach { relativePath ->
                require(ASSET_PATH.matches(relativePath)) { "Invalid media asset path in database" }
                val source = File(mediaRoot, relativePath)
                check(source.canonicalFile.parentFile == mediaRoot.canonicalFile && source.isFile) {
                    "A referenced media asset is missing"
                }
                val target = File(mediaDestination, relativePath)
                source.copyTo(target, overwrite = true)
                check(target.length() == source.length()) { "Media asset snapshot is incomplete" }
            }
            sqlite.version
        })
    }

    private fun journalMode(database: SupportSQLiteDatabase): String =
        database.query("PRAGMA journal_mode").use { cursor ->
            check(cursor.moveToFirst()) { "Unable to read database journal mode" }
            cursor.getString(0).lowercase()
        }

    private companion object {
        const val DATABASE_NAME = "negah_coach.db"
        const val MEDIA_DIRECTORY = "media-assets"
        val ASSET_PATH = Regex("^[0-9a-fA-F-]{36}\\.(jpg|png|webp)$")
    }
}
