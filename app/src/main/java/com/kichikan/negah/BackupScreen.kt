package com.kichikan.negah

import android.app.Activity
import android.os.Process
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kichikan.negah.domain.backup.BackupRetention
import java.time.LocalDateTime

@Composable
fun BackupScreen(viewModel: BackupViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(), viewModel::onFolderPicked,
    )
    val exportPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BackupArchiveManager.ZIP_MIME_TYPE), viewModel::exportTo,
    )
    val importPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(), viewModel::prepareRestore,
    )

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("ورودی و خروجی اطلاعات", style = MaterialTheme.typography.headlineSmall)
        Text(
            "برای پشتیبان خودکار، پوشه MRT را انتخاب کنید. برنامه هنگام رفتن به پس‌زمینه تلاش می‌کند نسخه‌ای محلی بسازد و حداکثر دو نسخهٔ خودکار را نگه می‌دارد.",
        )
        Text(if (state.folder == null) "پوشه MRT انتخاب نشده است." else "پوشه MRT انتخاب شده است.")
        OutlinedButton(
            onClick = { folderPicker.launch(null) },
            enabled = !state.busy && !state.restoreQueued,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("انتخاب پوشه MRT") }
        Button(
            onClick = viewModel::backupNow,
            enabled = state.folder != null && !state.busy && !state.restoreQueued,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("ساخت پشتیبان خودکار اکنون") }

        Text("خروجی و بازیابی دستی", style = MaterialTheme.typography.titleMedium)
        Text("خروجی کامل حلقه شامل پایگاه داده، سوابق و تصاویر است. فایل خروجی رمزگذاری نمی‌شود؛ آن را در محل امن نگه دارید.")
        OutlinedButton(
            onClick = { exportPicker.launch(BackupRetention.fileName(LocalDateTime.now())) },
            enabled = !state.busy && !state.restoreQueued,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("تهیه خروجی کامل حلقه") }
        OutlinedButton(
            onClick = { importPicker.launch(arrayOf(BackupArchiveManager.ZIP_MIME_TYPE, "application/octet-stream")) },
            enabled = !state.busy && !state.restoreQueued,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("بررسی فایل و بازیابی") }

        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        if (state.restoreQueued) {
            Text(
                "نسخهٔ ایمنی از اطلاعات فعلی ساخته شده است. برای اعمال بازیابی، برنامه بسته می‌شود؛ سپس آن را دوباره باز کنید.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = {
                    (context as? Activity)?.finishAffinity()
                    Process.killProcess(Process.myPid())
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("بستن برنامه و اعمال بازیابی") }
        }
        TextButton(onClick = onBack, enabled = !state.busy) { Text("بازگشت") }
    }

    state.restorePreview?.let { preview ->
        AlertDialog(
            onDismissRequest = viewModel::dismissRestorePreview,
            title = { Text("تأیید جایگزینی اطلاعات") },
            text = {
                Text(
                    "این فایل معتبر است و شامل نسخهٔ " + preview.databaseVersion +
                        " پایگاه داده و " + preview.mediaAssetCount + " تصویر است. " +
                        "با تأیید، تمام اطلاعات فعلی حلقه با اطلاعات این پشتیبان جایگزین می‌شود؛ " +
                        "پیش از اعمال، نسخهٔ ایمنی از وضعیت فعلی ساخته خواهد شد. این کار را فقط در صورت اطمینان انجام دهید.",
                )
            },
            confirmButton = {
                Button(onClick = viewModel::confirmRestore, enabled = !state.busy) {
                    Text("تأیید جایگزینی")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissRestorePreview, enabled = !state.busy) {
                    Text("انصراف")
                }
            },
        )
    }
}
