package com.kichikan.negah

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kichikan.negah.domain.settings.HomeShortcut

@Composable
fun HomeShortcutsScreen(
    viewModel: HomeShortcutsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = {
                viewModel.discardChanges()
                onBack()
            }) { Text("بازگشت") }
            Text("میان‌برهای خانه", style = MaterialTheme.typography.headlineSmall)
        }
        Text("مشخص کنید چه بخش‌هایی در دسترسی سریع خانه نمایش داده شوند. حداکثر چهار مورد قابل انتخاب است.")
        Text(
            "${state.selected.size} از ${HomeShortcut.MAX_SELECTION} میان‌بر انتخاب شده",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )

        if (state.isLoading) {
            CircularProgressIndicator()
        } else {
            HomeShortcut.entries.forEach { shortcut ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = shortcut in state.selected,
                        onCheckedChange = { viewModel.toggle(shortcut) },
                    )
                    Text(shortcut.label)
                }
            }
        }

        state.message?.let { message ->
            Text(
                text = message,
                color = if (state.messageIsError) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary,
            )
        }

        Button(
            onClick = viewModel::save,
            enabled = !state.isLoading && !state.isSaving && state.hasUnsavedChanges,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (state.isSaving) "در حال ذخیره..." else "ذخیره")
        }
    }
}
