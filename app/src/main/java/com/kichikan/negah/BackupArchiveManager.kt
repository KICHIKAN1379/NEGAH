package com.kichikan.negah

import android.content.Context
import android.net.Uri
import android.database.sqlite.SQLiteDatabase
import android.provider.DocumentsContract
import com.kichikan.negah.data.local.DatabaseMaintenance
import com.kichikan.negah.domain.backup.BackupRetention
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.time.LocalDateTime
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONArray
import org.json.JSONObject

data class BackupPreview(
    val createdAtEpochMillis: Long,
    val databaseVersion: Int,
    val mediaAssetCount: Int,
)

data class PreparedRestore internal constructor(
    val preview: BackupPreview,
    internal val directory: File,
)

/** Full-ring ZIP archive with a JSON manifest and SHA-256 validation. */
@Singleton
class BackupArchiveManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val maintenance: DatabaseMaintenance,
) {
    fun exportTo(destination: Uri) {
        val output = context.contentResolver.openOutputStream(destination, "w")
            ?: error("Unable to open the selected export destination")
        output.use { createArchive(it) }
    }

    /** Returns null when the coach has not selected an MRT folder yet. */
    fun createAutomaticBackup(): String? {
        if (hasPendingRestore(context)) return null
        val tree = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_TREE, null)?.let(Uri::parse) ?: return null
        val resolver = context.contentResolver
        val parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val name = BackupRetention.fileName(LocalDateTime.now())
        val target = DocumentsContract.createDocument(resolver, parent, ZIP_MIME_TYPE, name)
            ?: error("Unable to create a backup file in the selected MRT folder")
        try {
            val output = resolver.openOutputStream(target, "w")
                ?: error("Unable to open the new backup file")
            output.use { createArchive(it) }
            applyRetention(tree)
            return name
        } catch (failure: Exception) {
            runCatching { DocumentsContract.deleteDocument(resolver, target) }
            throw failure
        }
    }

    fun prepareRestore(source: Uri): PreparedRestore {
        val root = File(context.cacheDir, "negah-restore-\${UUID.randomUUID()}")
        val extracted = File(root, "payload")
        check(extracted.mkdirs()) { "Unable to prepare restore validation area" }
        try {
            val archive = File(root, "selected-backup.zip")
            val input = context.contentResolver.openInputStream(source)
                ?: error("Unable to read the selected backup file")
            input.use { copyLimited(it, archive, MAX_ARCHIVE_BYTES) }
            val files = extractAndValidatePaths(archive, extracted)
            val manifestFile = files[MANIFEST_ENTRY] ?: error("Backup manifest is missing")
            val manifest = JSONObject(manifestFile.readText(Charsets.UTF_8))
            require(manifest.getString("format") == FORMAT_ID) { "This file is not a NEGAH full-ring backup" }
            require(manifest.getInt("archiveVersion") == ARCHIVE_VERSION) { "Backup format version is not supported" }
            require(manifest.getString("appId") == APP_ID) { "Backup belongs to a different application" }

            val manifestVersion = manifest.getInt("databaseVersion")
            require(manifestVersion in 1..maintenance.schemaVersion()) {
                "Backup database version is newer than this app or invalid"
            }
            val expected = readManifestEntries(manifest.getJSONArray("entries"))
            require((files.keys - MANIFEST_ENTRY) == expected.keys) {
                "Backup contents do not match the manifest"
            }
            expected.forEach { (path, metadata) ->
                val file = files[path] ?: error("A backup file is missing")
                require(file.length() == metadata.first) { "Backup file size does not match the manifest" }
                require(sha256(file) == metadata.second) { "Backup file checksum is invalid" }
            }

            val databaseFile = files[DATABASE_ENTRY] ?: error("Backup database is missing")
            val mediaCount = validateDatabase(databaseFile, manifestVersion, files)
            return PreparedRestore(
                BackupPreview(
                    createdAtEpochMillis = manifest.getLong("createdAtEpochMillis"),
                    databaseVersion = manifestVersion,
                    mediaAssetCount = mediaCount,
                ),
                root,
            )
        } catch (failure: Exception) {
            root.deleteRecursively()
            throw failure
        }
    }

    /** Creates an internal safety archive before staging the replacement for next launch. */
    fun queueRestore(prepared: PreparedRestore) {
        val backupRoot = File(context.filesDir, BACKUP_DIRECTORY)
        check(backupRoot.exists() || backupRoot.mkdirs()) { "Unable to create private backup directory" }
        val safetyTemp = File(backupRoot, "restore-safety.tmp")
        val safety = File(backupRoot, "restore-safety.zip")
        try {
            FileOutputStream(safetyTemp).use { createArchive(it) }
            if (safety.exists()) check(safety.delete()) { "Unable to replace the previous safety point" }
            check(safetyTemp.renameTo(safety)) { "Unable to finalize the safety point" }

            val pending = File(backupRoot, PENDING_DIRECTORY)
            if (pending.exists()) check(pending.deleteRecursively()) { "Unable to clear old restore staging" }
            val pendingDatabase = File(pending, DATABASE_NAME)
            val pendingMedia = File(pending, MEDIA_DIRECTORY)
            check(pendingMedia.mkdirs()) { "Unable to create media restore staging" }
            File(prepared.directory, DATABASE_ENTRY).copyTo(pendingDatabase, overwrite = true)
            val sourceMedia = File(prepared.directory, MEDIA_DIRECTORY)
            if (sourceMedia.exists()) {
                check(sourceMedia.copyRecursively(pendingMedia, overwrite = true).isSuccess) {
                    "Unable to stage media assets"
                }
            }
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            check(
                prefs.edit()
                    .putBoolean(KEY_PENDING_RESTORE, true)
                    .putBoolean(KEY_INSTALLING_RESTORE, false)
                    .putString(KEY_RESTORE_MESSAGE, "بازیابی آماده است؛ برای اعمال آن برنامه را ببندید و دوباره باز کنید.")
                    .commit(),
            ) { "Unable to record the pending restore operation" }
        } catch (failure: Exception) {
            safetyTemp.delete()
            throw failure
        } finally {
            prepared.directory.deleteRecursively()
        }
    }

    private fun createArchive(output: OutputStream) {
        val snapshot = File(context.cacheDir, "negah-backup-\${UUID.randomUUID()}")
        check(snapshot.mkdirs()) { "Unable to create backup snapshot" }
        try {
            val databaseVersion = maintenance.snapshotTo(snapshot)
            val payloads = buildList {
                add(DATABASE_ENTRY to File(snapshot, DATABASE_NAME))
                File(snapshot, MEDIA_DIRECTORY).listFiles()
                    ?.filter { it.isFile }
                    ?.sortedBy { it.name }
                    ?.forEach { add("$MEDIA_DIRECTORY/\${it.name}" to it) }
            }
            val entries = JSONArray()
            payloads.forEach { (path, file) ->
                require(file.isFile) { "A snapshot file is missing" }
                entries.put(JSONObject().put("path", path).put("size", file.length()).put("sha256", sha256(file)))
            }
            val manifest = JSONObject()
                .put("format", FORMAT_ID)
                .put("archiveVersion", ARCHIVE_VERSION)
                .put("appId", APP_ID)
                .put("databaseVersion", databaseVersion)
                .put("createdAtEpochMillis", System.currentTimeMillis())
                .put("entries", entries)

            ZipOutputStream(BufferedOutputStream(output)).use { zip ->
                payloads.forEach { (path, file) ->
                    zip.putNextEntry(ZipEntry(path))
                    FileInputStream(file).buffered().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
                zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
                zip.write(manifest.toString().toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        } finally {
            snapshot.deleteRecursively()
        }
    }

    private fun extractAndValidatePaths(archive: File, destination: File): Map<String, File> {
        val files = linkedMapOf<String, File>()
        var totalBytes = 0L
        var entryCount = 0
        ZipInputStream(BufferedInputStream(FileInputStream(archive))).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entryCount += 1
                require(entryCount <= MAX_ENTRIES) { "Backup contains too many files" }
                require(!entry.isDirectory) { "Backup contains an unexpected directory entry" }
                val path = entry.name
                require(isAllowedArchivePath(path)) { "Backup contains an unsafe or unsupported path" }
                require(path !in files) { "Backup contains duplicate file entries" }
                val target = File(destination, path)
                require(target.canonicalPath.startsWith(destination.canonicalPath + File.separator)) {
                    "Backup contains an invalid file path"
                }
                target.parentFile?.let { check(it.exists() || it.mkdirs()) }
                var entryBytes = 0L
                FileOutputStream(target).buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = zip.read(buffer)
                        if (count < 0) break
                        entryBytes += count
                        totalBytes += count
                        require(entryBytes <= MAX_ENTRY_BYTES && totalBytes <= MAX_TOTAL_BYTES) {
                            "Backup expands beyond the allowed size"
                        }
                        output.write(buffer, 0, count)
                    }
                }
                files[path] = target
                zip.closeEntry()
            }
        }
        require(MANIFEST_ENTRY in files) { "Backup manifest is missing" }
        return files
    }

    private fun readManifestEntries(array: JSONArray): Map<String, Pair<Long, String>> {
        require(array.length() in 1..MAX_ENTRIES) { "Backup manifest has an invalid file count" }
        val result = linkedMapOf<String, Pair<Long, String>>()
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            val path = item.getString("path")
            require(path != MANIFEST_ENTRY && isAllowedPayloadPath(path)) {
                "Backup manifest contains an unsupported path"
            }
            val size = item.getLong("size")
            val checksum = item.getString("sha256")
            require(size in 1..MAX_ENTRY_BYTES && SHA256_PATTERN.matches(checksum)) {
                "Backup manifest contains invalid file metadata"
            }
            require(result.put(path, size to checksum) == null) {
                "Backup manifest contains duplicate file entries"
            }
        }
        require(DATABASE_ENTRY in result) { "Backup manifest does not contain a database" }
        return result
    }

    private fun validateDatabase(file: File, expectedVersion: Int, archiveFiles: Map<String, File>): Int {
        val database = SQLiteDatabase.openDatabase(
            file.absolutePath, null, SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS,
        )
        try {
            require(database.version == expectedVersion) { "Database version does not match the manifest" }
            val integrity = database.rawQuery("PRAGMA integrity_check", null).use { cursor ->
                require(cursor.moveToFirst()) { "Database integrity check returned no result" }
                cursor.getString(0)
            }
            require(integrity.equals("ok", ignoreCase = true)) { "Backup database failed integrity validation" }
            val foreignKeyErrors = database.rawQuery("PRAGMA foreign_key_check", null).use { it.moveToFirst() }
            require(!foreignKeyErrors) { "Backup database contains broken references" }

            val hasMediaTable = database.rawQuery(
                "SELECT 1 FROM sqlite_master WHERE type='table' AND name='media_assets' LIMIT 1", null,
            ).use { it.moveToFirst() }
            val referenced = linkedSetOf<String>()
            if (hasMediaTable) {
                database.rawQuery("SELECT relativePath, sha256, byteSize FROM media_assets", null).use { cursor ->
                    while (cursor.moveToNext()) {
                        val relativePath = cursor.getString(0)
                        require(ASSET_PATH.matches(relativePath)) { "Database contains an unsafe media path" }
                        val archivePath = "$MEDIA_DIRECTORY/$relativePath"
                        val asset = archiveFiles[archivePath] ?: error("A referenced image is missing from backup")
                        require(asset.length() == cursor.getLong(2) && sha256(asset) == cursor.getString(1)) {
                            "A referenced image failed integrity validation"
                        }
                        referenced += archivePath
                    }
                }
            }
            val archivedMedia = archiveFiles.keys.filter { it.startsWith("$MEDIA_DIRECTORY/") }.toSet()
            require(archivedMedia == referenced) { "Backup contains missing or unreferenced media files" }
            return referenced.size
        } finally {
            database.close()
        }
    }

    private fun applyRetention(tree: Uri) {
        val resolver = context.contentResolver
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(
            tree, DocumentsContract.getTreeDocumentId(tree),
        )
        val existing = mutableListOf<Pair<String, String>>()
        resolver.query(
            children,
            arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null, null, null,
        )?.use { cursor ->
            while (cursor.moveToNext()) existing += cursor.getString(1) to cursor.getString(0)
        }
        BackupRetention.namesToDelete(existing.map { it.first }).forEach { stale ->
            existing.firstOrNull { it.first == stale }?.let { (_, id) ->
                DocumentsContract.deleteDocument(resolver, DocumentsContract.buildDocumentUriUsingTree(tree, id))
            }
        }
    }

    private fun isAllowedArchivePath(path: String): Boolean =
        path == MANIFEST_ENTRY || isAllowedPayloadPath(path)

    private fun isAllowedPayloadPath(path: String): Boolean =
        path == DATABASE_ENTRY || path.matches(Regex("^$MEDIA_DIRECTORY/[0-9a-fA-F-]{36}\\.(jpg|png|webp)$"))

    private fun copyLimited(input: InputStream, destination: File, maxBytes: Long) {
        var total = 0L
        destination.outputStream().buffered().use { output ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                total += count
                require(total <= maxBytes) { "Selected backup file is too large" }
                output.write(buffer, 0, count)
            }
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        const val PREFS_NAME = "backup"
        const val KEY_TREE = "tree_uri"
        const val KEY_PENDING_RESTORE = "pending_restore"
        const val KEY_INSTALLING_RESTORE = "installing_restore"
        const val KEY_RESTORE_MESSAGE = "restore_message"
        const val DATABASE_NAME = "negah_coach.db"
        const val MEDIA_DIRECTORY = "media-assets"
        const val BACKUP_DIRECTORY = "backup"
        const val PENDING_DIRECTORY = "restore-pending"
        const val FORMAT_ID = "negah-full-ring-backup"
        const val ARCHIVE_VERSION = 1
        const val APP_ID = "com.kichikan.negah"
        const val MANIFEST_ENTRY = "manifest.json"
        const val DATABASE_ENTRY = "database/$DATABASE_NAME"
        const val ZIP_MIME_TYPE = "application/zip"

        private const val MAX_ARCHIVE_BYTES = 1024L * 1024 * 1024
        private const val MAX_TOTAL_BYTES = 1024L * 1024 * 1024
        private const val MAX_ENTRY_BYTES = 512L * 1024 * 1024
        private const val MAX_ENTRIES = 5002
        private val SHA256_PATTERN = Regex("^[0-9a-f]{64}$")
        private val ASSET_PATH = Regex("^[0-9a-fA-F-]{36}\\.(jpg|png|webp)$")

        fun hasPendingRestore(context: Context): Boolean =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_PENDING_RESTORE, false)
    }
}

