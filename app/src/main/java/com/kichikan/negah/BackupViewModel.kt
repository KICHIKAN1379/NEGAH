package com.kichikan.negah

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class BackupUiState(
    val folder: String? = null,
    val busy: Boolean = false,
    val message: String? = null,
    val restorePreview: BackupPreview? = null,
    val restoreQueued: Boolean = false,
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupManager: BackupArchiveManager,
) : ViewModel() {
    private val prefs = context.getSharedPreferences(BackupArchiveManager.PREFS_NAME, Context.MODE_PRIVATE)
    private val mutableState = MutableStateFlow(
        BackupUiState(
            folder = prefs.getString(BackupArchiveManager.KEY_TREE, null),
            message = prefs.getString(BackupArchiveManager.KEY_RESTORE_MESSAGE, null),
            restoreQueued = BackupArchiveManager.hasPendingRestore(context),
        ),
    )
    val state = mutableState.asStateFlow()
    private var preparedRestore: PreparedRestore? = null

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
        prefs.edit().putString(BackupArchiveManager.KEY_TREE, uri.toString()).apply()
        mutableState.update { it.copy(folder = uri.toString(), message = "پوشه MRT انتخاب شد") }
    }

    fun backupNow() {
        if (mutableState.value.folder == null) {
            mutableState.update { it.copy(message = "ابتدا پوشه MRT را انتخاب کنید") }
            return
        }
        runBusy("در حال ساخت پشتیبان…") {
            val name = backupManager.createAutomaticBackup()
            if (name == null) "پشتیبان خودکار هنوز فعال نشده است."
            else "پشتیبان ساخته شد: $name"
        }
    }

    fun exportTo(uri: Uri?) {
        if (uri == null) return
        runBusy("در حال آماده‌سازی خروجی…") {
            backupManager.exportTo(uri)
            "خروجی کامل حلقه با موفقیت ساخته شد."
        }
    }

    fun prepareRestore(uri: Uri?) {
        if (uri == null) return
        preparedRestore?.directory?.deleteRecursively()
        preparedRestore = null
        runBusy("در حال بررسی فایل پشتیبان…") {
            val prepared = backupManager.prepareRestore(uri)
            preparedRestore = prepared
            mutableState.update {
                it.copy(busy = false, restorePreview = prepared.preview, message = null)
            }
            null
        }
    }

    fun dismissRestorePreview() {
        preparedRestore?.directory?.deleteRecursively()
        preparedRestore = null
        mutableState.update { it.copy(restorePreview = null) }
    }

    fun confirmRestore() {
        val prepared = preparedRestore ?: return
        runBusy("در حال ساخت نسخه ایمنی…") {
            backupManager.queueRestore(prepared)
            preparedRestore = null
            mutableState.update {
                it.copy(
                    busy = false,
                    restorePreview = null,
                    restoreQueued = true,
                    message = prefs.getString(BackupArchiveManager.KEY_RESTORE_MESSAGE, null),
                )
            }
            null
        }
    }

    private fun runBusy(progress: String, operation: suspend () -> String?) {
        if (mutableState.value.busy || mutableState.value.restoreQueued) return
        mutableState.update { it.copy(busy = true, message = progress) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { operation() }
            }
            result.fold(
                onSuccess = { message ->
                    mutableState.update { it.copy(busy = false, message = message ?: it.message) }
                },
                onFailure = { failure ->
                    mutableState.update {
                        it.copy(busy = false, message = failure.message ?: "عملیات پشتیبان‌گیری ناموفق بود")
                    }
                },
            )
        }
    }

    override fun onCleared() {
        preparedRestore?.directory?.deleteRecursively()
        super.onCleared()
    }
}
