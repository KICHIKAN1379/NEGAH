package com.kichikan.negah

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kichikan.negah.domain.backup.BackupRetention

@Composable
fun BackupScreen(viewModel: BackupViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree(), viewModel::onFolderPicked)
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("پشتیبان‌گیری", style = MaterialTheme.typography.headlineSmall)
        Text("پوشه MRT را در فایل‌های شخصی گوشی بسازید یا انتخاب کنید و دسترسی را تأیید کنید. پشتیبان‌ها رمزگذاری نمی‌شوند و فقط ${BackupRetention.MAX_VERSIONS} نسخه آخر نگه داشته می‌شود.")
        Text(if (state.folder == null) "پوشه‌ای انتخاب نشده است." else "پوشه پشتیبان انتخاب شده است.")
        OutlinedButton(onClick = { picker.launch(null) }, modifier = Modifier.fillMaxWidth()) { Text("انتخاب پوشه MRT") }
        Button(onClick = viewModel::backupNow, enabled = state.folder != null && !state.busy, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.busy) "در حال ساخت پشتیبان…" else "ساخت پشتیبان اکنون")
        }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        Text("بازیابی از پشتیبان هنوز پیاده‌سازی نشده است.", style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = onBack) { Text("بازگشت") }
    }
}
