package com.kichikan.negah

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kichikan.negah.data.local.NegahDatabase
import com.kichikan.negah.domain.backup.BackupRetention
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.LocalDateTime
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class BackupUiState(val folder: String? = null, val busy: Boolean = false, val message: String? = null)

@HiltViewModel
class BackupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: NegahDatabase,
) : ViewModel() {
    private val prefs = context.getSharedPreferences("backup", Context.MODE_PRIVATE)
    private val mutableState = MutableStateFlow(BackupUiState(folder = prefs.getString(KEY_TREE, null)))
    val state = mutableState.asStateFlow()

    fun onFolderPicked(uri: Uri?) {
        if (uri == null) return
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }.onFailure {
            mutableState.update { s -> s.copy(message = "دسترسی دائمی به پوشه داده نشد") }
            return
        }
        prefs.edit().putString(KEY_TREE, uri.toString()).apply()
        mutableState.update { it.copy(folder = uri.toString(), message = "پوشه پشتیبان انتخاب شد") }
    }

    fun backupNow() {
        val tree = mutableState.value.folder?.let(Uri::parse)
        if (tree == null) {
            mutableState.update { it.copy(message = "ابتدا پوشه MRT را انتخاب کنید") }
            return
        }
        mutableState.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching { writeBackup(tree) }
                    .fold({ "پشتیبان ساخته شد: $it" }, { "پشتیبان‌گیری ناموفق بود؛ پوشه را دوباره انتخاب کنید" })
            }
            mutableState.update { it.copy(busy = false, message = text) }
        }
    }

    private fun writeBackup(tree: Uri): String {
        // Flush the write-ahead log so the single .db file is complete.
        database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
        val dbFile: File = context.getDatabasePath(DB_NAME)
        val resolver = context.contentResolver
        val parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val name = BackupRetention.fileName(LocalDateTime.now())
        val target = DocumentsContract.createDocument(resolver, parent, "application/zip", name)
            ?: error("create failed")
        resolver.openOutputStream(target, "w")!!.use { out ->
            ZipOutputStream(out).use { zip ->
                zip.putNextEntry(ZipEntry(DB_NAME))
                dbFile.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("manifest.txt"))
                zip.write("app=negah\ndbVersion=${database.openHelper.readableDatabase.version}\ncreated=$name\n".toByteArray())
                zip.closeEntry()
            }
        }
        // Retention: keep only the newest two backups; unrelated files are left alone.
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val existing = mutableListOf<Pair<String, String>>()
        resolver.query(
            children,
            arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null, null, null,
        )?.use { c -> while (c.moveToNext()) existing += c.getString(1) to c.getString(0) }
        BackupRetention.namesToDelete(existing.map { it.first }).forEach { stale ->
            existing.firstOrNull { it.first == stale }?.let { (_, id) ->
                DocumentsContract.deleteDocument(resolver, DocumentsContract.buildDocumentUriUsingTree(tree, id))
            }
        }
        return name
    }

    private companion object {
        const val KEY_TREE = "tree_uri"
        const val DB_NAME = "negah_coach.db"
    }
}
