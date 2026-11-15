package com.kichikan.negah

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun BackgroundMusicScreen(
    viewModel: BackgroundMusicViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        viewModel.selectAudio(it)
    }

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("موزیک پس‌زمینه", style = MaterialTheme.typography.headlineSmall)
        Text("یک فایل صوتی از دستگاه انتخاب کنید. موسیقی در صفحه‌های مختلف برنامه ادامه دارد و هنگام رفتن برنامه به پس‌زمینه متوقف می‌شود.")
        Text(
            text = state.displayName ?: "فایلی انتخاب نشده است",
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(
            onClick = { audioPicker.launch(arrayOf("audio/*")) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("انتخاب فایل موسیقی")
        }
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("پخش موزیک پس‌زمینه")
                Text(
                    if (state.playing) "در حال پخش" else "هنگام خروج از برنامه مکث می‌شود",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(checked = state.enabled, onCheckedChange = viewModel::setEnabled)
        }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) { Text("بازگشت") }
    }
}