/** Applies a staged replacement before Room is opened in the next app process. */
object PendingRestoreInstaller {
    fun apply(context: Context) {
        val prefs = context.getSharedPreferences(BackupArchiveManager.PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(BackupArchiveManager.KEY_PENDING_RESTORE, false)) return

        val backupRoot = File(context.filesDir, BackupArchiveManager.BACKUP_DIRECTORY)
        val pending = File(backupRoot, BackupArchiveManager.PENDING_DIRECTORY)
        val rollback = File(backupRoot, "restore-rollback")
        val databaseFile = context.getDatabasePath(BackupArchiveManager.DATABASE_NAME)
        val mediaDirectory = File(context.filesDir, BackupArchiveManager.MEDIA_DIRECTORY)

        if (prefs.getBoolean(BackupArchiveManager.KEY_INSTALLING_RESTORE, false)) {
            runCatching { restorePrevious(databaseFile, mediaDirectory, rollback) }
            prefs.edit()
                .putBoolean(BackupArchiveManager.KEY_PENDING_RESTORE, false)
                .putBoolean(BackupArchiveManager.KEY_INSTALLING_RESTORE, false)
                .putString(BackupArchiveManager.KEY_RESTORE_MESSAGE, "بازیابی کامل نشد؛ اطلاعات قبلی حفظ و بازیابی لغو شد.")
                .commit()
            pending.deleteRecursively()
            return
        }

        val rollbackTemp = File(backupRoot, "restore-rollback.tmp")
        val databaseTemp = File(databaseFile.parentFile, "negah_coach.restore.tmp")
        val mediaTemp = File(context.filesDir, "media-assets.restore.tmp")
        try {
            if (rollbackTemp.exists()) check(rollbackTemp.deleteRecursively())
            check(rollbackTemp.mkdirs())
            if (databaseFile.isFile) {
                databaseFile.copyTo(File(rollbackTemp, BackupArchiveManager.DATABASE_NAME), overwrite = true)
                File(rollbackTemp, "had-database").writeText("true")
            }
            if (mediaDirectory.isDirectory) {
                check(mediaDirectory.copyRecursively(File(rollbackTemp, BackupArchiveManager.MEDIA_DIRECTORY), overwrite = true).isSuccess)
            }
            if (rollback.exists()) check(rollback.deleteRecursively())
            check(rollbackTemp.renameTo(rollback)) { "Unable to prepare rollback files" }

            val pendingDatabase = File(pending, BackupArchiveManager.DATABASE_NAME)
            check(pendingDatabase.isFile) { "Staged restore database is missing" }
            pendingDatabase.copyTo(databaseTemp, overwrite = true)
            if (mediaTemp.exists()) check(mediaTemp.deleteRecursively())
            val pendingMedia = File(pending, BackupArchiveManager.MEDIA_DIRECTORY)
            if (pendingMedia.exists()) {
                check(pendingMedia.copyRecursively(mediaTemp, overwrite = true).isSuccess)
            } else {
                check(mediaTemp.mkdirs())
            }

            check(prefs.edit().putBoolean(BackupArchiveManager.KEY_INSTALLING_RESTORE, true).commit())
            deleteDatabaseSidecars(databaseFile)
            if (databaseFile.exists()) check(databaseFile.delete())
            if (mediaDirectory.exists()) check(mediaDirectory.deleteRecursively())
            check(databaseTemp.renameTo(databaseFile)) { "Unable to install restored database" }
            check(mediaTemp.renameTo(mediaDirectory)) { "Unable to install restored images" }

            prefs.edit()
                .putBoolean(BackupArchiveManager.KEY_PENDING_RESTORE, false)
                .putBoolean(BackupArchiveManager.KEY_INSTALLING_RESTORE, false)
                .putString(BackupArchiveManager.KEY_RESTORE_MESSAGE, "بازیابی با موفقیت انجام شد. نسخهٔ ایمنی قبلی نگهداری شده است.")
                .commit()
            pending.deleteRecursively()
            rollback.deleteRecursively()
        } catch (failure: Exception) {
            if (rollback.exists()) runCatching { restorePrevious(databaseFile, mediaDirectory, rollback) }
            prefs.edit()
                .putBoolean(BackupArchiveManager.KEY_PENDING_RESTORE, false)
                .putBoolean(BackupArchiveManager.KEY_INSTALLING_RESTORE, false)
                .putString(BackupArchiveManager.KEY_RESTORE_MESSAGE, "بازیابی انجام نشد؛ نسخهٔ قبلی حفظ شد. فایل پشتیبان را دوباره بررسی کنید.")
                .commit()
            pending.deleteRecursively()
        } finally {
            databaseTemp.delete()
            if (mediaTemp.exists()) mediaTemp.deleteRecursively()
            if (rollbackTemp.exists()) rollbackTemp.deleteRecursively()
        }
    }

    private fun restorePrevious(databaseFile: File, mediaDirectory: File, rollback: File) {
        deleteDatabaseSidecars(databaseFile)
        val previousDatabase = File(rollback, BackupArchiveManager.DATABASE_NAME)
        if (previousDatabase.isFile) {
            if (databaseFile.exists()) databaseFile.delete()
            previousDatabase.copyTo(databaseFile, overwrite = true)
        } else if (!File(rollback, "had-database").exists() && databaseFile.exists()) {
            databaseFile.delete()
        }
        val previousMedia = File(rollback, BackupArchiveManager.MEDIA_DIRECTORY)
        if (mediaDirectory.exists()) mediaDirectory.deleteRecursively()
        if (previousMedia.isDirectory) previousMedia.copyRecursively(mediaDirectory, overwrite = true)
        else mediaDirectory.mkdirs()
    }

    private fun deleteDatabaseSidecars(databaseFile: File) {
        listOf("-wal", "-shm", "-journal").forEach { File(databaseFile.absolutePath + it).delete() }
    }
}
